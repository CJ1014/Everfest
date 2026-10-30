package com.holidayseasons.command;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarMode;
import com.holidayseasons.calendar.CalendarService;
import com.holidayseasons.calendar.CalendarSnapshot;
import com.holidayseasons.network.OpenScreenPayload;
import com.holidayseasons.server.EverFestServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/** {@code /everfest} - player-facing calendar queries and operator date controls. */
public final class EverFestCommands {
    public static final int ADMIN_LEVEL = 2;

    private EverFestCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("everfest");
        root.then(Commands.literal("calendar").executes(EverFestCommands::openCalendar));
        root.then(Commands.literal("date")
                .executes(EverFestCommands::showDate)
                .then(Commands.literal("info").executes(EverFestCommands::showInfo))
                .then(Commands.literal("set").requires(s -> s.hasPermission(ADMIN_LEVEL))
                        .then(Commands.argument("date", StringArgumentType.word()).executes(EverFestCommands::setDate)))
                .then(Commands.literal("add").requires(s -> s.hasPermission(ADMIN_LEVEL))
                        .then(Commands.argument("days", IntegerArgumentType.integer(-100000, 100000)).executes(EverFestCommands::addDays)))
                .then(Commands.literal("clearoverride").requires(s -> s.hasPermission(ADMIN_LEVEL))
                        .executes(EverFestCommands::clearOverride)));
        root.then(Commands.literal("villages").requires(s -> s.hasPermission(ADMIN_LEVEL)).executes(EverFestCommands::villages));
        root.then(Commands.literal("reputation").executes(EverFestCommands::showReputation)
                .then(Commands.literal("set").requires(s -> s.hasPermission(ADMIN_LEVEL))
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("points", IntegerArgumentType.integer(-200, 1000)).executes(EverFestCommands::setReputation)))));
        root.then(Commands.literal("halloween").requires(s -> s.hasPermission(ADMIN_LEVEL)).executes(EverFestCommands::halloweenStatus));
        root.then(Commands.literal("home")
                .then(Commands.literal("list").executes(EverFestCommands::listHomes))
                .then(Commands.literal("edit").executes(EverFestCommands::editHome))
                .then(Commands.literal("register").requires(s -> s.hasPermission(ADMIN_LEVEL))
                        .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                .then(Commands.argument("owner", net.minecraft.commands.arguments.EntityArgument.player())
                                        .executes(EverFestCommands::registerHome)))));
        HolidayCommands.attach(root);
        dispatcher.register(root);
    }

    static CalendarService calendar() {
        return EverFestServer.require().calendar();
    }

    static Component describe(CalendarSnapshot s) {
        boolean real = s.mode() == CalendarMode.REAL_WORLD;
        return Component.literal(s.date().toDisplayString(real))
                .append(Component.literal(String.format(Locale.ROOT, " %02d:%02d, ", s.hour(), s.minute())))
                .append(Component.translatable("holidayseasons.subseason." + s.subSeason().id()));
    }

    private static int openCalendar(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only players can open the calendar"));
            return 0;
        }
        calendar().sendTo(player);
        PacketDistributor.sendToPlayer(player, new OpenScreenPayload(OpenScreenPayload.Screen.CALENDAR));
        return 1;
    }

    private static int showDate(CommandContext<CommandSourceStack> ctx) {
        CalendarSnapshot s = calendar().snapshot();
        ctx.getSource().sendSuccess(() -> Component.translatable("holidayseasons.command.date", describe(s)), false);
        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        CalendarService cal = calendar();
        CalendarSnapshot s = cal.snapshot();
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.translatable("holidayseasons.command.date", describe(s)), false);
        src.sendSuccess(() -> Component.literal("Mode: " + s.mode() + "  system: " + s.systemSignature()
                + "  year length: " + cal.activeSystem().yearLength(s.date().year()) + " days"), false);
        src.sendSuccess(() -> Component.literal("Season source: " + (s.seasonsFromSereneSeasons() ? "Serene Seasons" : "calendar")
                + "  season adapter: " + cal.seasonAdapter().name()
                + "  policy: " + cal.settings().seasonSyncPolicy()), false);
        cal.seasonReading().ifPresent(r -> src.sendSuccess(() -> Component.literal("Serene Seasons: " + r.subSeason().id()
                + ", " + r.daysPerSubSeason() + " days per sub-season, cycle tick " + r.cycleTicks()
                + (r.cycling() ? "" : " (season cycle paused)")), false));
        if (s.mode() == CalendarMode.REAL_WORLD) {
            String override = cal.data().realDateOverride();
            src.sendSuccess(() -> Component.literal("Timezone: " + cal.settings().realWorldZone()
                    + (override != null ? "  override: " + override : "")
                    + (cal.data().realDayOffset() != 0 ? "  offset: " + cal.data().realDayOffset() + " days" : "")), false);
        }
        CalendarSnapshot sched = cal.schedulingSnapshot();
        if (sched != s) {
            src.sendSuccess(() -> Component.literal("Holidays are scheduled on the season calendar: " + sched.date()), false);
        }
        return 1;
    }

    private static int villages(CommandContext<CommandSourceStack> ctx) {
        var server = EverFestServer.require();
        var director = server.director();
        var festival = director.festival();
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("Festival: " + (festival == null ? "none" : festival.holiday().id() + " " + festival.phase())
                + "  tracked villagers: " + director.trackedCount() + "  running tasks: " + director.activeTasks()
                + "  decoration entries: " + server.ledger().size()), false);
        src.sendSuccess(() -> Component.literal("Assigned tasks: " + director.taskCounts()), false);
        int[] onFestivalSchedule = {0};
        for (var level : server.server().getAllLevels()) {
            for (var e : level.getAllEntities()) {
                if (e instanceof net.minecraft.world.entity.npc.Villager v
                        && com.holidayseasons.villager.HolidaySchedules.isOurs(v.getBrain().getSchedule())) onFestivalSchedule[0]++;
            }
        }
        src.sendSuccess(() -> Component.literal("Villagers on festival schedules: " + onFestivalSchedule[0]), false);
        director.liveVillageSizes().forEach((k, n) -> {
            var rec = director.villages().get(k);
            String occ = festival == null ? null : festival.occurrence().key();
            src.sendSuccess(() -> Component.literal(" " + k + ": " + n + " villagers, style " + (rec == null ? "?" : rec.style().id())
                    + (occ == null ? "" : ", decorations up " + server.ledger().decorationsFor(k, occ)
                    + " / target " + director.decorationTargetFor(festival))), false);
        });
        return 1;
    }

    /** The village nearest to a player (within 96 blocks), as the director knows it. */
    private static String nearestVillage(ServerPlayer p) {
        var director = EverFestServer.require().director();
        String best = null;
        double bestDist = 96 * 96;
        for (var rec : director.villages().all()) {
            if (!rec.key.startsWith(p.level().dimension().location() + "|bell|")) continue;
            double d = rec.centre().distSqr(p.blockPosition());
            if (d < bestDist) {
                best = rec.key;
                bestDist = d;
            }
        }
        return best;
    }

    private static int showReputation(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        String village = nearestVillage(p);
        if (village == null) {
            ctx.getSource().sendFailure(Component.translatable("holidayseasons.reputation.no_village"));
            return 0;
        }
        int rep = EverFestServer.require().reputation().get(p.getUUID(), village);
        var tier = com.holidayseasons.reputation.ReputationMath.tier(rep);
        ctx.getSource().sendSuccess(() -> Component.translatable("holidayseasons.reputation.show", rep,
                Component.translatable("holidayseasons.reputation.tier." + tier.name().toLowerCase(Locale.ROOT))), false);
        return rep;
    }

    private static int setReputation(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
        int points = IntegerArgumentType.getInteger(ctx, "points");
        String village = nearestVillage(p);
        if (village == null) {
            ctx.getSource().sendFailure(Component.translatable("holidayseasons.reputation.no_village"));
            return 0;
        }
        EverFestServer.require().reputation().set(p.getUUID(), village, points);
        ctx.getSource().sendSuccess(() -> Component.literal("Reputation of " + p.getGameProfile().getName() + " in " + village + " set to " + points), true);
        return 1;
    }

    private static int halloweenStatus(CommandContext<CommandSourceStack> ctx) {
        var s = EverFestServer.require();
        var h = s.halloween();
        ctx.getSource().sendSuccess(() -> Component.literal("Halloween: " + (h.halloween() == null ? "not live" : h.halloween().phase())
                + "  night: " + h.isHalloweenNight() + "  trick-or-treat groups out: " + h.activeGroups()
                + "  adults handing out candy: " + h.giverCount()), false);
        h.describeGroups().forEach(line -> ctx.getSource().sendSuccess(() -> Component.literal(" " + line), false));
        return 1;
    }

    private static int editHome(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        var homes = EverFestServer.require().homes();
        var home = homes.ownedBy(p.getUUID()).stream()
                .filter(h -> h.dimension == p.level().dimension() && h.hearth.distSqr(p.blockPosition()) < 16 * 16)
                .findFirst().orElse(null);
        if (home == null) {
            ctx.getSource().sendFailure(Component.translatable("holidayseasons.home.none_nearby"));
            return 0;
        }
        homes.refreshEntrance(p.serverLevel(), home);
        PacketDistributor.sendToPlayer(p, com.holidayseasons.network.OpenHearthPayload.of(home));
        return 1;
    }

    private static int listHomes(CommandContext<CommandSourceStack> ctx) {
        var homes = EverFestServer.require().homes();
        ServerPlayer p = ctx.getSource().getPlayer();
        var list = p != null && !ctx.getSource().hasPermission(ADMIN_LEVEL) ? homes.ownedBy(p.getUUID()) : java.util.List.copyOf(homes.all());
        ctx.getSource().sendSuccess(() -> Component.literal(list.size() + " registered homes"), false);
        for (var h : list) {
            ctx.getSource().sendSuccess(() -> Component.literal(" " + h.name() + " (" + h.ownerName() + ") hearth " + h.hearth.toShortString()
                    + " entrance " + h.entrance().toShortString() + " visitors " + h.acceptVisitors() + " visits " + h.visitsThisYear()), false);
        }
        return list.size();
    }

    private static int registerHome(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ServerPlayer owner = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "owner");
        var level = ctx.getSource().getLevel();
        if (!level.getBlockState(pos).is(com.holidayseasons.block.EverFestBlocks.HOLIDAY_HEARTH.get())
                || !(level.getBlockEntity(pos) instanceof com.holidayseasons.block.entity.HearthBlockEntity be)) {
            ctx.getSource().sendFailure(Component.literal("There is no Holiday Hearth at " + pos.toShortString()));
            return 0;
        }
        var homes = EverFestServer.require().homes();
        if (homes.atHearth(level.dimension(), pos) != null) {
            ctx.getSource().sendFailure(Component.literal("That hearth is already registered"));
            return 0;
        }
        var home = homes.register(level, owner.getUUID(), owner.getGameProfile().getName(), pos);
        be.setHomeId(home.id);
        ctx.getSource().sendSuccess(() -> Component.translatable("holidayseasons.home.registered", home.name(), home.entrance().toShortString()), true);
        return 1;
    }

    private static int setDate(CommandContext<CommandSourceStack> ctx) {
        String raw = StringArgumentType.getString(ctx, "date");
        CalendarDate target;
        try {
            target = CalendarDate.parseIso(raw);
        } catch (RuntimeException e) {
            ctx.getSource().sendFailure(Component.translatable("holidayseasons.command.bad_date", raw));
            return 0;
        }
        String error = calendar().setDate(target);
        if (error != null) {
            ctx.getSource().sendFailure(Component.literal(error));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("holidayseasons.command.date_set", target.toIsoString()), true);
        return 1;
    }

    private static int addDays(CommandContext<CommandSourceStack> ctx) {
        int days = IntegerArgumentType.getInteger(ctx, "days");
        String result = calendar().addDays(days);
        ctx.getSource().sendSuccess(() -> Component.literal(result), true);
        return 1;
    }

    private static int clearOverride(CommandContext<CommandSourceStack> ctx) {
        calendar().clearRealWorldOverride();
        ctx.getSource().sendSuccess(() -> Component.translatable("holidayseasons.command.override_cleared"), true);
        return 1;
    }
}
