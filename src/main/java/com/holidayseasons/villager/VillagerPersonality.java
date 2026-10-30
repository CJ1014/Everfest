package com.holidayseasons.villager;

import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Deterministic holiday personality of a villager, derived from its UUID (which is persistent), its profession and
 * its age. The same villager always has the same personality, on every server, without storing anything.
 *
 * @param sociability   0..1, likes gatherings and talking to others
 * @param generosity    0..1, gives gifts and candy more often and more generously
 * @param enthusiasm    0..1, how often it joins holiday activities at all
 * @param preferred     favourite kind of holiday activity
 * @param giftPreference what it likes to receive (drives wish lists)
 * @param costume       preferred Halloween costume
 */
public record VillagerPersonality(float sociability, float generosity, float enthusiasm, Preferred preferred,
                                  GiftPreference giftPreference, Costume costume) {
    public enum Preferred { DECORATE, GATHER, SOCIALIZE, FEAST, GIFTS, GAMES }

    public enum GiftPreference { FOOD, BOOKS, FLOWERS, TOOLS, GEMS, TREATS, CLOTHES }

    public enum Costume { PUMPKIN, GHOST, SKELETON, WITCH, VAMPIRE, SCARECROW, BAT, BLACK_CAT }

    public static VillagerPersonality of(UUID uuid, String profession, boolean baby) {
        SplittableRandom r = new SplittableRandom(uuid.getMostSignificantBits() ^ Long.rotateLeft(uuid.getLeastSignificantBits(), 17) ^ 0x5EA5_0115L);
        float soc = (float) r.nextDouble();
        float gen = (float) r.nextDouble();
        float ent = (float) (0.35 + 0.65 * r.nextDouble());
        Preferred pref = Preferred.values()[r.nextInt(Preferred.values().length)];
        GiftPreference gift = GiftPreference.values()[r.nextInt(GiftPreference.values().length)];
        Costume costume = Costume.values()[r.nextInt(Costume.values().length)];
        // Profession and age shape the random base without removing individuality.
        switch (profession) {
            case "cleric" -> soc = lift(soc, 0.25f);
            case "librarian", "cartographer" -> {
                gen = lift(gen, 0.15f);
                if (r.nextInt(3) == 0) gift = GiftPreference.BOOKS;
            }
            case "farmer", "fisherman", "butcher" -> {
                if (r.nextInt(3) == 0) pref = Preferred.FEAST;
                if (r.nextInt(2) == 0) gift = GiftPreference.FOOD;
                if (profession.equals("farmer") && r.nextInt(2) == 0) costume = Costume.SCARECROW;
            }
            case "toolsmith", "weaponsmith", "armorer" -> {
                if (r.nextInt(2) == 0) gift = GiftPreference.TOOLS;
            }
            case "shepherd", "leatherworker" -> {
                if (r.nextInt(3) == 0) pref = Preferred.DECORATE;
                if (r.nextInt(2) == 0) gift = GiftPreference.CLOTHES;
            }
            case "mason" -> {
                if (r.nextInt(2) == 0) gift = GiftPreference.GEMS;
            }
            case "nitwit" -> {
                ent = lift(ent, 0.3f);
                if (r.nextInt(2) == 0) pref = Preferred.GAMES;
            }
            default -> {
            }
        }
        if (baby) {
            ent = lift(ent, 0.4f);
            soc = lift(soc, 0.2f);
            pref = r.nextInt(2) == 0 ? Preferred.GAMES : pref;
            gift = r.nextInt(2) == 0 ? GiftPreference.TREATS : gift;
        }
        return new VillagerPersonality(soc, gen, ent, pref, gift, costume);
    }

    private static float lift(float v, float amount) {
        return Math.min(1f, v + (1f - v) * amount);
    }
}
