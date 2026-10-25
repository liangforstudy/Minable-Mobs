package io.github.liangforstudy.minemobs.mixin.client;

import io.github.liangforstudy.minemobs.client.ClientMiningState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClientMixin {
    /** Holds the legs in whatever pose they were in when mining started. */
    @Inject(method = "updateWalkAnimation", at = @At("HEAD"), cancellable = true)
    private void minemobs$freezeWalkAnimation(float distance, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide && ClientMiningState.isBeingMined(self.getId())) {
            ci.cancel();
        }
    }
}
