package com.holidayseasons.item;

import com.holidayseasons.EverFest;
import com.holidayseasons.block.EverFestBlocks;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EverFestItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EverFest.MOD_ID);

    public static final DeferredItem<WallCalendarItem> WALL_CALENDAR = ITEMS.register("wall_calendar",
            () -> new WallCalendarItem(EverFestBlocks.WALL_CALENDAR.get(), new Item.Properties().stacksTo(16)));

    private EverFestItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
