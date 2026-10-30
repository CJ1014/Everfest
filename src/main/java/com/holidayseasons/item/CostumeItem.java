package com.holidayseasons.item;

import com.holidayseasons.villager.VillagerPersonality;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A wearable Halloween costume piece. Rendered on heads by vanilla's head layer (players and villagers alike) using
 * the item model's {@code head} transform; no armour value.
 */
public class CostumeItem extends Item implements Equipable {
    /** Custom-data key marking a costume EverFest put on a villager; such stacks are removed after Halloween. */
    public static final String TEMPORARY_KEY = "holidayseasons_temporary_costume";

    private final VillagerPersonality.Costume costume;

    public CostumeItem(VillagerPersonality.Costume costume, Properties properties) {
        super(properties);
        this.costume = costume;
    }

    public VillagerPersonality.Costume costume() {
        return costume;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    public static boolean isTemporary(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(TEMPORARY_KEY);
    }

    public static ItemStack temporary(ItemStack stack, String occurrenceKey) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TEMPORARY_KEY, occurrenceKey));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.holidayseasons.costume.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
