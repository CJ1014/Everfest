package com.holidayseasons.block.entity;

import com.holidayseasons.item.EverFestDataComponents;
import com.holidayseasons.item.PresentInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A present: nine slots of contents plus its wrapping info. Like a shulker box, breaking it keeps the contents in the
 * dropped item, so presents can be carried and handed over without duplicating anything.
 */
public class PresentBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 9;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private PresentInfo info = PresentInfo.EMPTY;
    @Nullable
    private UUID owner;

    public PresentBlockEntity(BlockPos pos, BlockState state) {
        super(EverFestBlockEntities.PRESENT.get(), pos, state);
    }

    public PresentInfo info() {
        return info;
    }

    public void setInfo(PresentInfo info) {
        this.info = info;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.holidayseasons.present");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, this, 1);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Presents inside presents would allow unbounded nesting; keep it to one level.
        return !info.wrapped() && !(stack.getItem() instanceof com.holidayseasons.item.PresentItem);
    }

    @Override
    public boolean stillValid(Player player) {
        return !info.wrapped() && super.stillValid(player);
    }

    /** Removes and returns all contents (used when the present is unwrapped). */
    public NonNullList<ItemStack> takeAll() {
        NonNullList<ItemStack> out = items;
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        setChanged();
        return out;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        info = tag.contains("present") ? PresentInfo.CODEC.parse(NbtOps.INSTANCE, tag.get("present")).result().orElse(PresentInfo.EMPTY) : PresentInfo.EMPTY;
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        PresentInfo.CODEC.encodeStart(NbtOps.INSTANCE, info).result().ifPresent(t -> tag.put("present", t));
        if (owner != null) tag.putUUID("owner", owner);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        info = input.getOrDefault(EverFestDataComponents.PRESENT.get(), PresentInfo.EMPTY);
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(EverFestDataComponents.PRESENT.get(), info);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Items");
        tag.remove("present");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        PresentInfo.CODEC.encodeStart(NbtOps.INSTANCE, info).result().ifPresent(t -> tag.put("present", t));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
