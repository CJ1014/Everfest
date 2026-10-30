package com.holidayseasons.block.entity;

import com.holidayseasons.block.CandyBowlBlock;
import com.holidayseasons.item.EverFestItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Candy storage for a bowl. Only candy fits. Server-side bookkeeping limits how much each visiting player takes per
 * day; automation may insert but never extract, so a hopper cannot drain someone's bowl.
 */
public class CandyBowlBlockEntity extends BlockEntity {
    public static final int SLOTS = 3;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(EverFestItems.CANDY_TAG);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateFill();
        }
    };

    /** Automation view: insert only. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inventory.isItemValid(slot, stack);
        }
    };

    @Nullable
    private UUID owner;
    /** visitor -> "day:count" taken from this bowl. */
    private final Map<UUID, long[]> taken = new HashMap<>();
    private int dispensedTotal;

    public CandyBowlBlockEntity(BlockPos pos, BlockState state) {
        super(EverFestBlockEntities.CANDY_BOWL.get(), pos, state);
    }

    public IItemHandler automationHandler() {
        return automation;
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public int candyCount() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) n += inventory.getStackInSlot(i).getCount();
        return n;
    }

    public int dispensedTotal() {
        return dispensedTotal;
    }

    /** Adds candy from a stack; returns the remainder. */
    public ItemStack add(ItemStack stack) {
        ItemStack rest = stack;
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) rest = inventory.insertItem(i, rest, false);
        return rest;
    }

    /** Takes one random-ish candy (from the fullest slot), or empty. */
    public ItemStack takeOne() {
        int best = -1;
        for (int i = 0; i < SLOTS; i++) {
            if (!inventory.getStackInSlot(i).isEmpty() && (best < 0 || inventory.getStackInSlot(i).getCount() > inventory.getStackInSlot(best).getCount())) {
                best = i;
            }
        }
        if (best < 0) return ItemStack.EMPTY;
        ItemStack out = inventory.extractItem(best, 1, false);
        if (!out.isEmpty()) {
            dispensedTotal++;
            setChanged();
        }
        return out;
    }

    /** How many candies {@code visitor} already took on {@code day}. */
    public int takenOn(UUID visitor, long day) {
        long[] e = taken.get(visitor);
        return e != null && e[0] == day ? (int) e[1] : 0;
    }

    public void recordTaken(UUID visitor, long day) {
        long[] e = taken.get(visitor);
        if (e == null || e[0] != day) taken.put(visitor, new long[]{day, 1});
        else e[1]++;
        setChanged();
    }

    private void updateFill() {
        if (level == null || level.isClientSide) return;
        int n = candyCount();
        int fill = n == 0 ? 0 : n <= 8 ? 1 : n <= 32 ? 2 : 3;
        BlockState state = getBlockState();
        if (state.getValue(CandyBowlBlock.FILL) != fill) level.setBlock(worldPosition, state.setValue(CandyBowlBlock.FILL, fill), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        if (owner != null) tag.putUUID("owner", owner);
        tag.putInt("dispensed", dispensedTotal);
        CompoundTag t = new CompoundTag();
        taken.forEach((uuid, e) -> t.putLongArray(uuid.toString(), e));
        tag.put("taken", t);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        dispensedTotal = tag.getInt("dispensed");
        taken.clear();
        CompoundTag t = tag.getCompound("taken");
        for (String k : t.getAllKeys()) {
            long[] e = t.getLongArray(k);
            if (e.length == 2) taken.put(UUID.fromString(k), e);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", inventory.serializeNBT(registries));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
