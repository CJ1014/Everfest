package com.holidayseasons.economy;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.entity.HolidayMerchantEntity;
import com.holidayseasons.entity.SpawnSpots;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.sound.EverFestSounds;
import com.holidayseasons.village.VillageRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Brings holiday merchants to celebrating villages and the Midnight Peddler to players on Halloween night.
 * <ul>
 *   <li>A village is visited at most once per celebration day (persistent claim) and only while a player is nearby,
 *   so merchants never spawn in unloaded or unwatched places.</li>
 *   <li>The peddler visits each player at most once per Halloween, at night, outdoors in the overworld.</li>
 * </ul>
 * Both are configurable (economy and events sections of the server config).
 */
public final class MerchantService {
    private static final int CHECK_INTERVAL = 200;
    private static final int PLAYER_RANGE = 48;
    /** Checks per Halloween night (13000..23000 at one per {@link #CHECK_INTERVAL} ticks), for the per-night chance. */
    private static final int NIGHT_CHECKS = 10000 / CHECK_INTERVAL;
    private final EverFestServer services;
    private final Random random = new Random();

    public MerchantService(EverFestServer services) {
        this.services = services;
    }

    public void tick() {
        ServerLevel level = services.server().overworld();
        if (level.getGameTime() % CHECK_INTERVAL != 0 || !ServerConfig.SPEC.isLoaded()) return;
        CelebrationDirector.Festival f = services.director().festival();
        if (f == null || f.phase() != HolidayPhase.ACTIVE) return;
        if (ServerConfig.INSTANCE.holidayMerchants.get()) {
            for (VillageRecord village : services.director().villages().all()) visitVillage(level, f, village, false);
        }
        if (ServerConfig.INSTANCE.midnightPeddler.get() && f.holiday().id().equals(EverFest.MOD_ID + ":halloween")) {
            long tod = level.getDayTime() % 24000L;
            if (tod >= 13000 && tod < 23000) {
                double perCheck = 1 - Math.pow(1 - ServerConfig.INSTANCE.midnightPeddlerChance.get(), 1.0 / NIGHT_CHECKS);
                for (ServerPlayer p : level.players()) {
                    if (random.nextDouble() < perCheck) peddlerVisits(level, f, p, false);
                }
            }
        }
    }

    private static boolean inOverworld(VillageRecord r, ServerLevel level) {
        return r.key.startsWith(level.dimension().location() + "|");
    }

    /**
     * A merchant may come to {@code village} today. {@code force} (commands/tests) skips the chance roll but not the
     * once-per-day claim or the nearby-player rule. Returns the merchant, or null.
     */
    @Nullable
    public HolidayMerchantEntity visitVillage(ServerLevel level, CelebrationDirector.Festival f, VillageRecord village, boolean force) {
        if (!inOverworld(village, level)) return null;
        BlockPos centre = village.centre();
        if (!level.isLoaded(centre) || level.getNearestPlayer(centre.getX(), centre.getY(), centre.getZ(), PLAYER_RANGE, false) == null) return null;
        if (!level.getEntitiesOfClass(HolidayMerchantEntity.class, new AABB(centre).inflate(64)).isEmpty()) return null;
        long day = services.calendar().schedulingSnapshot().epochDay();
        String claim = "merchant|" + f.occurrence().key() + "|" + village.key + "|" + day;
        if (services.gifts().claims().has(claim)) return null;
        services.gifts().claims().claim(claim);
        double chance = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.merchantVisitChance.get() : 1.0;
        if (!force && random.nextDouble() >= chance) return null;
        BlockPos at = SpawnSpots.around(level, centre, 4, 12, level.getRandom(), 16);
        if (at == null) return null;
        int stay = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.merchantStayTicks.get() : 12000;
        HolidayMerchantEntity m = spawn(level, at, f.holiday().id(), stay);
        if (m != null) {
            m.settleAround(centre, 16);
            if (!ServerConfig.SPEC.isLoaded() || ServerConfig.INSTANCE.festivalMarkets.get()) {
                com.holidayseasons.decoration.MarketStalls.place(level, services.ledger(), at, f.holiday().id(), f.occurrence().key(),
                        village.key, level.getRandom());
            }
            EverFest.LOGGER.info("EverFest: a {} merchant arrived at village {}", f.holiday().id(), village.key);
        }
        return m;
    }

    /** The Midnight Peddler steps out of the dark near {@code player}, once per Halloween per player. */
    @Nullable
    public HolidayMerchantEntity peddlerVisits(ServerLevel level, CelebrationDirector.Festival f, ServerPlayer player, boolean force) {
        if (player.isSpectator() || player.level().dimension() != Level.OVERWORLD) return null;
        if (!force && !level.canSeeSky(player.blockPosition())) return null;
        String claim = "peddler|" + f.occurrence().key() + "|" + player.getUUID();
        if (services.gifts().claims().has(claim)) return null;
        BlockPos at = SpawnSpots.around(level, player.blockPosition(), 8, 14, level.getRandom(), 16);
        if (at == null) return null;
        services.gifts().claims().claim(claim);
        HolidayMerchantEntity m = spawn(level, at, HolidayMerchantEntity.MIDNIGHT, 11000);
        if (m == null) return null;
        m.settleAround(at, 6);
        player.displayClientMessage(Component.translatable("holidayseasons.peddler.arrives"), false);
        player.playNotifySound(EverFestSounds.DISTANT_BELL.get(), SoundSource.AMBIENT, 0.8f, 0.8f);
        player.playNotifySound(EverFestSounds.WHISPERS.get(), SoundSource.AMBIENT, 0.6f, 1f);
        EverFest.LOGGER.info("EverFest: the Midnight Peddler visits {}", player.getGameProfile().getName());
        return m;
    }

    @Nullable
    private static HolidayMerchantEntity spawn(ServerLevel level, BlockPos at, String occasion, int stayTicks) {
        HolidayMerchantEntity m = EverFestEntities.HOLIDAY_MERCHANT.get().create(level);
        if (m == null) return null;
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(m, level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        m.setOccasion(occasion);
        m.setDespawnDelay(stayTicks);
        level.addFreshEntity(m);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, m.getX(), m.getY() + 1, m.getZ(), 16, 0.4, 0.6, 0.4, 0.02);
        return m;
    }
}
