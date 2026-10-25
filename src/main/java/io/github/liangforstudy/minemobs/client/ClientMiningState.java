package io.github.liangforstudy.minemobs.client;

import io.github.liangforstudy.minemobs.config.MineMobsConfig;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.world.entity.Crackiness;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** What the server told us: which entities are being mined, and how far along. */
public final class ClientMiningState {
    private static final Int2IntMap PERCENT = new Int2IntOpenHashMap();
    private static final Int2FloatMap HELD_BOB = new Int2FloatOpenHashMap();
    @Nullable private static MineMobsConfig serverSettings;
    private static boolean canEdit;

    private ClientMiningState() {
    }

    public static void setProgress(int entityId, int percent) {
        if (percent < 0) {
            PERCENT.remove(entityId);
            HELD_BOB.remove(entityId);
        } else {
            PERCENT.put(entityId, percent);
        }
    }

    public static boolean isBeingMined(int entityId) {
        return !PERCENT.isEmpty() && PERCENT.containsKey(entityId);
    }

    /** destroy_stage_0-9, or -1 if nobody is mining it. */
    public static int getStage(int entityId) {
        if (!isBeingMined(entityId)) return -1;
        return Math.min(9, PERCENT.get(entityId) / 10);
    }

    /** Iron golem cracks by mining progress: 0-25% none, 25-50% low, 50-75% medium, 75-100% high. */
    public static Crackiness.Level getCrackiness(int entityId) {
        if (!isBeingMined(entityId)) return Crackiness.Level.NONE;
        int percent = PERCENT.get(entityId);
        if (percent >= 75) return Crackiness.Level.HIGH;
        if (percent >= 50) return Crackiness.Level.MEDIUM;
        if (percent >= 25) return Crackiness.Level.LOW;
        return Crackiness.Level.NONE;
    }

    /** The model's age-in-ticks from the first frame it was frozen, so animations hold still. */
    public static float heldBob(int entityId, Supplier<Float> current) {
        if (!HELD_BOB.containsKey(entityId)) HELD_BOB.put(entityId, (float) current.get());
        return HELD_BOB.get(entityId);
    }

    public static boolean isServerEnabled() {
        return serverSettings != null && serverSettings.enabled;
    }

    /** The server's settings as last sent, or null if the server doesn't have the mod. */
    @Nullable
    public static MineMobsConfig serverSettings() {
        return serverSettings;
    }

    public static boolean canEditServerSettings() {
        return canEdit;
    }

    public static void setServerSettings(MineMobsConfig settings, boolean mayEdit) {
        serverSettings = settings;
        canEdit = mayEdit;
    }

    public static void reset() {
        PERCENT.clear();
        HELD_BOB.clear();
        serverSettings = null;
        canEdit = false;
    }
}
