package io.github.liangforstudy.minemobs.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.liangforstudy.minemobs.client.overlay.ModelFaces;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the break overlay which box the next faces belong to. A no-op unless the overlay is drawing. */
@Mixin(ModelPart.Cube.class)
public abstract class ModelPartCubeMixin {
    @Shadow @Final private ModelPart.Polygon[] polygons;

    @Inject(method = "compile", at = @At("HEAD"))
    private void minemobs$trackCube(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color, CallbackInfo ci) {
        ModelFaces.onCubeCompile((ModelPart.Cube) (Object) this, this.polygons);
    }
}
