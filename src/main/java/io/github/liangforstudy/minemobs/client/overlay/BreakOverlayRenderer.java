package io.github.liangforstudy.minemobs.client.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.entity.Entity;

/**
 * Block-breaking cracks on an entity model, the way vanilla cracks a chest: draw the model again into
 * the destroy_stage render type, but with our own UVs. Where the texture lands on each face comes
 * from the mob's break_overlay file (BreakOverlayMappings).
 */
public final class BreakOverlayRenderer {
    private BreakOverlayRenderer() {
    }

    /**
     * @param suppressed true for mobs that show mining another way (iron golems); still exportable
     */
    public static void render(Entity entity, Model model, int stage, boolean suppressed,
                              PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ModelExporter.Recorder recorder = ModelExporter.recorderFor(entity);
        if (recorder == null && (stage < 0 || suppressed)) return;
        if (recorder != null) stage = Math.max(stage, 0);

        BreakOverlayConsumer consumer = new BreakOverlayConsumer(
                buffer.getBuffer(ModelBakery.DESTROY_TYPES.get(stage)), poseStack.last(),
                BreakOverlayMappings.get(entity.getType()), recorder);
        ModelFaces.begin(consumer);
        try {
            model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
        } finally {
            ModelFaces.end();
        }
    }
}
