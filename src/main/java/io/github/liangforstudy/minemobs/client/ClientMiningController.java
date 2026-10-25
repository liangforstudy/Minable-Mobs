package io.github.liangforstudy.minemobs.client;

import io.github.liangforstudy.minemobs.mining.Mining;
import io.github.liangforstudy.minemobs.network.MineEntityPayload;
import io.github.liangforstudy.minemobs.network.MineEntityPayload.Action;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Client half of mob mining. With a mining tool on a minable mob, a press is held back for the hold
 * delay: let go before it ends and it's a normal hit; keep holding and it becomes mining (START /
 * CONTINUE / ABORT packets; progress itself lives on the server). Delay 0 mines right away.
 */
public final class ClientMiningController {
    /** Entity being mined, or -1. */
    private static int targetId = -1;
    /** Pressed on this mob and still deciding between tap and hold, or -1. */
    private static int pendingId = -1;
    private static int pendingTicks;
    /** How long attack has been held, for sweeping onto a mob mid-hold. */
    private static int heldTicks;

    private ClientMiningController() {
    }

    /**
     * startAttack: the attack button was just pressed.
     *
     * @return true if mob mining took the press, so vanilla must not attack
     */
    public static boolean onPress(Minecraft mc) {
        Entity target = minableTarget(mc);
        if (target == null) return false;
        if (holdDelay() <= 0) {
            mine(mc, target, 0);
        } else {
            stop();
            pendingId = target.getId();
            pendingTicks = 0;
        }
        return true;
    }

    /**
     * continueAttack, every tick.
     *
     * @param attackHeld attack is down and usable (no miss cooldown, not using an item)
     * @return true if vanilla should skip its own block-breaking step this tick
     */
    public static boolean tick(Minecraft mc, boolean attackHeld) {
        if (mc.screen != null) {
            // A menu opened mid-press: neither a hit nor mining
            pendingId = -1;
            heldTicks = 0;
            stop();
            return false;
        }
        heldTicks = attackHeld ? heldTicks + 1 : 0;

        if (pendingId != -1) {
            if (!attackHeld) {
                tap(mc);
                return false;
            }
            if (++pendingTicks < holdDelay()) return true;
            // Held long enough: mine whatever minable mob is under the crosshair now
            int held = pendingTicks;
            pendingId = -1;
            Entity target = minableTarget(mc);
            if (target == null) return false;
            mine(mc, target, held - 1); // this tick is counted by the server itself
            return true;
        }

        // Keep mining, or start when the crosshair sweeps onto a mob while attack is already held
        if (attackHeld && heldTicks > holdDelay()) {
            Entity target = minableTarget(mc);
            if (target != null) {
                mine(mc, target, 0);
                return true;
            }
        } else if (attackHeld && targetId != -1) {
            Entity target = minableTarget(mc);
            if (target != null && target.getId() == targetId) {
                mine(mc, target, 0);
                return true;
            }
        }
        stop();
        return false;
    }

    /** Released before the hold delay ended: a normal hit on whatever is under the crosshair now. */
    private static void tap(Minecraft mc) {
        pendingId = -1;
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.hitResult instanceof EntityHitResult hit) {
            mc.gameMode.attack(mc.player, hit.getEntity());
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private static void mine(Minecraft mc, Entity target, int heldBefore) {
        if (target.getId() != targetId) {
            stop();
            targetId = target.getId();
            send(Action.START, heldBefore);
        } else {
            send(Action.CONTINUE, 0);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    /** Released the button, looked away, switched item... */
    public static void stop() {
        if (targetId != -1) {
            send(Action.ABORT, 0);
            targetId = -1;
        }
    }

    public static void reset() {
        targetId = -1;
        pendingId = -1;
        heldTicks = 0;
    }

    private static int holdDelay() {
        return MineMobsClientConfig.get().holdDelayTicks;
    }

    private static Entity minableTarget(Minecraft mc) {
        if (mc.player == null || !ClientMiningState.isServerEnabled()
                || !ClientPlayNetworking.canSend(MineEntityPayload.TYPE)) {
            return null;
        }
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY) return null;
        Entity entity = Mining.resolve(((EntityHitResult) mc.hitResult).getEntity());
        return Mining.canMine(mc.player, entity) ? entity : null;
    }

    private static void send(Action action, int heldBefore) {
        if (ClientPlayNetworking.canSend(MineEntityPayload.TYPE)) {
            ClientPlayNetworking.send(new MineEntityPayload(targetId, action, heldBefore));
        }
    }
}
