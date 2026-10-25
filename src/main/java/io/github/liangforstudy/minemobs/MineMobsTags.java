package io.github.liangforstudy.minemobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class MineMobsTags {
    /** Items that can mine mobs on top of anything that can harvest iron ore. */
    public static final TagKey<Item> EXTRA_MINING_TOOLS = TagKey.create(Registries.ITEM, MineMobs.id("extra_mining_tools"));
    /** Tools that use each mob's netherite time instead of its normal break time (netherite pickaxe). */
    public static final TagKey<Item> FAST_MINING_TOOLS = TagKey.create(Registries.ITEM, MineMobs.id("fast_mining_tools"));
    /** Entities that can never be mined. Empty by default; datapacks can add mobs. */
    public static final TagKey<EntityType<?>> UNMINABLE = TagKey.create(Registries.ENTITY_TYPE, MineMobs.id("unminable"));

    private MineMobsTags() {
    }
}
