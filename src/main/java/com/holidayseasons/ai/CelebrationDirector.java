package com.holidayseasons.ai;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.CalendarService;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.decoration.DecorationLedger;
import com.holidayseasons.decoration.DecorationPiece;
import com.holidayseasons.decoration.DecorationSchedule;
import com.holidayseasons.decoration.DecorationThemes;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayPhaseChangedEvent;
import com.holidayseasons.holiday.HolidayRegistry;
import com.holidayseasons.holiday.HolidayService;
import com.holidayseasons.holiday.HolidayTransition;
import com.holidayseasons.holiday.HolidayWindow;
import com.holidayseasons.holiday.OccurrenceRecord;
import com.holidayseasons.holiday.ScheduledHoliday;
import com.holidayseasons.village.VillageKeys;
import com.holidayseasons.village.VillageRecord;
import com.holidayseasons.village.VillageRegistryData;
import com.holidayseasons.village.VillageStyle;
import com.holidayseasons.villager.HolidayBrainInjector;
import com.holidayseasons.villager.VillagerPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates autonomous village celebrations with bounded work per tick.
 *
 * <ul>
 *   <li>Villagers are tracked through join/leave events - the world is never scanned.</li>
 *   <li>Each tick at most {@code villagerBudgetPerTick} villagers are examined (round robin) and one village is updated.</li>
 *   <li>Tasks are executed by {@link CelebrationBehavior} inside each villager's own brain.</li>
 *   <li>Unloaded villages are never loaded; their progress is reconciled from {@link VillageRecord} when they return.</li>
 * </ul>
 */
public final class CelebrationDirector {
    private final MinecraftServer server;
    private final CalendarService calendar;
    private final HolidayService holidays;
    private final DecorationLedger ledger;
    private final VillageRegistryData villages;
    private final Random random = new Random();

    private final Map<UUID, Villager> tracked = new HashMap<>();
    private final ArrayDeque<UUID> queue = new ArrayDeque<>();
    private final Map<UUID, CelebrationTask> tasks = new HashMap<>();
    private final Map<UUID, Long> assignedAt = new HashMap<>();
    private final Map<UUID, Long> nextAssign = new HashMap<>();
    /** Villagers whose current task was assigned directly (tests / admin); the director leaves those alone. */
    private final Set<UUID> manual = new HashSet<>();
    private final Map<String, LiveVillage> live = new LinkedHashMap<>();
    private Iterator<String> villageCursor;
    private final List<FireworkRocketEntity> rockets = new ArrayList<>();

    @Nullable
    private Festival festival;
    private long festivalComputedAt;
    private boolean festivalValid;

    /** What the villages are currently celebrating. */
    public record Festival(ScheduledHoliday holiday, OccurrenceRecord occurrence, HolidayPhase phase) {
        public boolean has(String behavior) {
            return holiday.definition() != null && holiday.definition().hasBehavior(behavior);
        }

        public String theme() {
            return holiday.definition() == null || holiday.definition().decorations().isEmpty() ? "" : holiday.definition().decorations().get(0);
        }
    }

    /** Transient info about a loaded village, rebuilt from its villagers. */
    static final class LiveVillage {
        final String key;
        BlockPos centre;
        boolean hasBell;
        final Set<UUID> members = new HashSet<>();
        final List<BlockPos> homes = new ArrayList<>();
        final Map<VillagerType, Integer> types = new HashMap<>();
        int pendingCatchUp;
        int catchUpAttempts;

        /** Only real villages decorate: ones with a bell, or a cluster of at least four villagers. */
        boolean decorates() {
            return hasBell || members.size() >= 4;
        }

        LiveVillage(String key) {
            this.key = key;
        }
    }

    public CelebrationDirector(MinecraftServer server, CalendarService calendar, HolidayService holidays) {
        this.server = server;
        this.calendar = calendar;
        this.holidays = holidays;
        this.ledger = server.overworld().getDataStorage().computeIfAbsent(DecorationLedger.FACTORY, DecorationLedger.FILE_ID);
        this.villages = server.overworld().getDataStorage().computeIfAbsent(VillageRegistryData.FACTORY, VillageRegistryData.FILE_ID);
    }

    public DecorationLedger ledger() {
        return ledger;
    }

    public VillageRegistryData villages() {
        return villages;
    }

    private static boolean cfg(java.util.function.Supplier<Boolean> v, boolean fallback) {
        return ServerConfig.SPEC.isLoaded() ? v.get() : fallback;
    }

    private static int cfgInt(java.util.function.Supplier<Integer> v, int fallback) {
        return ServerConfig.SPEC.isLoaded() ? v.get() : fallback;
    }

    // ------------------------------------------------------------------ tracking

    public void track(Villager v) {
        if (v.level().isClientSide) return;
        if (tracked.put(v.getUUID(), v) == null) queue.add(v.getUUID());
        HolidayBrainInjector.ensure(v, this);
    }

    public void untrack(Villager v) {
        Villager current = tracked.get(v.getUUID());
        if (current == v) {
            tracked.remove(v.getUUID());
            manual.remove(v.getUUID());
            CelebrationTask t = tasks.remove(v.getUUID());
            if (t != null) t.fail();
        }
    }

    public int trackedCount() {
        return tracked.size();
    }

    @Nullable
    public CelebrationTask taskFor(Villager v) {
        return tasks.get(v.getUUID());
    }

    void onTaskStopped(Villager v, CelebrationTask task) {
        if (tasks.get(v.getUUID()) == task) {
            tasks.remove(v.getUUID());
            assignedAt.remove(v.getUUID());
            manual.remove(v.getUUID());
        }
    }

    /** Assigns a task directly (GameTests, admin tools). The director will not replace or expire it. */
    public void assign(Villager v, CelebrationTask task) {
        track(v);
        tasks.put(v.getUUID(), task);
        assignedAt.put(v.getUUID(), v.level().getGameTime());
        manual.add(v.getUUID());
    }

    // ------------------------------------------------------------------ festival state

    @Nullable
    public Festival festival() {
        long now = server.overworld().getGameTime();
        if (!festivalValid || now - festivalComputedAt >= 20 || now < festivalComputedAt) {
            festivalValid = true;
            festivalComputedAt = now;
            festival = computeFestival();
        }
        return festival;
    }

    private void invalidateFestival() {
        festivalValid = false;
    }

    @Nullable
    private Festival computeFestival() {
        Festival best = null;
        for (OccurrenceRecord r : holidays.records().values()) {
            if (!r.phase().isLive()) continue;
            ScheduledHoliday h = holidays.holiday(r.holidayId).orElse(null);
            if (h == null || !h.enabled()) continue;
            if (best == null || rank(r.phase()) > rank(best.phase())) best = new Festival(h, r, r.phase());
        }
        return best;
    }

    private static int rank(HolidayPhase p) {
        return switch (p) {
            case ACTIVE -> 3;
            case PREPARING -> 2;
            case CLEANUP -> 1;
            default -> 0;
        };
    }

    public void onPhaseChanged(HolidayPhaseChangedEvent e) {
        invalidateFestival();
        HolidayTransition t = e.getTransition();
        if (t.kind() == HolidayTransition.Kind.ENTER && t.to() == HolidayPhase.CLEANUP) {
            OccurrenceRecord r = holidays.records().get(t.occurrenceKey());
            if (r != null) {
                HolidayWindow w = r.window();
                ledger.scheduleExpiry(t.occurrenceKey(), w.end(), (int) (w.cleanupEnd() - w.end()));
            }
            cancelTasks(t.occurrenceKey(), EnumSet.of(TaskType.DECORATE, TaskType.HIDE_EGGS));
        }
        if (t.kind() != HolidayTransition.Kind.ENTER || t.to() == HolidayPhase.COMPLETED) {
            ledger.expireOccurrence(t.occurrenceKey());
            cancelTasks(t.occurrenceKey(), EnumSet.allOf(TaskType.class));
        }
    }

    private void cancelTasks(String occ, Set<TaskType> types) {
        for (CelebrationTask task : tasks.values()) {
            if (task.occurrenceKey.equals(occ) && types.contains(task.type)) task.fail();
        }
    }

    // ------------------------------------------------------------------ tick

    public void tick() {
        long today = calendar.schedulingSnapshot().epochDay();
        ledger.tick(server, today, cfgInt(() -> ServerConfig.INSTANCE.decorationLedgerBudget.get(), 16));
        tickRockets();
        if (!cfg(() -> ServerConfig.INSTANCE.villageCelebrations.get(), true)) return;
        Festival f = festival();
        int budget = cfgInt(() -> ServerConfig.INSTANCE.villagerBudgetPerTick.get(), 6);
        for (int i = 0; i < budget && !queue.isEmpty(); i++) {
            UUID id = queue.poll();
            Villager v = tracked.get(id);
            if (v == null || v.isRemoved() || !v.isAlive()) {
                tracked.remove(id);
                tasks.remove(id);
                continue;
            }
            queue.add(id);
            processVillager(v, f, today);
        }
        if (f != null) tickOneVillage(f, today);
    }

    private void processVillager(Villager v, @Nullable Festival f, long today) {
        ServerLevel level = (ServerLevel) v.level();
        HolidayBrainInjector.ensure(v, this);
        VillageKeys.Resolved rk = VillageKeys.of(v);
        LiveVillage lv = live.computeIfAbsent(rk.key(), LiveVillage::new);
        lv.centre = rk.centre();
        lv.hasBell = rk.hasBell();
        lv.members.add(v.getUUID());
        lv.types.merge(v.getVillagerData().getType(), 1, Integer::sum);
        v.getBrain().getMemory(MemoryModuleType.HOME).filter(h -> h.dimension() == level.dimension()).map(GlobalPos::pos)
                .ifPresent(home -> {
                    if (!lv.homes.contains(home) && lv.homes.size() < 32) lv.homes.add(home);
                });
        VillageRecord rec = villages.getOrCreate(rk.key(), rk.centre());
        styleOf(lv, rec);
        if (rk.hasBell() && !rec.centre().equals(rk.centre())) {
            rec.setCentre(rk.centre());
            villages.setDirty();
        }

        boolean active = f != null && f.phase() == HolidayPhase.ACTIVE;
        if (cfg(() -> ServerConfig.INSTANCE.festivalSchedules.get(), true)) {
            HolidayBrainInjector.applySchedule(v, active, active && f.has("party"), active && f.has("trick_or_treat"));
        }

        UUID id = v.getUUID();
        if (manual.contains(id)) return;
        CelebrationTask current = tasks.get(id);
        if (f == null) {
            if (current != null && !current.isRunning()) tasks.remove(id);
            return;
        }
        long now = level.getGameTime();
        if (current != null) {
            boolean stale = !current.isRunning() && now - assignedAt.getOrDefault(id, now) > 1200;
            if ((current.isDone() && !current.isRunning()) || stale) {
                tasks.remove(id);
            } else {
                return;
            }
        }
        if (now < nextAssign.getOrDefault(id, 0L) || !CelebrationBehavior.eligible(v)) return;
        int cooldown = cfgInt(() -> ServerConfig.INSTANCE.taskCooldownTicks.get(), 300);
        nextAssign.put(id, now + cooldown / 2 + random.nextInt(Math.max(1, cooldown)));

        String profession = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession()).getPath();
        VillagerPersonality personality = VillagerPersonality.of(id, profession, v.isBaby());
        Set<String> behaviors = new HashSet<>(f.holiday().definition() == null ? List.of() : f.holiday().definition().behaviors());
        Set<TaskType> unavailable = EnumSet.noneOf(TaskType.class);
        if (!cfg(() -> ServerConfig.INSTANCE.villagerDecorations.get(), true) || f.theme().isEmpty() || !lv.decorates()
                || ledger.decorationsFor(rk.key(), f.occurrence().key()) >= decorationTarget(f, today)) {
            unavailable.add(TaskType.DECORATE);
        }
        ActivityPicker.Input in = new ActivityPicker.Input(f.phase(), behaviors, personality, profession, v.isBaby(),
                HolidayRegistry.intensity(f.holiday().id()), unavailable);
        ActivityPicker.pick(in, random).ifPresent(type -> {
            CelebrationTask task = build(type, v, level, lv, rec, f);
            if (task != null) {
                tasks.put(id, task);
                assignedAt.put(id, now);
            }
        });
    }

    public int decorationTargetFor(Festival f) {
        return decorationTarget(f, calendar.schedulingSnapshot().epochDay());
    }

    int decorationTarget(Festival f, long today) {
        int max = cfgInt(() -> ServerConfig.INSTANCE.maxDecorationsPerVillage.get(), 18);
        HolidayWindow w = f.occurrence().window();
        int prepDays = (int) (w.start() - w.prepStart());
        return DecorationSchedule.target(max, HolidayRegistry.intensity(f.holiday().id()), f.phase() == HolidayPhase.ACTIVE,
                (int) (today - w.prepStart()), prepDays);
    }

    // ------------------------------------------------------------------ task construction

    @Nullable
    private CelebrationTask build(TaskType type, Villager v, ServerLevel level, LiveVillage lv, VillageRecord rec, Festival f) {
        String occ = f.occurrence().key();
        BlockPos centre = lv.centre;
        return switch (type) {
            case DECORATE -> buildDecorate(level, lv, rec, f);
            case GATHER -> new GatherTask(occ, lv.key, centre, standingSpot(level, centre, 2, 5), 300 + random.nextInt(400));
            case SOCIALIZE -> {
                Villager partner = findPartner(v, lv);
                yield partner == null ? null : new SocializeTask(occ, lv.key, partner);
            }
            case FEAST -> new FeastTask(occ, lv.key, centre, standingSpot(level, centre, 2, 4), feastFood(f));
            case FIREWORKS -> new FireworksTask(occ, lv.key, standingSpot(level, centre, 3, 6), rockets::add);
            case TIDY_UP -> {
                DecorationLedger.Entry e = ledger.findTidyTarget(level, lv.key, v.blockPosition(), 32);
                yield e == null ? null : new TidyUpTask(occ, lv.key, e.pos(), e.occurrenceKey(), ledger, ItemStack.EMPTY);
            }
            // Implemented by later milestones; until then these holidays fall back to gathering.
            default -> new GatherTask(occ, lv.key, centre, standingSpot(level, centre, 2, 6), 200 + random.nextInt(300));
        };
    }

    @Nullable
    private CelebrationTask buildDecorate(ServerLevel level, LiveVillage lv, VillageRecord rec, Festival f) {
        VillageStyle style = styleOf(lv, rec);
        String occ = f.occurrence().key();
        VillageRecord.Celebration c = rec.celebration(occ);
        List<DecorationPiece> centre = DecorationThemes.centre(f.theme(), style);
        boolean wantCentre = !c.centrepiece() && !centre.isEmpty() && !ledger.hasPiece(lv.key, occ, centre.get(0).id());
        DecorationPiece piece = wantCentre ? centre.get(0) : null;
        BlockPos spot;
        if (piece != null) {
            spot = findSpot(level, lv.centre, 2, 4, 4);
        } else {
            List<DecorationPiece> ground = DecorationThemes.ground(f.theme(), style);
            if (ground.isEmpty()) return null;
            piece = DecorationThemes.random(ground, random);
            BlockPos anchor = !lv.homes.isEmpty() && random.nextInt(3) > 0 ? lv.homes.get(random.nextInt(lv.homes.size())) : lv.centre;
            int radius = cfgInt(() -> ServerConfig.INSTANCE.decorationRadius.get(), 20);
            spot = anchor == lv.centre ? findSpot(level, anchor, 3, radius, 2) : findSpot(level, anchor, 2, 6, 2);
        }
        if (spot == null) return null;
        DecorationPiece chosen = piece;
        boolean isCentre = wantCentre;
        float yaw = random.nextInt(4) * 90f;
        return new DecorateTask(occ, lv.key, spot, chosen, yaw, ledger, placed -> {
            c.recordPlaced(isCentre);
            villages.setDirty();
        });
    }

    private VillageStyle styleOf(LiveVillage lv, VillageRecord rec) {
        VillagerType best = null;
        int bestCount = 0;
        for (Map.Entry<VillagerType, Integer> e : lv.types.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        if (best != null) {
            VillageStyle s = VillageStyle.of(best);
            if (s != rec.style()) {
                rec.setStyle(s);
                villages.setDirty();
            }
            return s;
        }
        return rec.style();
    }

    /** Finds a free decoration spot: air above a sturdy block, near the anchor, within loaded chunks. */
    @Nullable
    BlockPos findSpot(ServerLevel level, BlockPos anchor, int minR, int maxR, int clearance) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double a = random.nextDouble() * Math.PI * 2;
            int r = minR + random.nextInt(Math.max(1, maxR - minR + 1));
            int x = anchor.getX() + (int) Math.round(Math.cos(a) * r);
            int z = anchor.getZ() + (int) Math.round(Math.sin(a) * r);
            for (int dy = 4; dy >= -6; dy--) {
                BlockPos p = new BlockPos(x, anchor.getY() + dy, z);
                if (!level.isLoaded(p)) break;
                if (!ledger.isFreeSpot(level, p)) continue;
                boolean clear = true;
                for (int up = 1; up < clearance && clear; up++) clear = level.getBlockState(p.above(up)).isAir();
                if (clear) return p;
            }
        }
        return null;
    }

    private BlockPos standingSpot(ServerLevel level, BlockPos centre, int minR, int maxR) {
        BlockPos p = findSpot(level, centre, minR, maxR, 2);
        return p != null ? p : centre;
    }

    @Nullable
    private Villager findPartner(Villager v, LiveVillage lv) {
        List<UUID> members = new ArrayList<>(lv.members);
        for (int i = 0; i < Math.min(12, members.size()); i++) {
            Villager other = tracked.get(members.get(random.nextInt(members.size())));
            if (other == null || other == v || !other.isAlive() || other.level() != v.level()) continue;
            if (other.distanceToSqr(v) > 24 * 24 || other.isSleeping()) continue;
            CelebrationTask t = tasks.get(other.getUUID());
            if (t != null && t.type != TaskType.GATHER) continue;
            return other;
        }
        return null;
    }

    private static ItemStack feastFood(Festival f) {
        String id = f.holiday().id();
        if (id.endsWith("christmas")) return new ItemStack(Items.COOKIE);
        if (id.endsWith("thanksgiving") || id.endsWith("halloween")) return new ItemStack(Items.PUMPKIN_PIE);
        if (id.endsWith("independence_day")) return new ItemStack(Items.COOKED_BEEF);
        return new ItemStack(Items.BREAD);
    }

    // ------------------------------------------------------------------ village level

    private void tickOneVillage(Festival f, long today) {
        if (live.isEmpty()) return;
        if (villageCursor == null || !villageCursor.hasNext()) villageCursor = new ArrayList<>(live.keySet()).iterator();
        String key = villageCursor.next();
        LiveVillage lv = live.get(key);
        if (lv == null) return;
        lv.members.removeIf(id -> !tracked.containsKey(id));
        if (lv.members.isEmpty()) {
            live.remove(key);
            return;
        }
        VillageRecord rec = villages.getOrCreate(key, lv.centre);
        String occ = f.occurrence().key();
        VillageRecord.Celebration c = rec.celebration(occ);
        // Unloaded-time reconciliation: the village was away for at least a full day during a live holiday.
        if (rec.lastSeenDay() != Long.MIN_VALUE && rec.lastSeenDay() < today - 1 && f.phase() != HolidayPhase.CLEANUP
                && cfg(() -> ServerConfig.INSTANCE.reconcileUnloadedVillages.get(), true)
                && cfg(() -> ServerConfig.INSTANCE.villagerDecorations.get(), true)) {
            int deficit = lv.decorates() ? decorationTarget(f, today) - ledger.decorationsFor(key, occ) : 0;
            lv.pendingCatchUp = Math.max(0, (int) Math.round(deficit * 0.75));
            lv.catchUpAttempts = lv.pendingCatchUp * 4;
            if (lv.pendingCatchUp > 0) {
                EverFest.LOGGER.info("EverFest: village {} returned after {} days; catching up {} decorations",
                        key, today - rec.lastSeenDay(), lv.pendingCatchUp);
            }
        }
        if (rec.lastSeenDay() != today) {
            rec.setLastSeenDay(today);
            villages.setDirty();
        }
        if (lv.pendingCatchUp > 0 && lv.catchUpAttempts-- > 0) {
            // Work the villagers did while nobody was around: placed directly, a few attempts per decoration.
            ServerLevel level = levelOf(key);
            if (level != null) {
                CelebrationTask t = buildDecorate(level, lv, rec, f);
                if (t instanceof DecorateTask d && ledger.place(level, d.spot(), d.piece(), random.nextInt(4) * 90f, occ, key) > 0) {
                    c.recordPlaced(d.piece().placement() == DecorationPiece.Placement.FESTIVAL_CENTER);
                    villages.setDirty();
                    lv.pendingCatchUp--;
                }
            }
        }
    }

    @Nullable
    private ServerLevel levelOf(String villageKey) {
        String dim = villageKey.substring(0, villageKey.indexOf('|'));
        for (ServerLevel l : server.getAllLevels()) {
            if (l.dimension().location().toString().equals(dim)) return l;
        }
        return null;
    }

    // ------------------------------------------------------------------ fireworks

    private void tickRockets() {
        Iterator<FireworkRocketEntity> it = rockets.iterator();
        while (it.hasNext()) {
            FireworkRocketEntity r = it.next();
            if (!r.isRemoved()) continue;
            it.remove();
            if (r.level() instanceof ServerLevel level) burst(level, r.position());
        }
    }

    /** A harmless particle firework: coloured sparks in a sphere plus the vanilla blast sounds. */
    public static void burst(ServerLevel level, Vec3 at) {
        Random rnd = new Random();
        int[] palette = {0xFF4040, 0xFFD040, 0x40C0FF, 0x80FF60, 0xFF60D0, 0xFFFFFF};
        int color = palette[rnd.nextInt(palette.length)];
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f), 1.6f);
        for (int i = 0; i < 70; i++) {
            double theta = rnd.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * rnd.nextDouble() - 1);
            double r = 2.5 + rnd.nextDouble() * 0.6;
            double x = at.x + r * Math.sin(phi) * Math.cos(theta);
            double y = at.y + r * Math.cos(phi);
            double z = at.z + r * Math.sin(phi) * Math.sin(theta);
            level.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y, at.z, 30, 0.6, 0.6, 0.6, 0.15);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, at.x, at.y, at.z, rnd.nextBoolean() ? SoundEvents.FIREWORK_ROCKET_BLAST : SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                SoundSource.AMBIENT, 3f, 0.95f + rnd.nextFloat() * 0.1f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.AMBIENT, 2f, 1f);
    }

    // ------------------------------------------------------------------ diagnostics

    public Map<String, Integer> liveVillageSizes() {
        Map<String, Integer> out = new LinkedHashMap<>();
        live.forEach((k, v) -> out.put(k, v.members.size()));
        return out;
    }

    public long activeTasks() {
        return tasks.values().stream().filter(CelebrationTask::isRunning).count();
    }

    public Map<TaskType, Integer> taskCounts() {
        Map<TaskType, Integer> out = new java.util.EnumMap<>(TaskType.class);
        for (CelebrationTask t : tasks.values()) out.merge(t.type, 1, Integer::sum);
        return out;
    }
}
