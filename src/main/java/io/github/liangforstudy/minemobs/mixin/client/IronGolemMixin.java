package io.github.liangforstudy.minemobs.mixin.client;

import io.github.liangforstudy.minemobs.client.ClientMiningState;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.animal.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Iron golems already have crack textures (driven by health). While mined, show whichever is worse:
 * the health cracks or the mining cracks. Mining progress resets on abort, so they revert to health.
 */
@Mixin(IronGolem.class)
public abstract class IronGolemMixin {
    @Inject(method = "getCrackiness", at = @At("RETURN"), cancellable = true)
    private void minemobs$miningCracks(CallbackInfoReturnable<Crackiness.Level> cir) {
        IronGolem self = (IronGolem) (Object) this;
        if (!self.level().isClientSide) return;
        Crackiness.Level mining = ClientMiningState.getCrackiness(self.getId());
        if (mining.ordinal() > cir.getReturnValue().ordinal()) cir.setReturnValue(mining);
    }
}
