package io.github.liangforstudy.minemobs.client.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Sits between a model and the destroy_stage buffer. Collects each face's 4 vertices, works out
 * where the break texture goes on that face (fit or projection mode plus the face's adjustment),
 * then passes the face on with those UVs.
 */
final class BreakOverlayConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    /** Undoes the pose the model was drawn with, giving positions relative to the mob, in blocks. */
    private final Matrix4f inversePose;
    private final Matrix3f inverseNormal;
    private final BreakOverlayMapping mapping;
    @Nullable private final ModelExporter.Recorder recorder;

    private String cubeId = "unknown";
    private ModelPart.Polygon[] polygons = new ModelPart.Polygon[0];
    private int faceInCube;

    // The face being collected
    private int vertex;
    private final float[] pos = new float[12];
    private final float[] local = new float[12];
    private final float[] modelUv = new float[8];
    private final int[] light = new int[4];
    private float nx, ny, nz;

    BreakOverlayConsumer(VertexConsumer delegate, PoseStack.Pose pose, BreakOverlayMapping mapping, @Nullable ModelExporter.Recorder recorder) {
        this.delegate = delegate;
        this.inversePose = new Matrix4f(pose.pose()).invert();
        this.inverseNormal = new Matrix3f(pose.normal()).invert();
        this.mapping = mapping;
        this.recorder = recorder;
    }

    void beginCube(String cubeId, ModelPart.Polygon[] polygons) {
        this.cubeId = cubeId;
        this.polygons = polygons;
        this.faceInCube = 0;
        this.vertex = 0;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        pos[vertex * 3] = x;
        pos[vertex * 3 + 1] = y;
        pos[vertex * 3 + 2] = z;
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        modelUv[vertex * 2] = u;
        modelUv[vertex * 2 + 1] = v;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        light[vertex] = (u & 0xFFFF) | (v << 16);
        return this;
    }

    /** The last call for each vertex, as in vanilla's addVertex(...) chain. */
    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        nx = normalX;
        ny = normalY;
        nz = normalZ;
        if (++vertex == 4) {
            vertex = 0;
            emitFace();
        }
        return this;
    }

    private void emitFace() {
        String faceId = cubeId + "/" + (faceInCube < polygons.length ? ModelFaces.faceName(polygons[faceInCube]) : "face" + faceInCube);
        faceInCube++;

        Vector3f tmp = new Vector3f();
        for (int i = 0; i < 4; i++) {
            inversePose.transformPosition(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2], tmp);
            local[i * 3] = tmp.x;
            local[i * 3 + 1] = tmp.y;
            local[i * 3 + 2] = tmp.z;
        }
        Vector3f localNormal = inverseNormal.transform(nx, ny, nz, new Vector3f());

        FaceAdjust adjust = mapping.adjust(faceId);
        float[] uv = mapping.mode() == BreakOverlayMapping.Mode.FIT
                ? FaceUv.fit(local, modelUv, localNormal, adjust.stretch())
                : FaceUv.projection(local, localNormal);
        if (recorder != null) recorder.record(faceId, local, modelUv, localNormal);
        adjust.apply(uv);

        for (int i = 0; i < 4; i++) {
            delegate.addVertex(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2])
                    .setColor(-1)
                    .setUv(uv[i * 2], uv[i * 2 + 1])
                    .setLight(light[i])
                    .setNormal(nx, ny, nz);
        }
    }
}
