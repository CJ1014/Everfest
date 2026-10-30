package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.block.EverFestBlocks;
import com.holidayseasons.block.WallCalendarBlock;
import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarService;
import com.holidayseasons.holiday.HolidayEngineCore;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayService;
import com.holidayseasons.holiday.HolidayStateData;
import com.holidayseasons.holiday.HolidayTransition;
import com.holidayseasons.holiday.HolidayWindow;
import com.holidayseasons.holiday.OccurrenceRecord;
import com.holidayseasons.holiday.ScheduledHoliday;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicReference;

/**
 * In-world tests for the calendar and holiday engine. Tests that move the global calendar run in their own batches
 * so they never overlap.
 */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CalendarGameTests {
    private CalendarGameTests() {
    }

    @GameTest(template = "empty_5x5")
    public static void wallCalendarNeedsSupport(GameTestHelper helper) {
        BlockPos support = new BlockPos(2, 1, 3);
        BlockPos calendar = new BlockPos(2, 1, 2);
        helper.setBlock(support, Blocks.OAK_PLANKS);
        // Facing NORTH = hangs on the block to its south.
        helper.setBlock(calendar, EverFestBlocks.WALL_CALENDAR.get().defaultBlockState().setValue(WallCalendarBlock.FACING, Direction.NORTH));
        helper.assertBlockPresent(EverFestBlocks.WALL_CALENDAR.get(), calendar);
        helper.startSequence()
                .thenExecute(() -> helper.setBlock(support, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertBlockNotPresent(EverFestBlocks.WALL_CALENDAR.get(), calendar))
                .thenSucceed();
    }

    @GameTest(template = "empty_5x5")
    public static void wallCalendarCannotFloat(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        boolean survives = EverFestBlocks.WALL_CALENDAR.get().defaultBlockState()
                .canSurvive(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(!survives, "A wall calendar with nothing behind it must not survive");
        helper.succeed();
    }

    @GameTest(template = "empty_5x5", batch = "everfest_calendar_1", timeoutTicks = 600)
    public static void holidayLifecycleRunsOnceAcrossTimeTravel(GameTestHelper helper) {
        EverFestServer s = EverFestServer.require();
        CalendarService cal = s.calendar();
        HolidayService holidays = s.holidays();
        String id = EverFest.MOD_ID + ":halloween";
        ScheduledHoliday halloween = holidays.holiday(id).orElseThrow();
        HolidayEngineCore.Context ctx = holidays.context();
        int year = ctx.currentYear() + 1;
        HolidayWindow w = halloween.window(year, ctx.system());
        CalendarDate prepDate = ctx.system().fromEpochDay(w.prepStart());
        CalendarDate startDate = ctx.system().fromEpochDay(w.start());
        CalendarDate beforeDate = ctx.system().fromEpochDay(w.prepStart() - 3);
        String key = OccurrenceRecord.key(id, ctx.system().signature(), year);
        AtomicReference<String> error = new AtomicReference<>();

        helper.startSequence()
                .thenExecute(() -> error.set(cal.setDate(prepDate)))
                .thenWaitUntil(() -> {
                    helper.assertTrue(error.get() == null, "setDate failed: " + error.get());
                    OccurrenceRecord r = holidays.records().get(key);
                    helper.assertTrue(r != null && r.phase() == HolidayPhase.PREPARING, "expected PREPARING, got " + describe(r));
                })
                .thenExecute(() -> error.set(cal.setDate(startDate)))
                .thenWaitUntil(() -> {
                    OccurrenceRecord r = holidays.records().get(key);
                    helper.assertTrue(r != null && r.phase() == HolidayPhase.ACTIVE && r.activations() == 1, "expected ACTIVE once, got " + describe(r));
                })
                // Rewind to before preparation: the occurrence is aborted back to INACTIVE.
                .thenExecute(() -> error.set(cal.setDate(beforeDate)))
                .thenWaitUntil(() -> {
                    OccurrenceRecord r = holidays.records().get(key);
                    helper.assertTrue(r != null && r.phase() == HolidayPhase.INACTIVE, "expected INACTIVE after rewind, got " + describe(r));
                })
                // Replay the holiday: it must not be celebrated a second time.
                .thenExecute(() -> error.set(cal.setDate(startDate)))
                .thenWaitUntil(() -> {
                    OccurrenceRecord r = holidays.records().get(key);
                    helper.assertTrue(r != null && r.phase() == HolidayPhase.COMPLETED, "expected COMPLETED, got " + describe(r));
                    helper.assertTrue(r.activations() == 1, "activated " + r.activations() + " times");
                    long activeEvents = holidays.recentTransitions().stream()
                            .filter(t -> t.occurrenceKey().equals(key) && t.to() == HolidayPhase.ACTIVE && t.kind() == HolidayTransition.Kind.ENTER)
                            .count();
                    helper.assertTrue(activeEvents == 1, "ACTIVE entered " + activeEvents + " times");
                })
                .thenExecute(() -> {
                    // Persistence round trip of every record.
                    HolidayStateData data = new HolidayStateData();
                    data.records().putAll(holidays.records());
                    CompoundTag tag = data.save(new CompoundTag(), helper.getLevel().registryAccess());
                    HolidayStateData loaded = HolidayStateData.load(tag, helper.getLevel().registryAccess());
                    helper.assertTrue(loaded.records().size() == holidays.records().size(), "record count survives save/load");
                    OccurrenceRecord r = loaded.records().get(key);
                    helper.assertTrue(r != null && r.phase() == HolidayPhase.COMPLETED && r.activations() == 1, "record survives save/load: " + describe(r));
                })
                .thenSucceed();
    }

    @GameTest(template = "empty_5x5", batch = "everfest_calendar_2", timeoutTicks = 400)
    public static void dateAdvancesWithWorldClock(GameTestHelper helper) {
        CalendarService cal = EverFestServer.require().calendar();
        AtomicReference<Long> before = new AtomicReference<>();
        helper.startSequence()
                .thenExecute(() -> {
                    before.set(cal.snapshot().epochDay());
                    helper.getLevel().getServer().getCommands().performPrefixedCommand(
                            helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput().withPermission(4), "time add 24000");
                })
                .thenWaitUntil(() -> helper.assertTrue(cal.snapshot().epochDay() == before.get() + 1,
                        "expected one day later than " + before.get() + ", got " + cal.snapshot().epochDay()))
                .thenSucceed();
    }

    private static String describe(OccurrenceRecord r) {
        return r == null ? "no record" : r.phase() + " x" + r.activations() + (r.skipReason().isEmpty() ? "" : " (" + r.skipReason() + ")");
    }
}
