package io.github.liangforstudy.minemobs.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.liangforstudy.minemobs.client.overlay.BreakOverlayRenderer;
import io.github.liangforstudy.minemobs.client.ClientMiningState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Shadow protected EntityModel<?> model;

    /**
     * "Bob" is the age-in-ticks every model animates from: bee wings, blaze rods, bat wings, idle
     * keyframe animations, chicken flapping. Holding it at its first frozen value stops all of them.
     */
    @WrapOperation(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getBob(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float minemobs$freezeBob(LivingEntityRenderer<?, ?> renderer, LivingEntity entity, float partialTick, Operation<Float> original) {
        if (!ClientMiningState.isBeingMined(entity.getId())) return original.call(renderer, entity, partialTick);
        return ClientMiningState.heldBob(entity.getId(), () -> original.call(renderer, entity, partialTick));
    }

    /** Right after the body is drawn, draw it again with the crack texture on top. */
    @Inject(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", shift = At.Shift.AFTER))
    private void minemobs$drawCracks(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        // Iron golems show mining through their own crack textures instead (IronGolemMixin)
        BreakOverlayRenderer.render(entity, this.model, ClientMiningState.getStage(entity.getId()),
                entity instanceof IronGolem, poseStack, buffer, packedLight);
    }
}
