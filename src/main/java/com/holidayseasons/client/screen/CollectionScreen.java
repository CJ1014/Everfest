package com.holidayseasons.client.screen;

import com.holidayseasons.client.ClientHolidayState;
import com.holidayseasons.holiday.HolidayStatus;
import com.holidayseasons.reputation.Collectibles;
import com.holidayseasons.reputation.CollectionData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** The Holiday Collection Book: holidays celebrated, collectibles, wildlife, festival achievements and encounters. */
public class CollectionScreen extends Screen {
    private static final int W = 336;
    private static final int H = 210;
    private static final String[] TABS = {"holidays", "collectibles", "wildlife", "achievements", "encounters"};
    private static final List<String> BUILT_IN = List.of("new_years", "valentines", "easter", "independence_day", "halloween",
            "thanksgiving", "christmas");
    private final CollectionData.Book book;
    private int tab;

    public CollectionScreen(CollectionData.Book book) {
        super(Component.translatable("holidayseasons.collection.title"));
        this.book = book;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        int bw = (W - 16) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            addRenderableWidget(Button.builder(Component.translatable("holidayseasons.collection.tab." + TABS[i]), b -> tab = t)
                    .bounds(left() + 8 + i * bw, top() + 24, bw - 2, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left() + W / 2 - 40, top() + H - 26, 80, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int l = left();
        int t = top();
        g.fill(l, t, l + W, t + H, 0xF0F4EBD0);
        g.fill(l, t, l + W, t + 2, 0xFF8E231D);
        g.fill(l, t + H - 2, l + W, t + H, 0xFF8E231D);
        g.renderOutline(l, t, W, H, 0xFF3B2A17);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int l = left();
        int t = top();
        g.drawString(font, title, l + 10, t + 9, 0xFF3B2A17, false);
        Component pct = Component.translatable("holidayseasons.collection.complete", book.completion());
        g.drawString(font, pct, l + W - 10 - font.width(pct), t + 9, 0xFF8E231D, false);
        int y = t + 50;
        switch (tab) {
            case 0 -> renderHolidays(g, l + 12, y);
            case 1 -> renderCollectibles(g, l + 12, y, mouseX, mouseY);
            case 2 -> renderList(g, l + 12, y, Collectibles.WILDLIFE, book.wildlife, "wildlife");
            case 3 -> renderAchievements(g, l + 12, y);
            default -> renderList(g, l + 12, y, Collectibles.ENCOUNTERS, book.encounters, "encounter");
        }
    }

    private void renderHolidays(GuiGraphics g, int x, int y) {
        Map<String, String> known = new LinkedHashMap<>();
        for (String h : BUILT_IN) known.put("holidayseasons:" + h, "holiday.holidayseasons." + h);
        for (HolidayStatus s : ClientHolidayState.statuses()) known.putIfAbsent(s.id(), s.translationKey());
        for (String h : book.celebrated.keySet()) known.putIfAbsent(h, "holiday." + h.replace(':', '.'));
        for (Map.Entry<String, String> e : known.entrySet()) {
            Set<Integer> years = book.celebrated.getOrDefault(e.getKey(), Set.of());
            Component name = Component.translatable(e.getValue());
            Component line = years.isEmpty()
                    ? Component.translatable("holidayseasons.collection.not_yet", name).withStyle(ChatFormatting.GRAY)
                    : Component.translatable("holidayseasons.collection.celebrated", name, years.size(),
                    years.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            g.drawString(font, line, x, y, years.isEmpty() ? 0xFF8C7F68 : 0xFF2F5A1E, false);
            y += 12;
        }
    }

    private void renderCollectibles(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        ItemStack hovered = null;
        boolean hoveredHave = false;
        int row = 0;
        for (Map.Entry<String, List<String>> section : Collectibles.ITEMS.entrySet()) {
            g.drawString(font, Component.translatable("holidayseasons.collection.section." + section.getKey()), x, y + row, 0xFF3B2A17, false);
            int col = 0;
            int lineY = y + row + 10;
            for (String id : section.getValue()) {
                ItemStack stack = stackFor(id);
                int ix = x + col * 20;
                boolean have = book.items.contains(id);
                g.renderItem(stack, ix, lineY);
                if (!have) g.fill(ix, lineY, ix + 16, lineY + 16, 0xC0F4EBD0);
                if (mouseX >= ix && mouseX < ix + 16 && mouseY >= lineY && mouseY < lineY + 16) {
                    hovered = stack;
                    hoveredHave = have;
                }
                col++;
            }
            row += 30;
        }
        if (hovered != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(hoveredHave ? hovered.getHoverName() : Component.translatable("holidayseasons.collection.unknown_item"));
            if (!hoveredHave) tip.add(Component.translatable("holidayseasons.collection.not_found").withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    private static ItemStack stackFor(String id) {
        if (id.equals(Collectibles.PEDDLERS_LANTERN)) {
            ItemStack s = new ItemStack(Items.SOUL_LANTERN);
            s.set(DataComponents.CUSTOM_NAME, Component.translatable("item.holidayseasons.peddlers_lantern"));
            return s;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private void renderList(GuiGraphics g, int x, int y, List<String> entries, Set<String> have, String kind) {
        for (String e : entries) {
            boolean ok = have.contains(e);
            Component line = ok
                    ? Component.literal("✔ ").append(Component.translatable("holidayseasons.collection." + kind + "." + e))
                    : Component.literal("? ").append(Component.translatable("holidayseasons.collection.undiscovered"));
            g.drawString(font, line, x, y, ok ? 0xFF2F5A1E : 0xFF8C7F68, false);
            y += 12;
        }
    }

    private void renderAchievements(GuiGraphics g, int x, int y) {
        for (Map.Entry<String, Integer> e : Collectibles.ACHIEVEMENTS.entrySet()) {
            int have = book.stat(e.getKey());
            boolean done = have >= e.getValue();
            Component line = Component.literal(done ? "✔ " : "  ").append(Component.translatable("holidayseasons.collection.achievement." + e.getKey(),
                    Math.min(have, e.getValue()), e.getValue()));
            g.drawString(font, line, x, y, done ? 0xFF2F5A1E : 0xFF3B2A17, false);
            y += 12;
        }
        y += 6;
        g.drawString(font, Component.translatable("holidayseasons.collection.totals", book.stat("eggs_found"), book.stat("candy_given"),
                book.stat("presents_given"), book.stat("guests_welcomed")), x, y, 0xFF5A4A32, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
