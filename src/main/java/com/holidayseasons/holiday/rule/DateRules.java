package com.holidayseasons.holiday.rule;

import com.holidayseasons.calendar.SubSeason;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.time.DayOfWeek;
import java.util.Locale;

/**
 * Text form of date rules, used in holiday JSON ({@code "date": "fixed:10-31"}) and in the server config:
 * <ul>
 *     <li>{@code fixed:MM-DD} - Gregorian month/day, scaled to native months</li>
 *     <li>{@code nth_weekday:MM:weekday:N} - N-th weekday of a month, N = -1 for the last</li>
 *     <li>{@code easter:OFFSET} - Western Easter Sunday plus OFFSET days</li>
 *     <li>{@code native:MM-DD} - exact native date, no scaling</li>
 *     <li>{@code season:sub_season:FRACTION} - point inside a sub-season's month (0..1)</li>
 * </ul>
 */
public final class DateRules {
    public static final Codec<DateRule> CODEC = Codec.STRING.comapFlatMap(DateRules::tryParse, DateRule::describe);

    private DateRules() {
    }

    public static DataResult<DateRule> tryParse(String text) {
        try {
            return DataResult.success(parse(text));
        } catch (RuntimeException e) {
            return DataResult.error(() -> "Invalid date rule '" + text + "': " + e.getMessage());
        }
    }

    public static DateRule parse(String text) {
        String t = text.trim().toLowerCase(Locale.ROOT);
        int colon = t.indexOf(':');
        if (colon < 0) throw new IllegalArgumentException("expected type:arguments");
        String type = t.substring(0, colon);
        String args = t.substring(colon + 1);
        return switch (type) {
            case "fixed" -> {
                int[] md = monthDay(args);
                yield new FixedDateRule(md[0], md[1]);
            }
            case "native" -> {
                int[] md = monthDay(args);
                yield new NativeDateRule(md[0], md[1]);
            }
            case "nth_weekday" -> {
                String[] p = args.split(":");
                if (p.length != 3) throw new IllegalArgumentException("expected nth_weekday:MM:weekday:N");
                yield new NthWeekdayRule(Integer.parseInt(p[0]), DayOfWeek.valueOf(p[1].toUpperCase(Locale.ROOT)), Integer.parseInt(p[2]));
            }
            case "easter" -> new EasterRule(args.isEmpty() ? 0 : Integer.parseInt(args));
            case "season" -> {
                String[] p = args.split(":");
                if (p.length != 2) throw new IllegalArgumentException("expected season:sub_season:fraction");
                yield new SeasonPointRule(SubSeason.valueOf(p[0].toUpperCase(Locale.ROOT)), Double.parseDouble(p[1]));
            }
            default -> throw new IllegalArgumentException("unknown rule type '" + type + "'");
        };
    }

    private static int[] monthDay(String s) {
        String[] p = s.split("-");
        if (p.length != 2) throw new IllegalArgumentException("expected MM-DD");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])};
    }
}
