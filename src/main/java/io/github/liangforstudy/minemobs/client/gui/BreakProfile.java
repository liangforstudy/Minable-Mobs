package io.github.liangforstudy.minemobs.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * "As hard as ..." presets for a mob's break time: how long vanilla takes to break that block with
 * that tool, worked out with the game's own formula (so they follow any mod that changes it).
 *
 * @param icons the block, then the tool (none for a bare fist); shown on the profile button
 */
public record BreakProfile(String name, List<ItemStack> icons, int ticks) {
    public static List<BreakProfile> all() {
        return List.of(
                of("Wood with a bare fist", Blocks.OAK_LOG.defaultBlockState(), ItemStack.EMPTY),
                of("Stone with a stone pickaxe", Blocks.STONE.defaultBlockState(), new ItemStack(Items.STONE_PICKAXE)),
                of("Stone with a diamond pickaxe", Blocks.STONE.defaultBlockState(), new ItemStack(Items.DIAMOND_PICKAXE)),
                of("Stone with a netherite pickaxe", Blocks.STONE.defaultBlockState(), new ItemStack(Items.NETHERITE_PICKAXE)));
    }

    private static BreakProfile of(String name, BlockState block, ItemStack tool) {
        ItemStack blockIcon = new ItemStack(block.getBlock().asItem());
        List<ItemStack> icons = tool.isEmpty() ? List.of(blockIcon) : List.of(blockIcon, tool);
        return new BreakProfile(name, icons, ticksToBreak(block, tool));
    }

    /** Vanilla: each tick adds speed / hardness / 30 (or / 100 without the right tool); 1.0 breaks it. */
    static int ticksToBreak(BlockState block, ItemStack tool) {
        float hardness = block.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (hardness <= 0.0F) return 0;
        float speed = tool.isEmpty() ? 1.0F : tool.getDestroySpeed(block);
        boolean canHarvest = !block.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(block);
        float perTick = speed / hardness / (canHarvest ? 30.0F : 100.0F);
        return Math.max(1, (int) Math.ceil(1.0F / perTick - 1.0E-4F));
    }
}
