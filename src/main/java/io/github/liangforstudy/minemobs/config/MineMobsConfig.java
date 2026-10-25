package io.github.liangforstudy.minemobs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.liangforstudy.minemobs.MineMobs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Server-side gameplay settings, stored in config/minemobs.json and sent to every client (the
 * settings screen shows them; ops can change them from there). Times are in ticks (20 = 1 second).
 */
public final class MineMobsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("minemobs.json");
    /** Longest break time the settings accept: one hour. */
    public static final int MAX_TICKS = 72000;

    private static MineMobsConfig instance = new MineMobsConfig();

    /** Master switch, flipped by /minemobs. */
    public boolean enabled = true;
    /** Mobs without their own entry: wood punched with a bare fist. */
    public int defaultBreakTicks = 60;
    /** ...and with a netherite pickaxe: stone with a netherite pickaxe. */
    public int defaultNetheriteTicks = 5;
    /** How much of a player's tap-or-hold wait may count toward mining (anti-cheat cap). */
    public int maxHoldCreditTicks = 20;
    /** Per-mob times, e.g. "minecraft:pig". A missing value uses the default. */
    public Map<String, MobTimes> mobs = new TreeMap<>();

    public static final class MobTimes {
        @Nullable public Integer breakTicks;
        @Nullable public Integer netheriteTicks;

        public boolean isEmpty() {
            return breakTicks == null && netheriteTicks == null;
        }
    }

    public static MineMobsConfig get() {
        return instance;
    }

    public int breakTicks(EntityType<?> type) {
        MobTimes times = mobs.get(EntityType.getKey(type).toString());
        return times != null && times.breakTicks != null ? times.breakTicks : defaultBreakTicks;
    }

    public int netheriteTicks(EntityType<?> type) {
        MobTimes times = mobs.get(EntityType.getKey(type).toString());
        return times != null && times.netheriteTicks != null ? times.netheriteTicks : defaultNetheriteTicks;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    /** Parses and clamps settings, e.g. from the settings screen. Throws on malformed JSON. */
    public static MineMobsConfig fromJson(String json) {
        MineMobsConfig config = GSON.fromJson(json, MineMobsConfig.class);
        if (config == null) throw new JsonParseException("empty settings");
        config.sanitize();
        return config;
    }

    private void sanitize() {
        defaultBreakTicks = clampTicks(defaultBreakTicks);
        defaultNetheriteTicks = clampTicks(defaultNetheriteTicks);
        maxHoldCreditTicks = Math.max(0, Math.min(maxHoldCreditTicks, 200));
        Map<String, MobTimes> clean = new TreeMap<>();
        if (mobs != null) {
            mobs.forEach((id, times) -> {
                if (times == null) return;
                if (times.breakTicks != null) times.breakTicks = clampTicks(times.breakTicks);
                if (times.netheriteTicks != null) times.netheriteTicks = clampTicks(times.netheriteTicks);
                if (!times.isEmpty()) clean.put(id, times);
            });
        }
        mobs = clean;
    }

    private static int clampTicks(int ticks) {
        return Math.max(0, Math.min(ticks, MAX_TICKS));
    }

    public static void set(MineMobsConfig config) {
        instance = config;
        save();
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                MineMobsConfig loaded = GSON.fromJson(reader, MineMobsConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    instance = loaded;
                }
            } catch (IOException | JsonParseException e) {
                MineMobs.LOGGER.error("Couldn't read {}, using defaults", PATH, e);
            }
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            MineMobs.LOGGER.error("Couldn't write {}", PATH, e);
        }
    }
}
