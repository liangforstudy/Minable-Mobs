package io.github.liangforstudy.minemobs.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A profile preset as a button showing its block and tool as item icons (log, or stone + pickaxe),
 * which fits where text labels would be cut off. The full name and time are in the tooltip.
 */
public class ProfileButton extends Button {
    private final BreakProfile profile;

    public ProfileButton(int x, int y, int width, int height, BreakProfile profile, OnPress onPress) {
        // The message is only read out by narration; the icons are drawn instead of it
        super(x, y, width, height, Component.literal(profile.name()), onPress, DEFAULT_NARRATION);
        this.profile = profile;
        setTooltip(Tooltip.create(Component.literal(profile.name() + ": " + TimeText.both(profile.ticks()))));
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        int count = profile.icons().size();
        int x = getX() + (width - (count * 16 + (count - 1))) / 2;
        int y = getY() + (height - 16) / 2;
        for (ItemStack icon : profile.icons()) {
            graphics.renderFakeItem(icon, x, y);
            x += 17;
        }
    }
}
