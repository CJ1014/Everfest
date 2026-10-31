package com.holidayseasons.event;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.ai.CelebrationTask;
import com.holidayseasons.ai.FollowGroupTask;
import com.holidayseasons.ai.HandOutCandyTask;
import com.holidayseasons.ai.TrickOrTreatGroup;
import com.holidayseasons.ai.TrickOrTreatTask;
import com.holidayseasons.block.CandyBowlBlock;
import com.holidayseasons.block.DoorbellBlock;
import com.holidayseasons.block.EverFestBlocks;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.decoration.DecorationPiece;
import com.holidayseasons.decoration.DecorationThemes;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayWindow;
import com.holidayseasons.home.HomeRecord;
import com.holidayseasons.home.HomeRegistry;
import com.holidayseasons.item.CostumeItem;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.reputation.ReputationMath;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.sound.EverFestSounds;
import com.holidayseasons.village.VillageRecord;
import com.holidayseasons.village.VillageStyle;
import com.holidayseasons.villager.VillagerPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Halloween gameplay on the server: trick-or-treating, candy, costumes, tricks and Halloween-night creatures. */
public final class HalloweenService {
    public static final String HOLIDAY_ID = EverFest.MOD_ID + ":halloween";
    public static final String CREATURE_TAG = "holidayseasons_halloween_creature";

    private final MinecraftServer server;
    private final EverFestServer services;
    private final Random random = new Random();
    private final Map<String, List<TrickOrTreatGroup>> groupsByVillage = new HashMap<>();
    private final Map<BlockPos, Villager> givers = new HashMap<>();
    private final Map<BlockPos, BlockPos> entranceCache = new HashMap<>();
    private final Set<TrickOrTreatGroup> playerTreated = new HashSet<>();
    private String entranceCacheOccurrence = "";

    public HalloweenService(MinecraftServer server, EverFestServer services) {
        this.server = server;
        this.services = services;
    }

    private static boolean cfg(java.util.function.Supplier<Boolean> v, boolean fallback) {
        return ServerConfig.SPEC.isLoaded() ? v.get() : fallback;
    }

    private static int cfgInt(java.util.function.Supplier<Integer> v, int fallback) {
        return ServerConfig.SPEC.isLoaded() ? v.get() : fallback;
    }

    /** The festival if it is Halloween, else null. */
    @Nullable
    public CelebrationDirector.Festival halloween() {
        CelebrationDirector.Festival f = services.director().festival();
        return f != null && f.holiday().id().equals(HOLIDAY_ID) ? f : null;
    }

    public boolean isHalloweenNight() {
        CelebrationDirector.Festival f = halloween();
        if (f == null || f.phase() != HolidayPhase.ACTIVE) return false;
        long t = server.overworld().getDayTime() % 24000L;
        return t >= 12800 && t < 23200;
    }

    // ------------------------------------------------------------------ groups

    /** Forms a trick-or-treat group around {@code leader}; returns the leader's task or null if no group can go out. */
    @Nullable
    public TrickOrTreatTask formGroup(Villager leader, String villageKey, CelebrationDirector.Festival f, List<Villager> candidates) {
        if (!cfg(() -> ServerConfig.INSTANCE.trickOrTreating.get(), true)) return null;
        List<TrickOrTreatGroup> groups = groupsByVillage.computeIfAbsent(villageKey, k -> new ArrayList<>());
        groups.removeIf(g -> !g.isActive());
        if (groups.size() >= cfgInt(() -> ServerConfig.INSTANCE.maxTrickOrTreatGroups.get(), 3)) return null;
        TrickOrTreatGroup group = new TrickOrTreatGroup(leader);
        int maxSize = (int) f.holiday().definition().setting("group_size_max", 3);
        String occ = f.occurrence().key();
        for (Villager c : candidates) {
            if (group.size() >= maxSize) break;
            if (c == leader || !c.isBaby() || c.distanceToSqr(leader) > 16 * 16) continue;
            group.members.add(c);
        }
        TrickOrTreatTask task = new TrickOrTreatTask(occ, villageKey, this, group, 4 + random.nextInt(3));
        for (Villager m : group.members) services.director().assignFromDirector(m, new FollowGroupTask(occ, villageKey, group));
        groups.add(group);
        return task;
    }

    public void groupFinished(TrickOrTreatGroup group) {
        group.phase = TrickOrTreatGroup.Phase.DONE;
        playerTreated.remove(group);
        groupsByVillage.values().forEach(l -> l.remove(group));
    }

    public int activeGroups() {
        int n = 0;
        for (List<TrickOrTreatGroup> l : groupsByVillage.values()) for (TrickOrTreatGroup g : l) if (g.isActive()) n++;
        return n;
    }

    /** Chooses the nearest reachable door this group has not visited and that has not had too many visitors. */
    @Nullable
    public TrickOrTreatGroup.Destination pickDestination(ServerLevel level, Villager leader, String villageKey, String occ, TrickOrTreatGroup group) {
        // Navigation refuses to plan paths while airborne; never judge reachability then.
        if (!leader.onGround()) return null;
        if (!occ.equals(entranceCacheOccurrence)) {
            entranceCache.clear();
            entranceCacheOccurrence = occ;
        }
        int radius = cfgInt(() -> ServerConfig.INSTANCE.trickOrTreatRadius.get(), 48);
        int maxVisits = cfgInt(() -> ServerConfig.INSTANCE.maxVisitsPerHouse.get(), 2);
        VillageRecord rec = services.director().villages().get(villageKey);
        BlockPos centre = rec != null ? rec.centre() : leader.blockPosition();
        List<TrickOrTreatGroup.Destination> candidates = new ArrayList<>();
        Set<String> seenDoors = new HashSet<>();
        for (HomeRecord home : services.homes().visitable(level.dimension(), centre, radius)) {
            BlockPos door = nearestDoor(level, home.entrance(), home.hearth);
            String key = "home:" + home.id;
            if (seenDoors.add(key)) candidates.add(new TrickOrTreatGroup.Destination(key, home.entrance(), door, home.id));
        }
        for (BlockPos bed : services.director().homesOf(villageKey)) {
            if (bed.distSqr(centre) > (double) radius * radius) continue;
            BlockPos entrance = entranceCache.computeIfAbsent(bed, b -> HomeRegistry.findEntrance(level, b));
            if (entrance.equals(bed)) continue;
            if (services.homes().near(level.dimension(), entrance, 6) != null) continue;
            BlockPos door = nearestDoor(level, entrance, bed);
            String key = "door:" + door.getX() + "," + door.getY() + "," + door.getZ();
            if (seenDoors.add(key)) candidates.add(new TrickOrTreatGroup.Destination(key, entrance, door, null));
        }
        candidates.removeIf(d -> group.visited.contains(d.key()) || visits(villageKey, occ, d.key()) >= maxVisits);
        candidates.sort(Comparator.comparingDouble(d -> d.entrance().distSqr(leader.blockPosition()) * (0.7 + random.nextDouble() * 0.6)));
        int checks = 0;
        for (TrickOrTreatGroup.Destination d : candidates) {
            if (checks++ >= 4) break;
            Path path = leader.getNavigation().createPath(d.entrance(), 1);
            if (path != null && path.canReach()) return d;
            group.visited.add(d.key());
        }
        return null;
    }

    private static BlockPos nearestDoor(ServerLevel level, BlockPos entrance, BlockPos fallback) {
        for (BlockPos p : BlockPos.betweenClosed(entrance.offset(-1, -1, -1), entrance.offset(1, 1, 1))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock) return p.immutable();
        }
        return fallback;
    }

    private int visits(String villageKey, String occ, String doorKey) {
        VillageRecord rec = services.director().villages().get(villageKey);
        return rec == null ? 0 : rec.celebration(occ).visits(doorKey);
    }

    public void recordVisit(String villageKey, String occ, TrickOrTreatGroup.Destination d) {
        VillageRecord rec = services.director().villages().get(villageKey);
        if (rec != null) {
            rec.celebration(occ).recordVisit(d.key());
            services.director().villages().setDirty();
        }
        if (d.homeId() != null) {
            HomeRecord home = services.homes().get(d.homeId());
            if (home != null) {
                home.countVisit();
                services.homes().setDirty();
            }
        }
    }

    // ------------------------------------------------------------------ at the door

    public void knock(ServerLevel level, TrickOrTreatGroup.Destination d, Villager knocker) {
        for (BlockPos p : BlockPos.betweenClosed(d.door().offset(-2, -1, -2), d.door().offset(2, 2, 2))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof DoorbellBlock) {
                DoorbellBlock.ring(level, p.immutable(), s, null, Component.translatable("holidayseasons.trick_or_treaters"));
                return;
            }
        }
        level.playSound(null, d.door(), EverFestSounds.KNOCK.get(), SoundSource.BLOCKS, 1f, 1f);
        level.sendParticles(ParticleTypes.CRIT, d.door().getX() + 0.5, d.door().getY() + 1.1, d.door().getZ() + 0.5, 4, 0.2, 0.3, 0.2, 0.05);
    }

    /** Tells an online home owner that children are at the door. Returns true if the group should wait longer. */
    public boolean notifyOwner(ServerLevel level, TrickOrTreatGroup.Destination d, TrickOrTreatGroup group) {
        if (d.homeId() == null) return false;
        HomeRecord home = services.homes().get(d.homeId());
        if (home == null) return false;
        ServerPlayer owner = server.getPlayerList().getPlayer(home.owner);
        if (owner == null || owner.level() != level || owner.blockPosition().distSqr(d.entrance()) > 48 * 48) return false;
        owner.displayClientMessage(Component.translatable("holidayseasons.trick_or_treat.at_door", group.size(), home.name()), false);
        services.collection().encounter(owner, "trick_or_treaters");
        return true;
    }

    /** Called every waiting tick; returns candy once somebody answers. */
    public ItemStack tryAnswer(ServerLevel level, TrickOrTreatGroup.Destination d, String villageKey, TrickOrTreatGroup group, int waited) {
        if (playerTreated.remove(group)) return group.lastCandy.isEmpty() ? randomCandy() : group.lastCandy;
        if (waited == 20) {
            for (Map.Entry<BlockPos, Villager> e : givers.entrySet()) {
                Villager giver = e.getValue();
                if (giver.isAlive() && e.getKey().distSqr(d.entrance()) <= 9 && giver.distanceToSqr(e.getKey().getCenter()) < 9) {
                    CelebrationTask t = services.director().taskFor(giver);
                    if (t instanceof HandOutCandyTask hand) hand.serve(level, giver);
                    return randomCandy();
                }
            }
        }
        if (waited == 30) {
            BlockPos bowl = findBowl(level, d);
            if (bowl != null) {
                ItemStack first = ItemStack.EMPTY;
                for (int i = 0; i < group.size(); i++) {
                    ItemStack c = CandyBowlBlock.serveVillager(level, bowl, villageKey);
                    if (c.isEmpty()) break;
                    if (first.isEmpty()) first = c;
                }
                return first;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Nobody handed out candy in time: villager houses sometimes still answer; player homes do not. */
    public ItemStack finalAnswer(ServerLevel level, TrickOrTreatGroup.Destination d, String villageKey, String occ, TrickOrTreatGroup group) {
        if (d.homeId() != null) return ItemStack.EMPTY;
        // Someone inside answers. Deterministic per door and occurrence so the same house is consistently generous.
        long seed = d.key().hashCode() * 31L + occ.hashCode();
        return new Random(seed).nextDouble() < 0.7 ? randomCandy() : ItemStack.EMPTY;
    }

    @Nullable
    private static BlockPos findBowl(ServerLevel level, TrickOrTreatGroup.Destination d) {
        for (BlockPos p : BlockPos.betweenClosed(d.entrance().offset(-3, -1, -3), d.entrance().offset(3, 2, 3))) {
            if (level.getBlockState(p).is(EverFestBlocks.CANDY_BOWL.get())) return p.immutable();
        }
        return null;
    }

    /** A harmless trick: a puff of smoke and giggles, plus a temporary cobweb or pumpkin where decorations are allowed. */
    public void playTrick(ServerLevel level, TrickOrTreatGroup.Destination d, String villageKey, String occ, Villager v) {
        level.playSound(null, d.entrance(), EverFestSounds.TRICK_POOF.get(), SoundSource.NEUTRAL, 0.9f, 1f);
        level.playSound(null, v.getX(), v.getY(), v.getZ(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.7f, 1.8f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, d.entrance().getX() + 0.5, d.entrance().getY() + 0.6, d.entrance().getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.01);
        HomeRecord home = d.homeId() == null ? null : services.homes().get(d.homeId());
        if (home != null) {
            services.reputation().add(home.owner, villageKey, ReputationMath.Action.TRICK_PLAYED_ON, services.calendar().snapshot().epochDay());
        }
        if (!cfg(() -> ServerConfig.INSTANCE.harmlessTricks.get(), true)) return;
        if (home != null && !home.allowVillagerDecorations()) return;
        List<DecorationPiece> pieces = DecorationThemes.ground("halloween", VillageStyle.PLAINS).stream()
                .filter(p -> p.id().equals("cobwebs") || p.id().equals("carved_pumpkin")).toList();
        if (pieces.isEmpty()) return;
        for (int attempt = 0; attempt < 6; attempt++) {
            BlockPos spot = d.entrance().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos p = spot.above(dy);
                if (services.ledger().isFreeSpot(level, p)) {
                    services.ledger().place(level, p, pieces.get(random.nextInt(pieces.size())), random.nextInt(4) * 90f, occ, villageKey);
                    return;
                }
            }
        }
    }

    public void registerGiver(BlockPos post, Villager v) {
        givers.put(post, v);
    }

    public void unregisterGiver(BlockPos post, Villager v) {
        givers.remove(post, v);
    }

    /** A post for an adult handing out candy: outside its own front door, or at the festival square. */
    @Nullable
    public HandOutCandyTask handOutTask(ServerLevel level, Villager v, String villageKey, String occ, BlockPos square) {
        BlockPos bed = v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME)
                .filter(h -> h.dimension() == level.dimension()).map(net.minecraft.core.GlobalPos::pos).orElse(null);
        BlockPos post;
        BlockPos face;
        if (bed != null) {
            BlockPos entrance = entranceCache.computeIfAbsent(bed, b -> HomeRegistry.findEntrance(level, b));
            if (entrance.equals(bed)) return null;
            post = entrance;
            face = entrance.offset(entrance.getX() - nearestDoor(level, entrance, bed).getX(), 0, entrance.getZ() - nearestDoor(level, entrance, bed).getZ());
        } else {
            post = square;
            face = square;
        }
        if (givers.containsKey(post)) return null;
        return new HandOutCandyTask(occ, villageKey, this, post, face, randomCandy(), 600 + random.nextInt(900));
    }

    public ItemStack randomCandy() {
        List<Item> candy = List.of(EverFestItems.CANDY_CORN.get(), EverFestItems.CARAMEL_APPLE.get(), EverFestItems.CHOCOLATE_BAR.get(),
                EverFestItems.LOLLIPOP.get(), EverFestItems.GUMMY_BAT.get());
        return new ItemStack(candy.get(random.nextInt(candy.size())));
    }

    /** A player hands candy to a village child: the child is delighted, and if its group is waiting, that's a treat. */
    public boolean playerGivesCandy(Player player, Villager child, ItemStack candy) {
        if (!child.isBaby() || !candy.is(EverFestItems.CANDY_TAG)) return false;
        ServerLevel level = (ServerLevel) child.level();
        ItemStack given = candy.copyWithCount(1);
        if (!player.getAbilities().instabuild) candy.shrink(1);
        level.sendParticles(ParticleTypes.HEART, child.getX(), child.getY() + child.getBbHeight() + 0.3, child.getZ(), 3, 0.3, 0.2, 0.3, 0);
        level.playSound(null, child.getX(), child.getY(), child.getZ(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 0.8f, 1.6f);
        level.playSound(null, child.getX(), child.getY(), child.getZ(), EverFestSounds.CANDY.get(), SoundSource.NEUTRAL, 0.7f, 1.2f);
        String village = services.director().resolveVillage(child).key();
        services.reputation().add(player.getUUID(), village, ReputationMath.Action.GAVE_CANDY, services.calendar().snapshot().epochDay());
        for (List<TrickOrTreatGroup> l : groupsByVillage.values()) {
            for (TrickOrTreatGroup g : l) {
                if (g.isActive() && (g.leader == child || g.members.contains(child)) && g.phase == TrickOrTreatGroup.Phase.WAITING) {
                    g.lastCandy = given;
                    playerTreated.add(g);
                }
            }
        }
        services.collection().count(player, "candy_given", 1);
        com.holidayseasons.reputation.EverFestAdvancements.award(player, com.holidayseasons.reputation.EverFestAdvancements.TRICK_OR_TREAT);
        return true;
    }

    // ------------------------------------------------------------------ costumes

    /** Puts on or takes off a villager's temporary costume depending on whether it's Halloween costume time. */
    public void updateCostume(Villager v, @Nullable CelebrationDirector.Festival f, long today) {
        ItemStack head = v.getItemBySlot(EquipmentSlot.HEAD);
        boolean dressTime = f != null && cfg(() -> ServerConfig.INSTANCE.villagerCostumes.get(), true)
                && (f.phase() == HolidayPhase.ACTIVE || (f.phase() == HolidayPhase.PREPARING && today >= f.occurrence().window().start() - 1));
        boolean halloween = dressTime && f.holiday().id().equals(HOLIDAY_ID) && f.has("wear_costumes");
        boolean christmas = dressTime && f.holiday().id().equals(GiftService.HOLIDAY_ID);
        if (!halloween && !christmas) {
            if (!head.isEmpty() && CostumeItem.isTemporary(head)) v.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            return;
        }
        String occ = f.occurrence().key();
        if (!head.isEmpty()) {
            // Swap last holiday's temporary outfit for this one; never touch anything else.
            if (!CostumeItem.isTemporary(head) || occ.equals(head.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
                    .copyTag().getString(CostumeItem.TEMPORARY_KEY))) return;
        }
        String profession = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession()).getPath();
        VillagerPersonality p = VillagerPersonality.of(v.getUUID(), profession, v.isBaby());
        if (!v.isBaby() && p.enthusiasm() < 0.55f) return;
        Item wear = halloween ? EverFestItems.COSTUMES.get(p.costume()).get() : festiveHat(v, p);
        ItemStack costume = CostumeItem.temporary(new ItemStack(wear), occ);
        v.setItemSlot(EquipmentSlot.HEAD, costume);
        v.setDropChance(EquipmentSlot.HEAD, 0f);
    }

    private static Item festiveHat(Villager v, VillagerPersonality p) {
        if (v.isBaby()) return p.costume().ordinal() % 2 == 0 ? EverFestItems.ELF_HAT.get() : EverFestItems.ANTLERS.get();
        return switch (p.giftPreference()) {
            case GEMS, BOOKS -> EverFestItems.SANTA_HAT.get();
            case CLOTHES, FLOWERS -> EverFestItems.WINTER_BEANIE.get();
            case TREATS -> EverFestItems.ELF_HAT.get();
            default -> p.generosity() > 0.6f ? EverFestItems.SANTA_HAT.get() : EverFestItems.WINTER_BEANIE.get();
        };
    }

    // ------------------------------------------------------------------ creatures

    public void tick() {
        ServerLevel level = server.overworld();
        long gameTime = level.getGameTime();
        if (gameTime % 20 != 0) return;
        boolean night = isHalloweenNight();
        if (!night) {
            if (gameTime % 100 == 0) dismissCreatures(level);
            return;
        }
        if (!cfg(() -> ServerConfig.INSTANCE.halloweenCreatures.get(), true)) return;
        int cap = cfgInt(() -> ServerConfig.INSTANCE.creaturesPerPlayer.get(), 4);
        double chance = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.creatureSpawnChance.get() : 0.08;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || random.nextDouble() >= chance) continue;
            int near = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64), e -> e.getTags().contains(CREATURE_TAG)).size();
            if (near >= cap) continue;
            trySpawnNear(level, player);
        }
    }

    private void trySpawnNear(ServerLevel level, ServerPlayer player) {
        double roll = random.nextDouble();
        EntityType<? extends Mob> type = roll < 0.45 ? EverFestEntities.GHOST.get() : roll < 0.85 ? EverFestEntities.GOURDLING.get() : EverFestEntities.SCARECROW.get();
        for (int attempt = 0; attempt < 4; attempt++) {
            double a = random.nextDouble() * Math.PI * 2;
            int dist = 20 + random.nextInt(20);
            int x = player.getBlockX() + (int) (Math.cos(a) * dist);
            int z = player.getBlockZ() + (int) (Math.sin(a) * dist);
            if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) continue;
            if (!Monster.isDarkEnoughToSpawn(level, pos, level.random)) continue;
            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(pos.getX() + 0.5, pos.getY() + (type == EverFestEntities.GHOST.get() ? 1.5 : 0), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
            if (!level.noCollision(mob)) continue;
            mob.addTag(CREATURE_TAG);
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            level.addFreshEntity(mob);
            return;
        }
    }

    /** At dawn, or once Halloween is over, the creatures fade away. */
    private void dismissCreatures(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(128), e -> e.getTags().contains(CREATURE_TAG))) {
                level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.5, mob.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
                mob.discard();
            }
        }
    }

    /** Creatures saved in chunks that load after Halloween night are removed before they appear. */
    public boolean shouldRejectCreature(Entity e) {
        return e.getTags().contains(CREATURE_TAG) && !isHalloweenNight();
    }

    /** Leader position and phase of each group that is out, for diagnostics. */
    public List<String> describeGroups() {
        List<String> out = new ArrayList<>();
        groupsByVillage.forEach((village, list) -> list.stream().filter(TrickOrTreatGroup::isActive).forEach(g ->
                out.add(g.size() + " children at " + g.leader.blockPosition().toShortString() + " " + g.phase
                        + (g.current == null ? "" : " -> " + g.current.key()) + ", treats " + g.treats + ", tricks " + g.tricks)));
        return out;
    }

    /** For diagnostics. */
    public int giverCount() {
        return givers.size();
    }

    public HolidayWindow currentWindow() {
        CelebrationDirector.Festival f = halloween();
        return f == null ? null : f.occurrence().window();
    }
}
