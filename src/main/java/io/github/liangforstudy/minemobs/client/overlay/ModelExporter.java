package io.github.liangforstudy.minemobs.client.overlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * /minemobs exportmodel: renders a mob once through the break overlay and writes every face it drew
 * (id, corners, skin UVs, and the base break UVs for each mode) to minemobs-export/, for the tuner app.
 */
public final class ModelExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path EXPORT_DIR = Minecraft.getInstance().gameDirectory.toPath().resolve("minemobs-export");

    @Nullable private static Recorder current;

    private ModelExporter() {
    }

    /** The recorder if this entity is being exported right now. */
    @Nullable
    static Recorder recorderFor(Entity entity) {
        return current != null && current.entity == entity ? current : null;
    }

    /** @return the written file, or null if the mob has no break overlay to export */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Path export(Entity entity) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        Recorder recorder = new Recorder(entity);
        current = recorder;
        try {
            renderer.render(entity, 0.0F, 1.0F, new PoseStack(), DISCARD, 0xF000F0);
        } finally {
            current = null;
        }
        if (recorder.faces.isEmpty()) return null;

        ResourceLocation type = EntityType.getKey(entity.getType());
        JsonObject json = new JsonObject();
        json.addProperty("format", 1);
        json.addProperty("entity", type.toString());
        json.addProperty("texture", renderer.getTextureLocation(entity).toString());
        json.addProperty("note", "pos: corners for display (Y up, blocks). uv: skin UVs. fit / fit_stretch / projection: base break UVs per mode, before adjustments.");
        json.add("faces", recorder.faces);

        Files.createDirectories(EXPORT_DIR);
        Path file = EXPORT_DIR.resolve(type.getNamespace() + "_" + type.getPath().replace('/', '_') + ".json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(json, writer);
        }
        return file;
    }

    static final class Recorder {
        final Entity entity;
        final JsonArray faces = new JsonArray();
        /**
         * Model space -> display space. Renderers draw models flipped with scale(-1, -1, 1) and
         * shifted by 1.501; redo that so +Y is up and the feet are at 0.
         */
        private static final Matrix4f TO_DISPLAY = new Matrix4f().scale(-1.0F, -1.0F, 1.0F).translate(0.0F, -1.501F, 0.0F);

        Recorder(Entity entity) {
            this.entity = entity;
        }

        void record(String faceId, float[] local, float[] modelUv, Vector3f normal) {
            JsonObject face = new JsonObject();
            face.addProperty("id", faceId);
            JsonArray pos = new JsonArray();
            Vector3f p = new Vector3f();
            for (int i = 0; i < 4; i++) {
                TO_DISPLAY.transformPosition(local[i * 3], local[i * 3 + 1], local[i * 3 + 2], p);
                pos.add(array(round(p.x), round(p.y), round(p.z)));
            }
            face.add("pos", pos);
            face.add("uv", pairs(modelUv));
            face.add("fit", pairs(FaceUv.fit(local, modelUv, normal, false)));
            face.add("fit_stretch", pairs(FaceUv.fit(local, modelUv, normal, true)));
            face.add("projection", pairs(FaceUv.projection(local, normal)));
            faces.add(face);
        }

        private static JsonArray pairs(float[] uv) {
            JsonArray out = new JsonArray();
            for (int i = 0; i < 4; i++) out.add(array(round(uv[i * 2]), round(uv[i * 2 + 1])));
            return out;
        }

        private static JsonArray array(float... values) {
            JsonArray out = new JsonArray();
            for (float value : values) out.add(value);
            return out;
        }

        private static float round(float value) {
            return Math.round(value * 100000.0F) / 100000.0F;
        }
    }

    /** Swallows everything the renderer draws; only the overlay's recorder matters. */
    private static final MultiBufferSource DISCARD = renderType -> new VertexConsumer() {
        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float normalX, float normalY, float normalZ) { return this; }
    };
}
