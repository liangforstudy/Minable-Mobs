package io.github.liangforstudy.minemobs.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.liangforstudy.minemobs.client.gui.MineMobsSettingsScreen;
import io.github.liangforstudy.minemobs.client.gui.TimeText;
import io.github.liangforstudy.minemobs.client.overlay.BreakOverlayMapping;
import io.github.liangforstudy.minemobs.client.overlay.BreakOverlayMappings;
import io.github.liangforstudy.minemobs.client.overlay.ModelExporter;
import io.github.liangforstudy.minemobs.mining.Mining;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.EntityHitResult;

import java.nio.file.Path;
import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Client-side /minemobs subcommands (model data lives on the client). Anything the client doesn't
 * know, like /minemobs on, falls through to the server's /minemobs.
 */
public final class MineMobsClientCommand {
    private MineMobsClientCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            // Dev only: simulate pressing attack and holding it, for automated tests (the game ignores
            // a mouse button held while its window isn't focused)
            dispatcher.register(literal("minemobs").then(literal("debug").then(literal("attack")
                    .then(argument("ticks", IntegerArgumentType.integer(0, 2000)).executes(ctx -> {
                        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                        ctx.getSource().getClient().tell(() -> ClientMiningController.debugAttack(ticks));
                        return 1;
                    })))));
        }
        dispatcher.register(literal("minemobs")
                .then(literal("exportmodel")
                        .executes(MineMobsClientCommand::exportLookedAt)
                        .then(argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                                .executes(MineMobsClientCommand::exportType)))
                .then(literal("overlay")
                        .then(literal("files").executes(ctx -> overlay(ctx, null)))
                        .then(literal("fit").executes(ctx -> overlay(ctx, BreakOverlayMapping.Mode.FIT)))
                        .then(literal("projection").executes(ctx -> overlay(ctx, BreakOverlayMapping.Mode.PROJECTION))))
                .then(literal("holddelay")
                        .executes(MineMobsClientCommand::showHoldDelay)
                        .then(argument("delay", StringArgumentType.word()).executes(MineMobsClientCommand::setHoldDelay)))
                .then(literal("settings")
                        .executes(ctx -> openSettings(ctx, null))
                        .then(argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                                .executes(ctx -> openSettings(ctx, entityArgument(ctx).value())))));
    }

    private static int showHoldDelay(CommandContext<FabricClientCommandSource> ctx) {
        int ticks = MineMobsClientConfig.get().holdDelayTicks;
        ctx.getSource().sendFeedback(Component.literal("Tap or hold delay: " + TimeText.both(ticks)
                + (ticks == 0 ? ", mining starts right away" : "")));
        return ticks;
    }

    /** "/minemobs holddelay 4", "4t" or "0.2s". 0 = mine right away, no tap to hit. */
    private static int setHoldDelay(CommandContext<FabricClientCommandSource> ctx) {
        Integer ticks = TimeText.parseWithUnit(StringArgumentType.getString(ctx, "delay"));
        if (ticks == null || ticks < 0 || ticks > MineMobsClientConfig.MAX_HOLD_DELAY) {
            ctx.getSource().sendError(Component.literal("Use 0 to " + MineMobsClientConfig.MAX_HOLD_DELAY
                    + " ticks, for example 4, 4t or 0.2s"));
            return 0;
        }
        MineMobsClientConfig.get().holdDelayTicks = ticks;
        MineMobsClientConfig.save();
        return showHoldDelay(ctx);
    }

    /** @param mob open the Mobs tab with this mob selected, or null for the screen as last left */
    private static int openSettings(CommandContext<FabricClientCommandSource> ctx, EntityType<?> mob) {
        Minecraft mc = ctx.getSource().getClient();
        // The chat screen closes right after the command runs, so open ours on the next tick
        mc.tell(() -> mc.setScreen(new MineMobsSettingsScreen(null, mob)));
        return 1;
    }

    @SuppressWarnings("unchecked")
    private static Holder.Reference<EntityType<?>> entityArgument(CommandContext<FabricClientCommandSource> ctx) {
        return ctx.getArgument("entity", Holder.Reference.class);
    }

    /** Compare against the plain modes without editing any break_overlay file. Not saved. */
    private static int overlay(CommandContext<FabricClientCommandSource> ctx, BreakOverlayMapping.Mode mode) {
        BreakOverlayMappings.setOverride(mode);
        ctx.getSource().sendFeedback(Component.literal(mode == null
                ? "Break overlay: using break_overlay files"
                : "Break overlay: plain " + mode.name().toLowerCase(Locale.ROOT) + " for every mob, files ignored (/minemobs overlay files to go back)"));
        return 1;
    }

    private static int exportLookedAt(CommandContext<FabricClientCommandSource> ctx) {
        if (!(ctx.getSource().getClient().hitResult instanceof EntityHitResult hit)) {
            ctx.getSource().sendError(Component.literal("Look at a mob, or name one: /minemobs exportmodel minecraft:pig"));
            return 0;
        }
        return export(ctx, Mining.resolve(hit.getEntity()));
    }

    @SuppressWarnings("unchecked")
    private static int exportType(CommandContext<FabricClientCommandSource> ctx) {
        Holder.Reference<EntityType<?>> type = ctx.getArgument("entity", Holder.Reference.class);
        Entity entity = type.value().create(ctx.getSource().getWorld());
        if (entity == null) {
            ctx.getSource().sendError(Component.literal("Can't create " + type.key().location() + " here"));
            return 0;
        }
        entity.moveTo(ctx.getSource().getPosition());
        return export(ctx, entity);
    }

    private static int export(CommandContext<FabricClientCommandSource> ctx, Entity entity) {
        try {
            Path file = ModelExporter.export(entity);
            if (file == null) {
                ctx.getSource().sendError(Component.literal(entity.getType().getDescription().getString() + " has no model the break overlay can draw on"));
                return 0;
            }
            Component link = Component.literal(file.getFileName().toString()).withStyle(style -> style
                    .withColor(ChatFormatting.GREEN).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getParent().toString())));
            ctx.getSource().sendFeedback(Component.literal("Exported ").append(link).append(" (click to open the folder)"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Component.literal("Export failed: " + e.getMessage()));
            return 0;
        }
    }
}
