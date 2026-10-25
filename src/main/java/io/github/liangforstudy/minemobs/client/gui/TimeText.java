package io.github.liangforstudy.minemobs.client.gui;

import io.github.liangforstudy.minemobs.client.MineMobsClientConfig.TimeUnit;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Shows and reads times stored in ticks, as ticks ("4") or seconds ("0.20"). 20 ticks = 1 second. */
public final class TimeText {
    private TimeText() {
    }

    public static String format(int ticks, TimeUnit unit) {
        return unit == TimeUnit.TICKS ? Integer.toString(ticks) : String.format(Locale.ROOT, "%.2f", ticks / 20.0);
    }

    public static String label(TimeUnit unit) {
        return unit == TimeUnit.TICKS ? "ticks" : "sec";
    }

    /** Both units at once, e.g. "60 ticks (3.00 s)", for tooltips and chat. */
    public static String both(int ticks) {
        return ticks + " ticks (" + format(ticks, TimeUnit.SECONDS) + " s)";
    }

    /** @return ticks, or null if the text isn't a number. Seconds round to the nearest tick. */
    @Nullable
    public static Integer parse(String text, TimeUnit unit) {
        String t = text.trim();
        if (t.isEmpty()) return null;
        try {
            if (unit == TimeUnit.TICKS) return Integer.parseInt(t);
            return (int) Math.round(Double.parseDouble(t) * 20.0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * For commands: "4", "4t" or "4ticks" are ticks; "0.2s" or "0.2sec" are seconds.
     *
     * @return ticks, or null if unreadable
     */
    @Nullable
    public static Integer parseWithUnit(String text) {
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.endsWith("ticks")) return parse(t.substring(0, t.length() - 5), TimeUnit.TICKS);
        if (t.endsWith("t")) return parse(t.substring(0, t.length() - 1), TimeUnit.TICKS);
        if (t.endsWith("sec")) return parse(t.substring(0, t.length() - 3), TimeUnit.SECONDS);
        if (t.endsWith("s")) return parse(t.substring(0, t.length() - 1), TimeUnit.SECONDS);
        return parse(t, TimeUnit.TICKS);
    }
}
