package com.holidayseasons.reputation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Every player's Holiday Collection Book, saved with the world (multiplayer-safe: keyed by player UUID). */
public final class CollectionData extends SavedData {
    public static final String FILE_ID = "holidayseasons_collection";
    public static final Factory<CollectionData> FACTORY = new Factory<>(CollectionData::new, CollectionData::load, null);

    public static final class Book {
        public final Map<String, Set<Integer>> celebrated = new LinkedHashMap<>();
        public final Set<String> items = new LinkedHashSet<>();
        public final Set<String> wildlife = new LinkedHashSet<>();
        public final Set<String> encounters = new LinkedHashSet<>();
        public final Map<String, Integer> stats = new LinkedHashMap<>();

        public int stat(String key) {
            return stats.getOrDefault(key, 0);
        }

        public int finishedAchievements() {
            int n = 0;
            for (var e : Collectibles.ACHIEVEMENTS.entrySet()) if (stat(e.getKey()) >= e.getValue()) n++;
            return n;
        }

        public int completion() {
            return Collectibles.completion(items.size(), wildlife.size(), encounters.size(), finishedAchievements());
        }

        public CompoundTag toTag() {
            CompoundTag t = new CompoundTag();
            CompoundTag cel = new CompoundTag();
            celebrated.forEach((h, years) -> cel.put(h, new IntArrayTag(years.stream().mapToInt(Integer::intValue).toArray())));
            t.put("celebrated", cel);
            t.put("items", strings(items));
            t.put("wildlife", strings(wildlife));
            t.put("encounters", strings(encounters));
            CompoundTag st = new CompoundTag();
            stats.forEach(st::putInt);
            t.put("stats", st);
            return t;
        }

        public static Book fromTag(CompoundTag t) {
            Book b = new Book();
            CompoundTag cel = t.getCompound("celebrated");
            for (String h : cel.getAllKeys()) {
                Set<Integer> years = new TreeSet<>();
                for (int y : cel.getIntArray(h)) years.add(y);
                b.celebrated.put(h, years);
            }
            readStrings(t.getList("items", Tag.TAG_STRING), b.items);
            readStrings(t.getList("wildlife", Tag.TAG_STRING), b.wildlife);
            readStrings(t.getList("encounters", Tag.TAG_STRING), b.encounters);
            CompoundTag st = t.getCompound("stats");
            for (String k : st.getAllKeys()) b.stats.put(k, st.getInt(k));
            return b;
        }

        private static ListTag strings(Set<String> set) {
            ListTag l = new ListTag();
            set.forEach(s -> l.add(StringTag.valueOf(s)));
            return l;
        }

        private static void readStrings(ListTag l, Set<String> into) {
            for (Tag t : l) into.add(t.getAsString());
        }
    }

    private final Map<UUID, Book> books = new HashMap<>();

    public Book book(UUID player) {
        return books.computeIfAbsent(player, u -> new Book());
    }

    public static CollectionData load(CompoundTag tag, HolderLookup.Provider registries) {
        CollectionData d = new CollectionData();
        CompoundTag all = tag.getCompound("books");
        for (String k : all.getAllKeys()) d.books.put(UUID.fromString(k), Book.fromTag(all.getCompound(k)));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        books.forEach((id, b) -> all.put(id.toString(), b.toTag()));
        tag.put("books", all);
        return tag;
    }
}
