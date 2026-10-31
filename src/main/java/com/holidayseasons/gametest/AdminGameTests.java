package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.config.AdminConfig;
import com.holidayseasons.config.ServerConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/** Tests for remote editing of the server config by operators. */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdminGameTests {
    private AdminGameTests() {
    }

    private static AdminConfig.Entry entry(List<AdminConfig.Entry> all, String path) {
        return all.stream().filter(e -> e.path().equals(path)).findFirst().orElseThrow(() -> new IllegalStateException("missing " + path));
    }

    @GameTest(template = "empty_5x5", batch = "everfest_admin")
    public static void settingsAreDescribedAndEditedAllOrNothing(GameTestHelper helper) {
        List<AdminConfig.Entry> all = AdminConfig.describe();
        helper.assertTrue(all.size() > 60, "every setting is described: " + all.size());
        AdminConfig.Entry cap = entry(all, "wildlife.maxSeasonalAnimalsNearPlayer");
        helper.assertTrue(cap.type() == AdminConfig.Type.INT && cap.min().equals("0") && cap.max().equals("32"), "int with its range");
        helper.assertTrue(entry(all, "calendar.mode").type() == AdminConfig.Type.ENUM && !entry(all, "calendar.mode").options().isEmpty(), "enum with options");
        helper.assertTrue(entry(all, "calendar.customMonthLengths").type() == AdminConfig.Type.INT_LIST, "int list");
        helper.assertTrue(entry(all, "holidays.disabledHolidays").type() == AdminConfig.Type.STRING_LIST, "string list");

        var cfg = ServerConfig.INSTANCE;
        int capBefore = cfg.maxSeasonalAnimalsNearPlayer.get();
        boolean tokensBefore = cfg.holidayTokens.get();
        List<? extends Integer> monthsBefore = cfg.customMonthLengths.get();
        String bad = AdminConfig.apply(Map.of("wildlife.maxSeasonalAnimalsNearPlayer", "7", "economy.holidayTokens", "maybe"));
        helper.assertTrue(bad != null && cfg.maxSeasonalAnimalsNearPlayer.get() == capBefore, "a batch with one bad value changes nothing");
        helper.assertTrue(AdminConfig.apply(Map.of("wildlife.maxSeasonalAnimalsNearPlayer", "99")) != null, "out of range refused");
        helper.assertTrue(AdminConfig.apply(Map.of("no.such.setting", "1")) != null, "unknown setting refused");
        String ok = AdminConfig.apply(Map.of("wildlife.maxSeasonalAnimalsNearPlayer", "7", "economy.holidayTokens", String.valueOf(!tokensBefore),
                "calendar.customMonthLengths", "31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31"));
        helper.assertTrue(ok == null, "valid batch applied: " + ok);
        helper.assertTrue(cfg.maxSeasonalAnimalsNearPlayer.get() == 7 && cfg.holidayTokens.get() != tokensBefore, "values changed");
        helper.assertTrue(cfg.customMonthLengths.get().size() == 12 && cfg.customMonthLengths.get().get(1) == 28, "list parsed");
        // Restore.
        String restore = AdminConfig.apply(Map.of("wildlife.maxSeasonalAnimalsNearPlayer", String.valueOf(capBefore),
                "economy.holidayTokens", String.valueOf(tokensBefore),
                "calendar.customMonthLengths", String.join(", ", monthsBefore.stream().map(String::valueOf).toList())));
        helper.assertTrue(restore == null && cfg.maxSeasonalAnimalsNearPlayer.get() == capBefore, "restored");
        helper.succeed();
    }
}
