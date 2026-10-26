package io.github.liangforstudy.minemobs.client.overlay;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/**
 * How the break texture sits on one face. Applied to the face's base UVs, around the face's center,
 * in this order: scale, flip, rotate, offset. The web app does exactly the same math.
 *
 * @param rotation clockwise, 0 / 90 / 180 / 270
 * @param offsetU  in texture pixels (16 = one whole texture)
 * @param offsetV  in texture pixels
 * @param scale    2 = texture pixels twice as big
 * @param stretch  fit mode only: stretch one whole texture over the face instead of 1 pixel per pixel
 */
public record FaceAdjust(int rotation, boolean flipU, boolean flipV, float offsetU, float offsetV, float scale, boolean stretch) {
    public static final FaceAdjust IDENTITY = new FaceAdjust(0, false, false, 0.0F, 0.0F, 1.0F, false);

    /** Missing fields keep the fallback's value. */
    public static FaceAdjust fromJson(JsonObject json, FaceAdjust fallback) {
        int rotation = Math.floorMod(Math.round(GsonHelper.getAsInt(json, "rotation", fallback.rotation) / 90.0F) * 90, 360);
        float scale = GsonHelper.getAsFloat(json, "scale", fallback.scale);
        return new FaceAdjust(
                rotation,
                GsonHelper.getAsBoolean(json, "flip_u", fallback.flipU),
                GsonHelper.getAsBoolean(json, "flip_v", fallback.flipV),
                GsonHelper.getAsFloat(json, "offset_u", fallback.offsetU),
                GsonHelper.getAsFloat(json, "offset_v", fallback.offsetV),
                scale > 0.0F ? scale : fallback.scale,
                GsonHelper.getAsBoolean(json, "stretch", fallback.stretch));
    }

    /** Only the fields that differ from base, as break_overlay JSON. */
    public JsonObject toJson(FaceAdjust base) {
        JsonObject json = new JsonObject();
        if (rotation != base.rotation) json.addProperty("rotation", rotation);
        if (flipU != base.flipU) json.addProperty("flip_u", flipU);
        if (flipV != base.flipV) json.addProperty("flip_v", flipV);
        if (offsetU != base.offsetU) json.addProperty("offset_u", offsetU);
        if (offsetV != base.offsetV) json.addProperty("offset_v", offsetV);
        if (scale != base.scale) json.addProperty("scale", scale);
        if (stretch != base.stretch) json.addProperty("stretch", stretch);
        return json;
    }

    /** Transforms 4 base UVs in place: {u0, v0, u1, v1, u2, v2, u3, v3}. */
    public void apply(float[] uv) {
        float cu = (uv[0] + uv[2] + uv[4] + uv[6]) / 4.0F;
        float cv = (uv[1] + uv[3] + uv[5] + uv[7]) / 4.0F;
        int turns = rotation / 90;
        for (int i = 0; i < 8; i += 2) {
            float du = (uv[i] - cu) / scale;
            float dv = (uv[i + 1] - cv) / scale;
            if (flipU) du = -du;
            if (flipV) dv = -dv;
            for (int t = 0; t < turns; t++) {
                float old = du;
                du = -dv;
                dv = old;
            }
            uv[i] = cu + du + offsetU / 16.0F;
            uv[i + 1] = cv + dv + offsetV / 16.0F;
        }
    }
}
