package com.holidayseasons.client.screen;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarMode;
import com.holidayseasons.calendar.CalendarSnapshot;
import com.holidayseasons.calendar.CalendarSystem;
import com.holidayseasons.calendar.CalendarSystems;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.client.CalendarText;
import com.holidayseasons.client.ClientCalendarState;
import com.holidayseasons.client.ClientHolidayState;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A paper wall calendar: month grid with today highlighted, holiday markers with tooltips, month navigation
 * and an upcoming-holiday countdown sidebar. Purely a view of server-sent state.
 */
public class CalendarScreen extends Screen {
    private static final int PANEL_W = 336;
    private static final int PANEL_H = 214;
    private static final int CELL_W = 26;
    private static final int CELL_H = 22;
    private static final int MAX_ROWS = 6;

    private static final int PAPER = 0xFFF4EBD0;
    private static final int PAPER_DARK = 0xFFE6D8B3;
    private static final int INK = 0xFF3B2A17;
    private static final int INK_SOFT = 0xFF7A6446;
    private static final int BORDER = 0xFF6B4F2A;
    private static final int TODAY = 0xFFC0392B;

    private int left;
    private int top;
    private int viewYear;
    private int viewMonth;
    private int rowScroll;
    @Nullable
    private CalendarSystem system;

    public CalendarScreen() {
        super(Component.translatable("holidayseasons.screen.calendar"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        CalendarSnapshot s = ClientCalendarState.snapshot();
        if (s != null && system == null) {
            system = CalendarSystems.fromSignature(s.systemSignature(), 2000);
            viewYear = s.date().year();
            viewMonth = s.date().month();
        }
        int navY = top + 8;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> shiftMonth(-1)).bounds(left + 8, navY, 18, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> shiftMonth(1)).bounds(left + 8 + 7 * CELL_W - 18, navY, 18, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("holidayseasons.screen.today"), b -> goToday())
                .bounds(left + PANEL_W - 64, top + PANEL_H - 24, 56, 16).build());
    }

    private void shiftMonth(int delta) {
        int m = viewMonth + delta;
        int y = viewYear;
        while (m < 1) { m += 12; y--; }
        while (m > 12) { m -= 12; y++; }
        if (y < 1) return;
        viewMonth = m;
        viewYear = y;
        rowScroll = 0;
    }

    private void goToday() {
        CalendarSnapshot s = ClientCalendarState.snapshot();
        if (s == null) return;
        viewYear = s.date().year();
        viewMonth = s.date().month();
        rowScroll = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && totalRows() > MAX_ROWS) {
            rowScroll = Math.max(0, Math.min(totalRows() - MAX_ROWS, rowScroll - (int) Math.signum(scrollY)));
            return true;
        }
        if (scrollY != 0) {
            shiftMonth(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int firstWeekdayOffset() {
        if (system == null) return 0;
        long epoch = system.toEpochDay(new CalendarDate(viewYear, viewMonth, 1));
        return system.dayOfWeek(epoch) - 1;
    }

    private int totalRows() {
        if (system == null) return 0;
        int cells = firstWeekdayOffset() + system.monthLength(viewYear, viewMonth);
        return (cells + 6) / 7;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        CalendarSnapshot s = ClientCalendarState.snapshot();
        if (s != null && system != null) drawPanel(g, s);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Background (panel) first, then widgets, then the grid and sidebar which never overlap the buttons.
        super.render(g, mouseX, mouseY, partialTick);
        CalendarSnapshot s = ClientCalendarState.snapshot();
        if (s == null || system == null) {
            g.drawCenteredString(font, Component.translatable("holidayseasons.screen.waiting"), width / 2, height / 2, 0xFFFFFFFF);
            return;
        }
        HolidayStatus hovered = drawGrid(g, s, mouseX, mouseY);
        drawSidebar(g, s);
        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(hovered.translationKey()).withStyle(ChatFormatting.BOLD));
            lines.add(Component.translatable("holidayseasons.screen.holiday_range",
                    CalendarText.fullDate(hovered.start(), s.mode()), CalendarText.fullDate(hovered.end(), s.mode())));
            lines.add(Component.translatable("holidayseasons.phase." + hovered.phase().name().toLowerCase()).withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void drawPanel(GuiGraphics g, CalendarSnapshot s) {
        // Drop shadow, border, paper.
        g.fill(left + 3, top + 3, left + PANEL_W + 3, top + PANEL_H + 3, 0x66000000);
        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, BORDER);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, PAPER);
        // Binding rings along the top edge.
        for (int i = 0; i < 6; i++) {
            int rx = left + 24 + i * ((PANEL_W - 48) / 5);
            g.fill(rx - 2, top - 6, rx + 2, top + 4, 0xFF8C8C8C);
            g.fill(rx - 1, top - 5, rx + 1, top + 3, 0xFFCFCFCF);
        }
        int bandColor = seasonColor(s.season());
        g.fill(left + 4, top + 4, left + 8 + 7 * CELL_W + 4, top + 28, bandColor);
        Component header = CalendarText.monthName(viewMonth).copy().append(" ").append(
                s.mode() == CalendarMode.REAL_WORLD ? Component.literal(Integer.toString(viewYear))
                        : Component.translatable("holidayseasons.date.year", viewYear));
        int gridCenter = left + 8 + 7 * CELL_W / 2;
        g.drawCenteredString(font, header, gridCenter, top + 12, 0xFFFFFFFF);
    }

    @Nullable
    private HolidayStatus drawGrid(GuiGraphics g, CalendarSnapshot s, int mouseX, int mouseY) {
        int gx = left + 8;
        int gy = top + 32;
        for (int d = 1; d <= 7; d++) {
            Component wd = CalendarText.weekday(d);
            int cx = gx + (d - 1) * CELL_W + CELL_W / 2;
            g.drawString(font, wd, cx - font.width(wd) / 2, gy, d >= 6 ? 0xFFA0522D : INK_SOFT, false);
        }
        gy += 11;
        int offset = firstWeekdayOffset();
        int len = system.monthLength(viewYear, viewMonth);
        HolidayStatus hovered = null;
        List<HolidayStatus> holidays = ClientHolidayState.statuses();
        for (int day = 1; day <= len; day++) {
            int cell = offset + day - 1;
            int row = cell / 7 - rowScroll;
            int col = cell % 7;
            if (row < 0 || row >= MAX_ROWS) continue;
            int x = gx + col * CELL_W;
            int y = gy + row * CELL_H;
            CalendarDate date = new CalendarDate(viewYear, viewMonth, day);
            boolean today = date.equals(s.date());
            HolidayStatus holiday = null;
            boolean preparing = false;
            for (HolidayStatus h : holidays) {
                if (!h.skippedReason().isEmpty()) continue;
                if (h.covers(date)) { holiday = h; break; }
                if (h.preparingOn(date)) { holiday = h; preparing = true; }
            }
            int bg = (col >= 5) ? PAPER_DARK : 0xFFFBF5E4;
            g.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, bg);
            if (holiday != null && !preparing) {
                g.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, (holiday.color() & 0x00FFFFFF) | 0x66000000);
            } else if (holiday != null) {
                g.fill(x + 1, y + CELL_H - 4, x + CELL_W - 1, y + CELL_H - 1, (holiday.color() & 0x00FFFFFF) | 0x99000000);
            }
            if (today) g.renderOutline(x, y, CELL_W, CELL_H, TODAY);
            String label = Integer.toString(day);
            g.drawString(font, label, x + 3, y + 3, today ? TODAY : INK, false);
            if (holiday != null && !preparing) {
                g.fill(x + CELL_W - 7, y + 3, x + CELL_W - 3, y + 7, holiday.color() | 0xFF000000);
            }
            if (holiday != null && mouseX >= x && mouseX < x + CELL_W && mouseY >= y && mouseY < y + CELL_H) {
                hovered = holiday;
            }
        }
        if (totalRows() > MAX_ROWS) {
            Component more = Component.translatable("holidayseasons.screen.scroll_rows", rowScroll + 1, totalRows() - MAX_ROWS + 1);
            g.drawString(font, more, gx, gy + MAX_ROWS * CELL_H + 1, INK_SOFT, false);
        }
        return hovered;
    }

    private void drawSidebar(GuiGraphics g, CalendarSnapshot s) {
        int sx = left + 8 + 7 * CELL_W + 12;
        int sw = left + PANEL_W - 8 - sx;
        int y = top + 8;
        g.fill(sx - 6, top + 6, sx - 5, top + PANEL_H - 6, 0x40000000);
        g.drawString(font, Component.translatable("holidayseasons.screen.today_is").withStyle(ChatFormatting.BOLD), sx, y, INK, false);
        y += 12;
        g.drawString(font, CalendarText.weekday(s.dayOfWeek()).copy().append(", ").append(CalendarText.fullDate(s.date(), s.mode())), sx, y, INK, false);
        y += 10;
        g.drawString(font, CalendarText.time(ClientCalendarState.liveTimeOfDay()), sx, y, INK_SOFT, false);
        y += 10;
        Component season = Component.translatable("holidayseasons.subseason." + s.subSeason().id());
        if (s.seasonsFromSereneSeasons()) season = season.copy().append(Component.literal(" ❀").withStyle(ChatFormatting.DARK_GREEN));
        g.drawString(font, season, sx, y, seasonColor(s.season()), false);
        y += 16;

        g.drawString(font, Component.translatable("holidayseasons.screen.upcoming").withStyle(ChatFormatting.BOLD), sx, y, INK, false);
        y += 12;
        List<HolidayStatus> list = ClientHolidayState.statuses();
        if (list.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("holidayseasons.screen.no_holidays"), sx, y, sw, INK_SOFT);
            return;
        }
        int shown = 0;
        for (HolidayStatus h : list) {
            if (shown >= 8 || y > top + PANEL_H - 40) break;
            if (h.phase() == HolidayPhase.COMPLETED) continue;
            g.fill(sx, y + 1, sx + 5, y + 6, h.color() | 0xFF000000);
            g.drawString(font, Component.translatable(h.translationKey()), sx + 8, y, INK, false);
            y += 9;
            g.drawString(font, countdown(h), sx + 8, y, INK_SOFT, false);
            y += 12;
            shown++;
        }
    }

    private static Component countdown(HolidayStatus h) {
        if (!h.skippedReason().isEmpty()) return Component.translatable("holidayseasons.screen.skipped");
        return switch (h.phase()) {
            case ACTIVE -> Component.translatable("holidayseasons.screen.happening_now").withStyle(ChatFormatting.DARK_GREEN);
            case PREPARING -> Component.translatable("holidayseasons.screen.preparing_in", h.daysUntilStart()).withStyle(ChatFormatting.GOLD);
            case CLEANUP -> Component.translatable("holidayseasons.screen.cleaning_up");
            default -> h.daysUntilStart() == 1
                    ? Component.translatable("holidayseasons.screen.tomorrow")
                    : Component.translatable("holidayseasons.screen.in_days", h.daysUntilStart());
        };
    }

    private static int seasonColor(Season season) {
        return switch (season) {
            case SPRING -> 0xFF5E9E4E;
            case SUMMER -> 0xFFC9A227;
            case AUTUMN -> 0xFFC2621F;
            case WINTER -> 0xFF4F7FAF;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
