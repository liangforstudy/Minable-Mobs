package io.github.liangforstudy.minemobs.client.gui;

import io.github.liangforstudy.minemobs.client.MineMobsClientConfig;
import io.github.liangforstudy.minemobs.client.MineMobsClientConfig.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * "‹ ticks ›" next to a time box. Click either arrow, or hover and scroll, to switch every time box
 * between ticks and seconds (like switching a ruler between pixels and centimetres).
 */
public class UnitSwitcher extends AbstractWidget {
    private final Runnable onSwitch;

    public UnitSwitcher(int x, int y, int width, int height, Runnable onSwitch) {
        super(x, y, width, height, Component.empty());
        this.onSwitch = onSwitch;
        setTooltip(Tooltip.create(Component.literal("Switch between ticks and seconds (click the arrows or scroll). 20 ticks = 1 second.")));
        updateMessage();
    }

    private void updateMessage() {
        setMessage(Component.literal("Unit: " + TimeText.label(MineMobsClientConfig.get().timeUnit)));
    }

    private void toggle() {
        MineMobsClientConfig config = MineMobsClientConfig.get();
        config.timeUnit = config.timeUnit == TimeUnit.TICKS ? TimeUnit.SECONDS : TimeUnit.TICKS;
        MineMobsClientConfig.save();
        updateMessage();
        onSwitch.run();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        toggle();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!active || !isMouseOver(mouseX, mouseY) || scrollY == 0) return false;
        toggle();
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        boolean hover = isHoveredOrFocused();
        graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0x60FFFFFF : 0x30FFFFFF);
        int color = active ? 0xFFFFFF : 0xA0A0A0;
        int mid = getY() + (height - 8) / 2;
        graphics.drawString(font, "‹", getX() + 4, mid, hover ? 0xFFE08A : color);
        graphics.drawString(font, "›", getX() + width - 4 - font.width("›"), mid, hover ? 0xFFE08A : color);
        String unit = TimeText.label(MineMobsClientConfig.get().timeUnit);
        graphics.drawCenteredString(font, unit, getX() + width / 2, mid, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
