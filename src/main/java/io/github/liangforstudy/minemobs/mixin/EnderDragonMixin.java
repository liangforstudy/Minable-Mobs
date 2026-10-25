package io.github.liangforstudy.minemobs.mixin;

import io.github.liangforstudy.minemobs.mining.MiningManager;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The dragon replaces LivingEntity#aiStep without calling super, so it needs its own freeze. */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void minemobs$freezeWhileMined(CallbackInfo ci) {
        EnderDragon self = (EnderDragon) (Object) this;
        if (!self.level().isClientSide && MiningManager.isFrozen(self)) {
            self.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
        }
    }
}
