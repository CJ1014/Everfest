package com.holidayseasons.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** State shared by a small group of trick-or-treating children: the leader decides, followers mirror. */
public final class TrickOrTreatGroup {
    public enum Phase { WALKING, KNOCKING, WAITING, TREAT, TRICK, DONE }

    /** A door the group can visit. */
    public record Destination(String key, BlockPos entrance, BlockPos door, @Nullable java.util.UUID homeId) {
    }

    public final Villager leader;
    public final List<Villager> members = new ArrayList<>();
    public final Set<String> visited = new HashSet<>();
    public Phase phase = Phase.WALKING;
    @Nullable
    public Destination current;
    public ItemStack lastCandy = ItemStack.EMPTY;
    public int treats;
    public int tricks;
    public int doorsVisited;

    public TrickOrTreatGroup(Villager leader) {
        this.leader = leader;
    }

    public boolean isActive() {
        return phase != Phase.DONE && leader.isAlive() && !leader.isRemoved();
    }

    public int size() {
        return 1 + members.size();
    }
}
