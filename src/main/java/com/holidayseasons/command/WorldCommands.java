package com.holidayseasons.command;

import com.holidayseasons.entity.SeasonalWildlife;
import com.holidayseasons.entity.WildlifeSpawner;
import com.holidayseasons.server.EverFestServer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Locale;

/** Operator commands for EverFest's world expansion: {@code /everfest wildlife [spawn <species>]}. */
final class WorldCommands {
    private WorldCommands() {
    }

    static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        LiteralArgumentBuilder<CommandSourceStack> wildlife = Commands.literal("wildlife")
                .requires(s -> s.hasPermission(EverFestCommands.ADMIN_LEVEL)).executes(WorldCommands::wildlifeStatus);
        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn");
        for (SeasonalWildlife.Species s : SeasonalWildlife.Species.values()) {
            spawn.then(Commands.literal(s.id).executes(ctx -> spawnNow(ctx, s)));
        }
        root.then(wildlife.then(spawn));
        root.then(Commands.literal("merchant").requires(s -> s.hasPermission(EverFestCommands.ADMIN_LEVEL))
                .then(Commands.literal("visit").executes(WorldCommands::merchantVisit))
                .then(Commands.literal("peddler").executes(WorldCommands::peddler)));
    }

    private static com.holidayseasons.ai.CelebrationDirector.Festival activeFestival(CommandSourceStack src) {
        var f = EverFestServer.require().director().festival();
        if (f == null || f.phase() != com.holidayseasons.holiday.HolidayPhase.ACTIVE) {
            src.sendFailure(Component.literal("No holiday is being celebrated right now"));
            return null;
        }
        return f;
    }

    private static int merchantVisit(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        var f = activeFestival(src);
        if (f == null) return 0;
        EverFestServer s = EverFestServer.require();
        BlockPos at = BlockPos.containing(src.getPosition());
        com.holidayseasons.village.VillageRecord nearest = null;
        for (var r : s.director().villages().all()) {
            if (!r.key.contains("|bell|")) continue;
            if (nearest == null || r.centre().distSqr(at) < nearest.centre().distSqr(at)) nearest = r;
        }
        if (nearest == null) {
            src.sendFailure(Component.literal("No known village"));
            return 0;
        }
        var m = s.merchants().visitVillage(src.getLevel(), f, nearest, true);
        if (m == null) {
            src.sendFailure(Component.literal("No merchant could come (one is already there, no player nearby, or no room)"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("A " + f.holiday().id() + " merchant arrived at " + m.blockPosition().toShortString()), true);
        return 1;
    }

    private static int peddler(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        var f = activeFestival(src);
        if (f == null) return 0;
        var m = EverFestServer.require().merchants().peddlerVisits(src.getLevel(), f, src.getPlayerOrException(), true);
        if (m == null) {
            src.sendFailure(Component.literal("The peddler cannot come (already visited you this Halloween, or no room)"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("The Midnight Peddler appeared at " + m.blockPosition().toShortString()), true);
        return 1;
    }

    private static int wildlifeStatus(CommandContext<CommandSourceStack> ctx) {
        EverFestServer s = EverFestServer.require();
        CommandSourceStack src = ctx.getSource();
        WildlifeSpawner w = s.wildlife();
        src.sendSuccess(() -> Component.literal("Season: " + s.calendar().snapshot().season().name().toLowerCase(Locale.ROOT)
                + "  visiting now: " + w.inSeasonNow()), false);
        ServerLevel level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        for (SeasonalWildlife.Species sp : SeasonalWildlife.Species.values()) {
            int n = w.nearby(level, at, sp);
            src.sendSuccess(() -> Component.literal(" " + sp.id + ": " + n + " within 64 blocks"
                    + (WildlifeSpawner.enabled(sp) ? "" : " (disabled)")), false);
        }
        return 1;
    }

    private static int spawnNow(CommandContext<CommandSourceStack> ctx, SeasonalWildlife.Species s) {
        CommandSourceStack src = ctx.getSource();
        int n = EverFestServer.require().wildlife().trySpawn(src.getLevel(), BlockPos.containing(src.getPosition()), s, true);
        if (n == 0) {
            src.sendFailure(Component.literal("No " + s.id + " could arrive here (wrong biome, no room, disabled, or the local cap is reached)"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(n + " " + s.id + " arrived nearby"), true);
        return n;
    }
}
