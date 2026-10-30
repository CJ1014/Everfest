package com.holidayseasons.command;

import com.holidayseasons.calendar.CalendarSystem;
import com.holidayseasons.holiday.HolidayEngineCore;
import com.holidayseasons.holiday.HolidayService;
import com.holidayseasons.holiday.HolidayStatus;
import com.holidayseasons.holiday.HolidayWindow;
import com.holidayseasons.holiday.ScheduledHoliday;
import com.holidayseasons.server.EverFestServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/** {@code /everfest holiday list|info|goto|reset|finish}. */
final class HolidayCommands {
    private static final SuggestionProvider<CommandSourceStack> HOLIDAYS = (ctx, builder) -> SharedSuggestionProvider.suggest(
            holidays().holidays().stream().map(ScheduledHoliday::id).map(HolidayCommands::shortId), builder);

    private HolidayCommands() {
    }

    static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("holiday")
                .then(Commands.literal("list").executes(HolidayCommands::list))
                .then(Commands.literal("info").then(Commands.argument("holiday", StringArgumentType.string()).suggests(HOLIDAYS)
                        .executes(HolidayCommands::info)))
                .then(Commands.literal("goto").requires(s -> s.hasPermission(EverFestCommands.ADMIN_LEVEL))
                        .then(Commands.argument("holiday", StringArgumentType.string()).suggests(HOLIDAYS)
                                .executes(ctx -> gotoHoliday(ctx, false))
                                .then(Commands.literal("prepare").executes(ctx -> gotoHoliday(ctx, true)))
                                .then(Commands.literal("start").executes(ctx -> gotoHoliday(ctx, false)))))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(EverFestCommands.ADMIN_LEVEL))
                        .then(Commands.argument("holiday", StringArgumentType.string()).suggests(HOLIDAYS).executes(HolidayCommands::reset)))
                .then(Commands.literal("finish").requires(s -> s.hasPermission(EverFestCommands.ADMIN_LEVEL))
                        .then(Commands.argument("holiday", StringArgumentType.string()).suggests(HOLIDAYS).executes(HolidayCommands::finish))));
    }

    static HolidayService holidays() {
        return EverFestServer.require().holidays();
    }

    static String shortId(String id) {
        return id.startsWith("holidayseasons:") ? id.substring("holidayseasons:".length()) : id;
    }

    private static Optional<ScheduledHoliday> find(CommandContext<CommandSourceStack> ctx) {
        String raw = StringArgumentType.getString(ctx, "holiday");
        String id = raw.contains(":") ? raw : "holidayseasons:" + raw;
        Optional<ScheduledHoliday> h = holidays().holiday(id);
        if (h.isEmpty()) ctx.getSource().sendFailure(Component.translatable("holidayseasons.command.unknown_holiday", raw));
        return h;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        HolidayService svc = holidays();
        ctx.getSource().sendSuccess(() -> Component.translatable("holidayseasons.command.holiday_list_header").withStyle(ChatFormatting.GOLD), false);
        for (ScheduledHoliday h : svc.holidays()) {
            HolidayStatus next = svc.nextStatus(h.id());
            Component name = Component.translatable(HolidayService.statusKey(h.id())).withColor(h.color() & 0xFFFFFF);
            Component line;
            if (!h.enabled()) {
                line = Component.literal(" - ").append(name).append(Component.literal(" (disabled)").withStyle(ChatFormatting.DARK_GRAY));
            } else if (next == null) {
                line = Component.literal(" - ").append(name).append(Component.literal(" (nothing scheduled)").withStyle(ChatFormatting.GRAY));
            } else {
                line = Component.literal(" - ").append(name).append(Component.literal(" " + next.start() + " " + next.phase()
                        + (next.daysUntilStart() > 0 ? ", in " + next.daysUntilStart() + " days" : "")).withStyle(ChatFormatting.GRAY));
            }
            Component finalLine = line;
            ctx.getSource().sendSuccess(() -> finalLine, false);
        }
        return svc.holidays().size();
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        Optional<ScheduledHoliday> oh = find(ctx);
        if (oh.isEmpty()) return 0;
        ScheduledHoliday h = oh.get();
        HolidayService svc = holidays();
        HolidayEngineCore.Context c = svc.context();
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.translatable(HolidayService.statusKey(h.id())).withColor(h.color() & 0xFFFFFF)
                .append(Component.literal(" [" + h.id() + "]").withStyle(ChatFormatting.DARK_GRAY)), false);
        src.sendSuccess(() -> Component.literal("Rule " + h.rule().describe() + ", prepare " + h.preparationDays()
                + "d, celebrate " + h.durationDays() + "d, cleanup " + h.cleanupDays() + "d, enabled " + h.enabled()
                + ", seasons " + h.seasons()), false);
        src.sendSuccess(() -> Component.literal("Behaviors " + h.definition().behaviors()), false);
        for (HolidayStatus s : svc.statuses()) {
            if (!s.id().equals(h.id())) continue;
            src.sendSuccess(() -> Component.literal(" " + s.start() + " .. " + s.end() + " (prep from " + s.prepStart() + "): "
                    + s.phase() + (s.skippedReason().isEmpty() ? "" : " [" + s.skippedReason() + "]")), false);
        }
        src.sendSuccess(() -> Component.literal("Scheduling calendar: " + c.system().signature() + ", today epoch day " + c.today()), false);
        return 1;
    }

    private static int gotoHoliday(CommandContext<CommandSourceStack> ctx, boolean prepare) {
        Optional<ScheduledHoliday> oh = find(ctx);
        if (oh.isEmpty()) return 0;
        ScheduledHoliday h = oh.get();
        HolidayService svc = holidays();
        HolidayEngineCore.Context c = svc.context();
        CalendarSystem sys = c.system();
        HolidayWindow target = null;
        for (int y = c.currentYear(); y <= c.currentYear() + 2; y++) {
            HolidayWindow w = h.window(y, sys);
            var rec = svc.records().get(com.holidayseasons.holiday.OccurrenceRecord.key(h.id(), sys.signature(), y));
            boolean used = rec != null && (rec.activations() > 0 || rec.phase() == com.holidayseasons.holiday.HolidayPhase.COMPLETED);
            if (!used && (prepare ? w.prepStart() : w.start()) > c.today()) {
                target = w;
                break;
            }
        }
        if (target == null) {
            ctx.getSource().sendFailure(Component.literal("No upcoming occurrence found"));
            return 0;
        }
        var date = sys.fromEpochDay(prepare ? target.prepStart() : target.start());
        if (!sys.signature().equals(EverFestServer.require().calendar().activeSystem().signature())) {
            ctx.getSource().sendFailure(Component.literal("Holidays follow the season calendar; move seasons with /season set instead"));
            return 0;
        }
        String error = EverFestServer.require().calendar().setDate(date);
        if (error != null) {
            ctx.getSource().sendFailure(Component.literal(error));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("holidayseasons.command.date_set", date.toIsoString()), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        Optional<ScheduledHoliday> oh = find(ctx);
        if (oh.isEmpty()) return 0;
        boolean done = holidays().reset(oh.get().id());
        ctx.getSource().sendSuccess(() -> Component.translatable(done ? "holidayseasons.command.holiday_reset" : "holidayseasons.command.nothing_to_do",
                Component.translatable(HolidayService.statusKey(oh.get().id()))), true);
        return done ? 1 : 0;
    }

    private static int finish(CommandContext<CommandSourceStack> ctx) {
        Optional<ScheduledHoliday> oh = find(ctx);
        if (oh.isEmpty()) return 0;
        boolean done = holidays().finish(oh.get().id());
        ctx.getSource().sendSuccess(() -> Component.translatable(done ? "holidayseasons.command.holiday_finished" : "holidayseasons.command.nothing_to_do",
                Component.translatable(HolidayService.statusKey(oh.get().id()))), true);
        return done ? 1 : 0;
    }
}
