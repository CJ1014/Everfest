package com.holidayseasons.economy;

import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.item.EverFestItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Optional festival tokens. When enabled in the server config they are earned only through community activities that
 * are already limited per holiday (finding hidden eggs, a liked present per villager, village objectives, hosting a
 * party), and spent at holiday merchants on token-only collectibles. Nothing buys tokens back, so there is no loop.
 */
public final class HolidayTokens {
    private HolidayTokens() {
    }

    public static boolean enabled() {
        return ServerConfig.SPEC.isLoaded() && ServerConfig.INSTANCE.holidayTokens.get();
    }

    /** Gives {@code count} tokens if tokens are enabled. Returns how many were given. */
    public static int award(Player player, int count, String reasonKey) {
        if (!enabled() || count <= 0 || player.level().isClientSide) return 0;
        ItemStack tokens = new ItemStack(EverFestItems.HOLIDAY_TOKEN.get(), count);
        if (!player.getInventory().add(tokens)) player.drop(tokens, false);
        player.displayClientMessage(Component.translatable("holidayseasons.tokens.earned", count, Component.translatable(reasonKey)), true);
        return count;
    }
}
