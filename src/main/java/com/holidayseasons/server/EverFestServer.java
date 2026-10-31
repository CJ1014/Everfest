package com.holidayseasons.server;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.CalendarService;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.decoration.DecorationLedger;
import com.holidayseasons.holiday.HolidayService;
import com.holidayseasons.home.HomeRegistry;
import com.holidayseasons.reputation.ReputationData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the per-server EverFest services. Created when a server starts and discarded when it stops, so an integrated
 * server reopened in the same client session never sees stale state.
 */
public final class EverFestServer {
    @Nullable
    private static EverFestServer instance;

    private final MinecraftServer server;
    private final CalendarService calendar;
    private final HolidayService holidays;
    private final CelebrationDirector director;
    private final HomeRegistry homes;
    private final ReputationData reputation;
    private final com.holidayseasons.event.HalloweenService halloween;
    private final com.holidayseasons.event.GiftService gifts;
    private final com.holidayseasons.event.EasterService easter;
    private final com.holidayseasons.event.FireworkShowService shows;
    private final com.holidayseasons.entity.WildlifeSpawner wildlife;
    private final com.holidayseasons.economy.MerchantService merchants;
    private final com.holidayseasons.event.VillageObjectives objectives;
    private final com.holidayseasons.event.GraveyardHaunting haunting;
    private final com.holidayseasons.home.PartyService parties;

    private EverFestServer(MinecraftServer server) {
        this.server = server;
        this.calendar = new CalendarService(server);
        this.holidays = new HolidayService(server, calendar);
        this.homes = server.overworld().getDataStorage().computeIfAbsent(HomeRegistry.FACTORY, HomeRegistry.FILE_ID);
        this.reputation = server.overworld().getDataStorage().computeIfAbsent(ReputationData.FACTORY, ReputationData.FILE_ID);
        this.director = new CelebrationDirector(server, calendar, holidays);
        this.halloween = new com.holidayseasons.event.HalloweenService(server, this);
        this.gifts = new com.holidayseasons.event.GiftService(server, this);
        this.easter = new com.holidayseasons.event.EasterService(this);
        this.shows = new com.holidayseasons.event.FireworkShowService(server, this);
        this.wildlife = new com.holidayseasons.entity.WildlifeSpawner(this);
        this.merchants = new com.holidayseasons.economy.MerchantService(this);
        this.objectives = new com.holidayseasons.event.VillageObjectives(this);
        this.haunting = new com.holidayseasons.event.GraveyardHaunting(this);
        this.parties = new com.holidayseasons.home.PartyService(this);
    }

    public com.holidayseasons.event.VillageObjectives objectives() {
        return objectives;
    }

    public com.holidayseasons.event.GraveyardHaunting haunting() {
        return haunting;
    }

    public com.holidayseasons.home.PartyService parties() {
        return parties;
    }

    public com.holidayseasons.economy.MerchantService merchants() {
        return merchants;
    }

    public com.holidayseasons.entity.WildlifeSpawner wildlife() {
        return wildlife;
    }

    public com.holidayseasons.event.EasterService easter() {
        return easter;
    }

    public com.holidayseasons.event.FireworkShowService shows() {
        return shows;
    }

    public com.holidayseasons.event.GiftService gifts() {
        return gifts;
    }

    public HomeRegistry homes() {
        return homes;
    }

    public ReputationData reputation() {
        return reputation;
    }

    public com.holidayseasons.event.HalloweenService halloween() {
        return halloween;
    }

    /** Entities in spawn chunks join before the services exist; adopt them once at start-up. */
    private void adoptLoadedEntities() {
        long today = calendar.schedulingSnapshot().epochDay();
        int villagers = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> rejected = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (e instanceof Villager v) {
                    director.track(v);
                    villagers++;
                } else if (director.ledger().shouldRejectEntity(e, today)) {
                    rejected.add(e);
                }
            }
            rejected.forEach(Entity::discard);
        }
        EverFest.LOGGER.info("EverFest: tracking {} loaded villagers", villagers);
    }

    public static void start(MinecraftServer server) {
        instance = new EverFestServer(server);
        instance.adoptLoadedEntities();
        EverFest.LOGGER.info("EverFest: services started, today is {}", instance.calendar.snapshot().date());
    }

    public static void stop() {
        instance = null;
    }

    @Nullable
    public static EverFestServer get() {
        return instance;
    }

    public static EverFestServer require() {
        if (instance == null) throw new IllegalStateException("EverFest server services are not running");
        return instance;
    }

    public MinecraftServer server() {
        return server;
    }

    public CalendarService calendar() {
        return calendar;
    }

    public HolidayService holidays() {
        return holidays;
    }

    public CelebrationDirector director() {
        return director;
    }

    public DecorationLedger ledger() {
        return director.ledger();
    }

    public void tick() {
        calendar.tick();
        holidays.tick();
        director.tick();
        halloween.tick();
        gifts.tick();
        shows.tick();
        wildlife.tick();
        merchants.tick();
        objectives.tick();
        haunting.tick();
        parties.tick();
    }
}
