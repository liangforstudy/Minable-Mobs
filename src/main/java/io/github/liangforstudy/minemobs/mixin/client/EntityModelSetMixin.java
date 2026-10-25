package io.github.liangforstudy.minemobs.mixin.client;

import io.github.liangforstudy.minemobs.client.overlay.ModelFaces;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Names every box of every model as it's baked, so overlay settings can address single faces. */
@Mixin(EntityModelSet.class)
public abstract class EntityModelSetMixin {
    @Inject(method = "bakeLayer", at = @At("RETURN"))
    private void minemobs$nameFaces(ModelLayerLocation layer, CallbackInfoReturnable<ModelPart> cir) {
        ModelFaces.register(cir.getReturnValue());
    }
}
