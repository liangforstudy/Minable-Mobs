package io.github.liangforstudy.minemobs.client.gui;

import io.github.liangforstudy.minemobs.client.MineMobsClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * A time box: type a number in the current unit (ticks or seconds), or hover and scroll to step it by
 * one tick. The value is always kept in ticks, so switching units never changes it.
 */
public class TimeField extends EditBox {
    private static final int VALID = 0xE0E0E0;
    private static final int INVALID = 0xFF6B6B;

    private final int min;
    private final int max;
    private final IntConsumer onChange;
    private int ticks;
    private boolean editable = true;
    private boolean updating;

    public TimeField(Font font, int x, int y, int width, int height, Component label, int ticks, int min, int max, IntConsumer onChange) {
        super(font, x, y, width, height, label);
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        this.ticks = ticks;
        setMaxLength(8);
        setFilter(text -> text.matches("[0-9.]*"));
        setResponder(this::typed);
        refresh();
    }

    public int ticks() {
        return ticks;
    }

    /** Set from outside (a profile button, a reset); calls the change listener. */
    public void setTicks(int value) {
        int clamped = Math.max(min, Math.min(max, value));
        boolean changed = clamped != ticks;
        ticks = clamped;
        refresh();
        if (changed) onChange.accept(ticks);
    }

    /** Re-show the value, e.g. after the unit changed. */
    public void refresh() {
        updating = true;
        setValue(TimeText.format(ticks, unit()));
        setTextColor(VALID);
        updating = false;
    }

    private void typed(String text) {
        if (updating) return;
        Integer parsed = TimeText.parse(text, unit());
        if (parsed == null || parsed < min || parsed > max) {
            setTextColor(INVALID);
            return;
        }
        setTextColor(VALID);
        if (parsed != ticks) {
            ticks = parsed;
            onChange.accept(ticks);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!editable || !active || !isMouseOver(mouseX, mouseY) || scrollY == 0) return false;
        setTicks(ticks + (scrollY > 0 ? 1 : -1));
        return true;
    }

    @Override
    public void setEditable(boolean enabled) {
        super.setEditable(enabled);
        editable = enabled;
    }

    private static MineMobsClientConfig.TimeUnit unit() {
        return MineMobsClientConfig.get().timeUnit;
    }
}
