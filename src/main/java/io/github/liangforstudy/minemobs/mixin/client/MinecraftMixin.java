package io.github.liangforstudy.minemobs.mixin.client;

import io.github.liangforstudy.minemobs.client.ClientMiningController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow public LocalPlayer player;
    @Shadow public int missTime;

    /** A press on a minable mob with a mining tool: hold to mine, or tap to hit (ClientMiningController). */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void minemobs$startMining(CallbackInfoReturnable<Boolean> cir) {
        if (this.missTime > 0 || this.player == null || this.player.isHandsBusy()) return;
        if (ClientMiningController.onPress((Minecraft) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    /** Called every tick with whether attack is held: the entity version of continueDestroyBlock. */
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void minemobs$continueMining(boolean leftClick, CallbackInfo ci) {
        boolean simulated = this.player != null && ClientMiningController.debugHeld((Minecraft) (Object) this);
        boolean held = (leftClick || simulated) && this.missTime <= 0 && this.player != null && !this.player.isUsingItem();
        if (ClientMiningController.tick((Minecraft) (Object) this, held)) {
            ci.cancel();
        }
    }
}
