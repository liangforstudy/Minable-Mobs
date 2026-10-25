package io.github.liangforstudy.minemobs.mixin.client;

import io.github.liangforstudy.minemobs.client.ClientMiningState;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The dragon's wings are driven by flapTime, which the client advances every tick on its own.
 * While mined, put it back after each tick so the wings (and the flap sound) stop where they were.
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonClientMixin {
    @Shadow public float oFlapTime;
    @Shadow public float flapTime;
    @Unique private float minemobs$heldFlapTime;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void minemobs$holdFlap(CallbackInfo ci) {
        this.minemobs$heldFlapTime = this.flapTime;
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void minemobs$restoreFlap(CallbackInfo ci) {
        EnderDragon self = (EnderDragon) (Object) this;
        if (self.level().isClientSide && ClientMiningState.isBeingMined(self.getId())) {
            this.oFlapTime = this.minemobs$heldFlapTime;
            this.flapTime = this.minemobs$heldFlapTime;
        }
    }
}
