package com.holidayseasons.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Remembers which registered home a Holiday Hearth belongs to. */
public class HearthBlockEntity extends BlockEntity {
    @Nullable
    private UUID homeId;

    public HearthBlockEntity(BlockPos pos, BlockState state) {
        super(EverFestBlockEntities.HEARTH.get(), pos, state);
    }

    @Nullable
    public UUID homeId() {
        return homeId;
    }

    public void setHomeId(@Nullable UUID id) {
        this.homeId = id;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (homeId != null) tag.putUUID("home", homeId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        homeId = tag.hasUUID("home") ? tag.getUUID("home") : null;
    }
}
