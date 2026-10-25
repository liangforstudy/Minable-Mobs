package io.github.liangforstudy.minemobs.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.liangforstudy.minemobs.MineMobs;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** This player's own preferences, stored in config/minemobs-client.json. */
public final class MineMobsClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("minemobs-client.json");
    public static final int MAX_HOLD_DELAY = 20;

    private static MineMobsClientConfig instance = new MineMobsClientConfig();

    /** Ticks to wait before a press on a mob counts as mining instead of a hit. 0 = mine immediately. */
    public int holdDelayTicks = 4;
    /** How time fields are shown: ticks or seconds. */
    public TimeUnit timeUnit = TimeUnit.TICKS;

    public enum TimeUnit { TICKS, SECONDS }

    public static MineMobsClientConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                MineMobsClientConfig loaded = GSON.fromJson(reader, MineMobsClientConfig.class);
                if (loaded != null) {
                    loaded.holdDelayTicks = Math.max(0, Math.min(loaded.holdDelayTicks, MAX_HOLD_DELAY));
                    if (loaded.timeUnit == null) loaded.timeUnit = TimeUnit.TICKS;
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
