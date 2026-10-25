package io.github.liangforstudy.minemobs.mining;

import io.github.liangforstudy.minemobs.MineMobsTags;
import io.github.liangforstudy.minemobs.config.MineMobsConfig;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Rules shared by client and server: what can be mined, with what, and how fast. */
public final class Mining {
    /** Anything that can harvest iron ore is a valid tool: stone pickaxe and up, but not wood. */
    public static final BlockState TOOL_CHECK_BLOCK = Blocks.IRON_ORE.defaultBlockState();

    private Mining() {
    }

    public static boolean isMiningTool(ItemStack stack) {
        return !stack.isEmpty() && (stack.isCorrectToolForDrops(TOOL_CHECK_BLOCK) || stack.is(MineMobsTags.EXTRA_MINING_TOOLS));
    }

    /** The entity actually being mined: hitting a dragon's body part mines the dragon. */
    public static Entity resolve(Entity hit) {
        return hit instanceof EnderDragonPart part ? part.parentMob : hit;
    }

    public static boolean isMinable(Entity entity, Player miner) {
        return entity instanceof LivingEntity living
                && living != miner
                && living.isAlive()
                && !living.getType().is(MineMobsTags.UNMINABLE)
                // Players follow the same rules as hitting them: server PvP setting and teams
                && !(living instanceof Player target && (target.isSpectator() || !miner.canHarmPlayer(target)));
    }

    public static boolean canMine(Player player, Entity target) {
        return !player.isSpectator() && isMiningTool(player.getMainHandItem()) && isMinable(target, player);
    }

    /**
     * Progress per tick (1.0 = broken). Each mob takes a set number of ticks (per-mob settings, or the
     * defaults), with a separate time for netherite pickaxes (#minemobs:fast_mining_tools). Haste,
     * Mining Fatigue, underwater and mid-air speed it up or slow it down like they do for blocks.
     */
    public static float progressPerTick(Player player, LivingEntity target) {
        MineMobsConfig config = MineMobsConfig.get();
        int ticks = player.getMainHandItem().is(MineMobsTags.FAST_MINING_TOOLS)
                ? config.netheriteTicks(target.getType())
                : config.breakTicks(target.getType());
        if (ticks <= 0) return 1.0F;
        return effectMultiplier(player) / ticks;
    }

    /**
     * The part of Player#getDestroySpeed that isn't the tool: Haste, Mining Fatigue, the break-speed
     * attribute, water and mid-air. 1.0 when none apply.
     */
    private static float effectMultiplier(Player player) {
        float speed = 1.0F;
        if (MobEffectUtil.hasDigSpeed(player)) {
            speed *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(player) + 1) * 0.2F;
        }
        if (player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            speed *= switch (player.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
        }
        speed *= (float) player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        if (player.isEyeInFluid(FluidTags.WATER)) {
            speed *= (float) player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
        }
        if (!player.onGround()) {
            speed /= 5.0F;
        }
        return speed;
    }
}
