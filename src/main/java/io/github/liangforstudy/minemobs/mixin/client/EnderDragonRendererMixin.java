package io.github.liangforstudy.minemobs.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.liangforstudy.minemobs.client.overlay.BreakOverlayRenderer;
import io.github.liangforstudy.minemobs.client.ClientMiningState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The dragon has its own renderer (not a LivingEntityRenderer), so it gets cracks separately. */
@Mixin(EnderDragonRenderer.class)
public abstract class EnderDragonRendererMixin {
    @Shadow @Final private EnderDragonRenderer.DragonModel model;

    /** After the eyes pass (the last pass that uses the body pose), add the cracks. */
    @Inject(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", ordinal = 2, shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/renderer/entity/EnderDragonRenderer$DragonModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void minemobs$drawCracks(EnderDragon entity, float entityYaw, float partialTicks, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        BreakOverlayRenderer.render(entity, this.model, ClientMiningState.getStage(entity.getId()), false, poseStack, buffer, packedLight);
    }
}
