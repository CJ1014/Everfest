package com.holidayseasons.client.screen;

import com.holidayseasons.config.AdminConfig;
import com.holidayseasons.network.SetConfigPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operator screen for EverFest's server settings on a remote server ({@code /everfest admin config}). Edits are
 * checked here for quick feedback and again on the server, which applies them all or none.
 */
public class AdminConfigScreen extends Screen {
    private final List<AdminConfig.Entry> entries;
    private final Map<String, String> edited = new LinkedHashMap<>();
    private final Map<String, Boolean> valid = new LinkedHashMap<>();
    private SettingsList list;
    private Component status = Component.empty();

    public AdminConfigScreen(List<AdminConfig.Entry> entries) {
        super(Component.translatable("holidayseasons.admin.title"));
        this.entries = entries;
    }

    @Override
    protected void init() {
        list = new SettingsList(minecraft, width, height - 64, 32, 24);
        String section = "";
        for (AdminConfig.Entry e : entries) {
            String s = e.path().contains(".") ? e.path().substring(0, e.path().indexOf('.')) : "general";
            if (!s.equals(section)) {
                section = s;
                list.addRow(new Header(sectionLabel(s)));
            }
            list.addRow(new Setting(e, widgetFor(e)));
        }
        addRenderableWidget(list);
        addRenderableWidget(Button.builder(Component.translatable("holidayseasons.screen.save"), b -> save()).bounds(width / 2 - 155, height - 26, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width / 2 + 5, height - 26, 150, 20).build());
    }

    private static Component sectionLabel(String s) {
        String key = "holidayseasons.configuration." + s;
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(s);
    }

    private static Component label(AdminConfig.Entry e) {
        if (!e.translationKey().isEmpty() && I18n.exists(e.translationKey())) return Component.translatable(e.translationKey());
        return Component.literal(e.path().substring(e.path().lastIndexOf('.') + 1));
    }

    private AbstractWidget widgetFor(AdminConfig.Entry e) {
        Tooltip tip = Tooltip.create(Component.literal(e.comment()
                + (e.min().isEmpty() ? "" : "\n" + I18n.get("holidayseasons.admin.range", e.min(), e.max()))));
        AbstractWidget w;
        switch (e.type()) {
            case BOOL -> w = CycleButton.onOffBuilder(Boolean.parseBoolean(e.value()))
                    .displayOnlyValue().create(0, 0, 140, 20, Component.empty(), (b, v) -> change(e, String.valueOf(v), true));
            case ENUM -> w = CycleButton.<String>builder(Component::literal).withValues(e.options()).withInitialValue(e.value())
                    .displayOnlyValue().create(0, 0, 140, 20, Component.empty(), (b, v) -> change(e, v, true));
            default -> {
                EditBox box = new EditBox(font, 0, 0, 140, 18, label(e));
                box.setMaxLength(4096);
                box.setValue(e.value());
                box.setResponder(text -> {
                    boolean ok = check(e, text);
                    box.setTextColor(ok ? 0xE0E0E0 : 0xFF5555);
                    change(e, text, ok);
                });
                w = box;
            }
        }
        w.setTooltip(tip);
        return w;
    }

    /** Client-side check for quick feedback; the server checks again. */
    static boolean check(AdminConfig.Entry e, String text) {
        String s = text.strip();
        try {
            switch (e.type()) {
                case INT, LONG -> {
                    long v = Long.parseLong(s);
                    return e.min().isEmpty() || (v >= Long.parseLong(e.min()) && v <= Long.parseLong(e.max()));
                }
                case DOUBLE -> {
                    double v = Double.parseDouble(s);
                    return e.min().isEmpty() || (v >= Double.parseDouble(e.min()) && v <= Double.parseDouble(e.max()));
                }
                case INT_LIST -> {
                    if (!s.isEmpty()) for (String part : s.split(",")) Integer.parseInt(part.strip());
                    return true;
                }
                default -> {
                    return true;
                }
            }
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void change(AdminConfig.Entry e, String value, boolean ok) {
        if (value.equals(e.value())) {
            edited.remove(e.path());
            valid.remove(e.path());
        } else {
            edited.put(e.path(), value);
            valid.put(e.path(), ok);
        }
        status = Component.empty();
    }

    private void save() {
        if (valid.containsValue(false)) {
            status = Component.translatable("holidayseasons.admin.fix_first").withStyle(ChatFormatting.RED);
            return;
        }
        if (!edited.isEmpty()) PacketDistributor.sendToServer(new SetConfigPayload(new LinkedHashMap<>(edited)));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        Component sub = status.getString().isEmpty()
                ? Component.translatable("holidayseasons.admin.changes", edited.size()).withStyle(ChatFormatting.GRAY) : status;
        g.drawCenteredString(font, sub, width / 2, 21, 0xFFFFFF);
    }

    // ------------------------------------------------------------------ list

    abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
    }

    final class Header extends Row {
        private final Component text;

        Header(Component text) {
            this.text = text.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int w, int h, int mouseX, int mouseY, boolean hovered, float partialTick) {
            g.drawString(font, text, left + 4, top + 8, 0xFFFFFF);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    final class Setting extends Row {
        private final Component label;
        private final AbstractWidget widget;

        Setting(AdminConfig.Entry e, AbstractWidget widget) {
            this.label = label(e);
            this.widget = widget;
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int w, int h, int mouseX, int mouseY, boolean hovered, float partialTick) {
            g.drawString(font, label, left + 12, top + 6, 0xE0E0E0);
            widget.setX(left + w - 150);
            widget.setY(top + 1);
            widget.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(widget);
        }
    }

    final class SettingsList extends ContainerObjectSelectionList<Row> {
        SettingsList(Minecraft mc, int w, int h, int y, int itemHeight) {
            super(mc, w, h, y, itemHeight);
        }

        void addRow(Row r) {
            addEntry(r);
        }

        @Override
        public int getRowWidth() {
            return Math.min(420, width - 40);
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + getRowWidth() / 2 + 6;
        }
    }

    static List<String> paths(List<AdminConfig.Entry> entries) {
        List<String> out = new ArrayList<>();
        entries.forEach(e -> out.add(e.path()));
        return out;
    }
}
