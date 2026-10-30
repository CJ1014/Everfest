package com.holidayseasons.item;

import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;

/** A trick-or-treat bag: works like a bundle, but only candy goes in. */
public class CandyBagItem extends BundleItem {
    public CandyBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack bag, Slot slot, ClickAction action, Player player) {
        ItemStack target = slot.getItem();
        if (action == ClickAction.SECONDARY && !target.isEmpty() && !target.is(EverFestItems.CANDY_TAG)) return false;
        return super.overrideStackedOnOther(bag, slot, action, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack bag, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action == ClickAction.SECONDARY && !other.isEmpty() && !other.is(EverFestItems.CANDY_TAG)) return false;
        return super.overrideOtherStackedOnMe(bag, other, slot, action, player, access);
    }
}
