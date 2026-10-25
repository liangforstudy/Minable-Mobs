package io.github.liangforstudy.minemobs.mixin;

import io.github.liangforstudy.minemobs.mining.MiningManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    private LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * While being mined, skip AI, pathfinding, jumping and travel (which is also where gravity lives),
     * so the mob hangs in place — midair if it was jumping. fallDistance is left alone.
     */
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void minemobs$freezeWhileMined(CallbackInfo ci) {
        if (!this.level().isClientSide && MiningManager.isFrozen(this)) {
            this.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
        }
    }
}
