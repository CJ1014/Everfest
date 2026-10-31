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
