package io.github.liangforstudy.minemobs.client.overlay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * One mob's break_overlay file:
 * <pre>{
 *   "mode": "fit",                      // or "projection"
 *   "default": { "rotation": 0 },       // optional, for faces not listed below
 *   "faces": { "body/cube0/top": { "rotation": 90, "offset_u": 4 } }
 * }</pre>
 */
public record BreakOverlayMapping(Mode mode, FaceAdjust defaults, Map<String, FaceAdjust> faces) {
    public static final BreakOverlayMapping DEFAULT = new BreakOverlayMapping(Mode.FIT, FaceAdjust.IDENTITY, Map.of());

    public enum Mode {
        /** Each face gets its own copy of the texture, lined up with the face's corners. */
        FIT,
        /** One texture per block, projected from the mob's center (vanilla block-entity style). */
        PROJECTION
    }

    public FaceAdjust adjust(String faceId) {
        return faces.getOrDefault(faceId, defaults);
    }

    public BreakOverlayMapping withMode(Mode newMode) {
        return new BreakOverlayMapping(newMode, defaults, faces);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", mode.name().toLowerCase(Locale.ROOT));
        if (!defaults.equals(FaceAdjust.IDENTITY)) json.add("default", defaults.toJson(FaceAdjust.IDENTITY));
        JsonObject faceJson = new JsonObject();
        faces.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> faceJson.add(e.getKey(), e.getValue().toJson(defaults)));
        json.add("faces", faceJson);
        return json;
    }

    public static BreakOverlayMapping fromJson(JsonObject json) {
        Mode mode = Mode.valueOf(GsonHelper.getAsString(json, "mode", "fit").toUpperCase(Locale.ROOT));
        FaceAdjust defaults = json.has("default")
                ? FaceAdjust.fromJson(GsonHelper.getAsJsonObject(json, "default"), FaceAdjust.IDENTITY)
                : FaceAdjust.IDENTITY;
        Map<String, FaceAdjust> faces = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "faces", new JsonObject()).entrySet()) {
            faces.put(entry.getKey(), FaceAdjust.fromJson(GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey()), defaults));
        }
        return new BreakOverlayMapping(mode, defaults, Map.copyOf(faces));
    }
}
