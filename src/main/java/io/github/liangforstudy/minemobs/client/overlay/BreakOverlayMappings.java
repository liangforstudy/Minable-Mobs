package io.github.liangforstudy.minemobs.client.overlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.liangforstudy.minemobs.MineMobs;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads break_overlay files on every resource reload (F3+T):
 * <ol>
 *   <li>{@code assets/<any>/break_overlay/<namespace>/<mob>.json} from the mod and resource packs</li>
 *   <li>{@code config/minemobs/break_overlay/<namespace>/<mob>.json}, which wins — drop files from the
 *       web app here to try them without rebuilding</li>
 * </ol>
 */
public final class BreakOverlayMappings implements SimpleSynchronousResourceReloadListener {
    private static final String FOLDER = "break_overlay";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MineMobs.MOD_ID).resolve(FOLDER);

    private static Map<ResourceLocation, BreakOverlayMapping> mappings = Map.of();
    /** Set by /minemobs overlay fit|projection: ignore every file and show that mode unadjusted. */
    @Nullable private static BreakOverlayMapping override;

    public static BreakOverlayMapping get(EntityType<?> type) {
        if (override != null) return override;
        return mappings.getOrDefault(EntityType.getKey(type), BreakOverlayMapping.DEFAULT);
    }

    /** @param mode null to go back to the files */
    public static void setOverride(@Nullable BreakOverlayMapping.Mode mode) {
        override = mode == null ? null : new BreakOverlayMapping(mode, FaceAdjust.IDENTITY, Map.of());
    }

    /** The mob's current mapping from its files (ignores the /minemobs overlay preview). */
    public static BreakOverlayMapping fromFiles(EntityType<?> type) {
        return mappings.getOrDefault(EntityType.getKey(type), BreakOverlayMapping.DEFAULT);
    }

    /**
     * Switch one mob between fit and projection from the settings screen: writes its file in
     * config/minemobs/break_overlay/ (keeping any face adjustments) and applies it right away.
     */
    public static void setMode(EntityType<?> type, BreakOverlayMapping.Mode mode) {
        ResourceLocation id = EntityType.getKey(type);
        BreakOverlayMapping next = fromFiles(type).withMode(mode);
        Path file = CONFIG_DIR.resolve(id.getNamespace()).resolve(id.getPath() + ".json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(next.toJson(), writer);
            }
        } catch (IOException e) {
            MineMobs.LOGGER.error("Couldn't write {}", file, e);
            return;
        }
        Map<ResourceLocation, BreakOverlayMapping> updated = new HashMap<>(mappings);
        updated.put(id, next);
        mappings = Map.copyOf(updated);
    }

    @Override
    public ResourceLocation getFabricId() {
        return MineMobs.id(FOLDER);
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<ResourceLocation, BreakOverlayMapping> loaded = new HashMap<>();

        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(FOLDER, id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation mob = mobId(entry.getKey().getPath().substring(FOLDER.length() + 1));
            if (mob == null) continue;
            try (Reader reader = entry.getValue().openAsReader()) {
                loaded.put(mob, BreakOverlayMapping.fromJson(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception e) {
                MineMobs.LOGGER.error("Bad break_overlay file {}", entry.getKey(), e);
            }
        }

        try {
            Files.createDirectories(CONFIG_DIR);
            try (Stream<Path> files = Files.walk(CONFIG_DIR)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                    ResourceLocation mob = mobId(CONFIG_DIR.relativize(file).toString().replace('\\', '/'));
                    if (mob == null) continue;
                    try (Reader reader = Files.newBufferedReader(file)) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        loaded.put(mob, BreakOverlayMapping.fromJson(json));
                    } catch (Exception e) {
                        MineMobs.LOGGER.error("Bad break_overlay file {}", file, e);
                    }
                }
            }
        } catch (IOException e) {
            MineMobs.LOGGER.error("Couldn't read {}", CONFIG_DIR, e);
        }

        mappings = Map.copyOf(loaded);
        MineMobs.LOGGER.info("Loaded {} break_overlay file(s)", loaded.size());
    }

    /** "minecraft/pig.json" -> minecraft:pig */
    private static ResourceLocation mobId(String relative) {
        int slash = relative.indexOf('/');
        if (slash <= 0 || !relative.endsWith(".json")) return null;
        return ResourceLocation.tryBuild(relative.substring(0, slash), relative.substring(slash + 1, relative.length() - 5));
    }
}
