package com.holidayseasons.client.screen;

import com.holidayseasons.home.HomeRecord;
import com.holidayseasons.network.OpenHearthPayload;
import com.holidayseasons.network.UpdateHearthPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Owner settings for a registered home. All changes are validated by the server. */
public class HearthScreen extends Screen {
    private static final int W = 260;
    private static final int H = 192;
    private final OpenHearthPayload data;
    private EditBox name;
    private boolean acceptVisitors;
    private boolean decorations;
    private HomeRecord.CandyPolicy policy;

    public HearthScreen(OpenHearthPayload data) {
        super(Component.translatable("holidayseasons.screen.hearth"));
        this.data = data;
        this.acceptVisitors = data.acceptVisitors();
        this.decorations = data.allowVillagerDecorations();
        this.policy = data.policy();
    }

    @Override
    protected void init() {
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        name = new EditBox(font, left + 12, top + 34, W - 24, 18, Component.translatable("holidayseasons.screen.hearth.name"));
        name.setMaxLength(40);
        name.setValue(data.name());
        addRenderableWidget(name);
        addRenderableWidget(CycleButton.onOffBuilder(acceptVisitors)
                .create(left + 12, top + 60, W - 24, 20, Component.translatable("holidayseasons.screen.hearth.visitors"), (b, v) -> acceptVisitors = v));
        addRenderableWidget(CycleButton.onOffBuilder(decorations)
                .create(left + 12, top + 84, W - 24, 20, Component.translatable("holidayseasons.screen.hearth.decorations"), (b, v) -> decorations = v));
        addRenderableWidget(CycleButton.<HomeRecord.CandyPolicy>builder(p -> Component.translatable("holidayseasons.candy_policy." + p.name().toLowerCase()))
                .withValues(HomeRecord.CandyPolicy.values()).withInitialValue(policy)
                .create(left + 12, top + 108, W - 24, 20, Component.translatable("holidayseasons.screen.hearth.candy"), (b, v) -> policy = v));
        addRenderableWidget(Button.builder(Component.translatable("holidayseasons.screen.save"), b -> save()).bounds(left + 12, top + H - 28, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(left + W - 122, top + H - 28, 110, 20).build());
    }

    private void save() {
        PacketDistributor.sendToServer(new UpdateHearthPayload(data.homeId(), name.getValue(), acceptVisitors, decorations, policy));
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF5A2E12);
        g.fillGradient(left, top, left + W, top + H, 0xFF2B1A12, 0xFF3D2416);
        g.fill(left, top, left + W, top + 24, 0xFFC2621F);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.drawCenteredString(font, Component.translatable("holidayseasons.screen.hearth").withStyle(ChatFormatting.BOLD), width / 2, top + 8, 0xFFFFF4E0);
        g.drawString(font, Component.translatable("holidayseasons.screen.hearth.name"), left + 12, top + 26 - 1, 0xFFE8C9A0, false);
        boolean door = !data.entrance().equals(data.hearth());
        Component entrance = door
                ? Component.translatable("holidayseasons.screen.hearth.entrance", data.entrance().toShortString())
                : Component.translatable("holidayseasons.screen.hearth.no_door");
        g.drawString(font, entrance, left + 12, top + 132, door ? 0xFFB8E0A0 : 0xFFE0B080, false);
        g.drawString(font, Component.translatable("holidayseasons.screen.hearth.visits", data.visits()), left + 12, top + 142, 0xFFE8C9A0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
