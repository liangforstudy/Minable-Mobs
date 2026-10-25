package io.github.liangforstudy.minemobs.client.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stable names for every face of every baked entity model, like {@code body/cube0/top}. They come from
 * the model's own part names (the same paths ModelPart#visit uses), so they survive restarts and
 * work for modded mobs whose models are baked through EntityModelSet.
 */
public final class ModelFaces {
    /** Box -> "partPath/cubeN". Weak, so models dropped on resource reload don't pile up. */
    private static final Map<ModelPart.Cube, String> CUBE_IDS = new WeakHashMap<>();
    private static final String UNKNOWN = "unknown";

    private ModelFaces() {
    }

    /** Called for every freshly baked model layer (EntityModelSetMixin). */
    public static void register(ModelPart root) {
        root.visit(new PoseStack(), (pose, path, index, cube) ->
                CUBE_IDS.put(cube, (path.isEmpty() ? "root" : path.substring(1)) + "/cube" + index));
    }

    public static String cubeId(ModelPart.Cube cube) {
        return CUBE_IDS.getOrDefault(cube, UNKNOWN);
    }

    /**
     * The face's side, named from the mob's point of view in model space: the head is at -Z ("front"),
     * model Y points down (so -Y is "top"), and +X is the mob's left. Named from the box's own
     * orientation, so it doesn't change when a part is rotated or animated.
     */
    public static String faceName(ModelPart.Polygon polygon) {
        Vector3f n = polygon.normal;
        return switch (Direction.getNearest(n.x(), n.y(), n.z())) {
            case DOWN -> "top";
            case UP -> "bottom";
            case NORTH -> "front";
            case SOUTH -> "back";
            case EAST -> "left";
            case WEST -> "right";
        };
    }

    // --- Which box is being drawn right now (only while the break overlay renders) ---

    private static BreakOverlayConsumer active;

    static void begin(BreakOverlayConsumer consumer) {
        active = consumer;
    }

    static void end() {
        active = null;
    }

    /** ModelPartCubeMixin: a box is about to emit its faces. */
    public static void onCubeCompile(ModelPart.Cube cube, ModelPart.Polygon[] polygons) {
        if (active != null) active.beginCube(cubeId(cube), polygons);
    }
}
