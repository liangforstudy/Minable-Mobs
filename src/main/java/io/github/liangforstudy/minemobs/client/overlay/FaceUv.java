package io.github.liangforstudy.minemobs.client.overlay;

import org.joml.Vector3f;

/**
 * Base break-texture UVs for one face (before its FaceAdjust), in texture units: 1.0 = one whole
 * 16px texture. Inputs are the face's 4 corners relative to the mob in blocks, its model UVs, and
 * its normal in the same space.
 */
final class FaceUv {
    private FaceUv() {
    }

    /**
     * Fit: line the texture up with the face's own corners, oriented like the mob's skin on that face.
     * Normally one texture pixel per model pixel, so a 16px-wide face shows the texture once; with
     * stretch, the whole texture covers the face exactly.
     */
    static float[] fit(float[] local, float[] modelUv, Vector3f normal, boolean stretch) {
        float uMin = Float.MAX_VALUE, uMax = -Float.MAX_VALUE, vMin = Float.MAX_VALUE, vMax = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            uMin = Math.min(uMin, modelUv[i * 2]);
            uMax = Math.max(uMax, modelUv[i * 2]);
            vMin = Math.min(vMin, modelUv[i * 2 + 1]);
            vMax = Math.max(vMax, modelUv[i * 2 + 1]);
        }
        // Zero-width faces (flat planes' edges) have no skin direction to follow
        if (uMax - uMin < 1.0E-6F || vMax - vMin < 1.0E-6F) return projection(local, normal);

        float[] uv = new float[8];
        int origin = -1, alongU = -1, alongV = -1;
        for (int i = 0; i < 4; i++) {
            float u = (modelUv[i * 2] - uMin) / (uMax - uMin);
            float v = (modelUv[i * 2 + 1] - vMin) / (vMax - vMin);
            uv[i * 2] = u;
            uv[i * 2 + 1] = v;
            if (u < 0.5F && v < 0.5F) origin = i;
            else if (u >= 0.5F && v < 0.5F) alongU = i;
            else if (u < 0.5F) alongV = i;
        }
        if (stretch || origin < 0 || alongU < 0 || alongV < 0) return uv;

        // Real face size in blocks; 1 block = 16 pixels = one texture
        float width = distance(local, origin, alongU);
        float height = distance(local, origin, alongV);
        for (int i = 0; i < 4; i++) {
            uv[i * 2] *= width;
            uv[i * 2 + 1] *= height;
        }
        return uv;
    }

    /**
     * Projection: one texture per block across the whole mob, taken from the axis the face points
     * along. Neighbouring faces line up, but the texture ignores face edges.
     */
    static float[] projection(float[] local, Vector3f normal) {
        float ax = Math.abs(normal.x()), ay = Math.abs(normal.y()), az = Math.abs(normal.z());
        float[] uv = new float[8];
        for (int i = 0; i < 4; i++) {
            float x = local[i * 3], y = local[i * 3 + 1], z = local[i * 3 + 2];
            float u, v;
            if (ax >= ay && ax >= az) {
                u = normal.x() > 0 ? -z : z;
                v = y;
            } else if (ay >= az) {
                u = x;
                v = normal.y() > 0 ? -z : z;
            } else {
                u = normal.z() > 0 ? x : -x;
                v = y;
            }
            uv[i * 2] = u;
            uv[i * 2 + 1] = v;
        }
        return uv;
    }

    private static float distance(float[] p, int a, int b) {
        float dx = p[a * 3] - p[b * 3], dy = p[a * 3 + 1] - p[b * 3 + 1], dz = p[a * 3 + 2] - p[b * 3 + 2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
