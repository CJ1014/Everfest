package com.holidayseasons.client.screen;

import com.holidayseasons.network.WrapPresentPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Wrapping a present: who it is for, a message, and the paper colour. The server validates everything. */
public class WrapPresentScreen extends Screen {
    public static final int[] PAPER = {0xC0392B, 0x2E8B57, 0x2E6EC0, 0xD4AF37, 0x8E44AD, 0xE8E8E8, 0xE67E22, 0xE84A7F};
    private static final int W = 240;
    private static final int H = 168;
    private final BlockPos pos;
    private int color;
    private EditBox to;
    private EditBox message;

    public WrapPresentScreen(BlockPos pos, int color) {
        super(Component.translatable("holidayseasons.screen.wrap"));
        this.pos = pos;
        this.color = color;
    }

    @Override
    protected void init() {
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        to = addRenderableWidget(new EditBox(font, left + 12, top + 38, W - 24, 18, Component.translatable("holidayseasons.screen.wrap.to")));
        to.setMaxLength(32);
        to.setHint(Component.translatable("holidayseasons.screen.wrap.to_hint").withStyle(ChatFormatting.DARK_GRAY));
        message = addRenderableWidget(new EditBox(font, left + 12, top + 74, W - 24, 18, Component.translatable("holidayseasons.screen.wrap.message")));
        message.setMaxLength(120);
        for (int i = 0; i < PAPER.length; i++) {
            int c = PAPER[i];
            addRenderableWidget(Button.builder(Component.empty(), b -> color = c).bounds(left + 12 + i * 27, top + 104, 24, 16).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("holidayseasons.screen.wrap.button"), b -> {
            PacketDistributor.sendToServer(new WrapPresentPayload(pos, to.getValue(), message.getValue(), color));
            onClose();
        }).bounds(left + 12, top + H - 28, 104, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(left + W - 116, top + H - 28, 104, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF7A1F1F);
        g.fillGradient(left, top, left + W, top + H, 0xFFF6EEDC, 0xFFE9DCC0);
        g.fill(left, top, left + W, top + 22, 0xFF1F6B3A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.drawCenteredString(font, Component.translatable("holidayseasons.screen.wrap").withStyle(ChatFormatting.BOLD), width / 2, top + 7, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("holidayseasons.screen.wrap.to"), left + 12, top + 28, 0xFF4A3420, false);
        g.drawString(font, Component.translatable("holidayseasons.screen.wrap.message"), left + 12, top + 64, 0xFF4A3420, false);
        g.drawString(font, Component.translatable("holidayseasons.screen.wrap.paper"), left + 12, top + 96 - 2, 0xFF4A3420, false);
        for (int i = 0; i < PAPER.length; i++) {
            int x = left + 14 + i * 27;
            int y = top + 106;
            g.fill(x, y, x + 20, y + 12, 0xFF000000 | PAPER[i]);
            if (PAPER[i] == color) g.renderOutline(x - 2, y - 2, 24, 16, 0xFFFFD700);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
