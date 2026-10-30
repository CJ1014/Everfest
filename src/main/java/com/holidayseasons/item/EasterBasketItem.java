package com.holidayseasons.item;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;

/** An Easter basket: a bundle for eggs, flowers and candy. */
public class EasterBasketItem extends BundleItem {
    public EasterBasketItem(Properties properties) {
        super(properties);
    }

    static boolean fits(ItemStack s) {
        return s.isEmpty() || s.is(EverFestItems.EGG_TAG) || s.is(ItemTags.SMALL_FLOWERS) || s.is(EverFestItems.CANDY_TAG);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack basket, Slot slot, ClickAction action, Player player) {
        if (action == ClickAction.SECONDARY && !fits(slot.getItem())) return false;
        return super.overrideStackedOnOther(basket, slot, action, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack basket, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action == ClickAction.SECONDARY && !fits(other)) return false;
        return super.overrideOtherStackedOnMe(basket, other, slot, action, player, access);
    }
}
