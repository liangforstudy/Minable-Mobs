package io.github.liangforstudy.minemobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.liangforstudy.minemobs.config.MineMobsConfig;
import io.github.liangforstudy.minemobs.config.SettingsSync;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;

/** /minemobs toggles mob mining for everyone; /minemobs on|off sets it explicitly (ops only). */
public final class MineMobsCommand {
    private MineMobsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("minemobs")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> set(ctx, !MineMobsConfig.get().enabled))
                .then(Commands.literal("on").executes(ctx -> set(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> set(ctx, false)))
                // exportmodel, overlay, holddelay and settings run on the client (MineMobsClientCommand). Listed here too because the server's
                // /minemobs is what the chat box checks and tab-completes against.
                .then(Commands.literal("exportmodel")
                        .executes(MineMobsCommand::clientOnly)
                        .then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                                .executes(MineMobsCommand::clientOnly)))
                .then(Commands.literal("overlay")
                        .then(Commands.literal("files").executes(MineMobsCommand::clientOnly))
                        .then(Commands.literal("fit").executes(MineMobsCommand::clientOnly))
                        .then(Commands.literal("projection").executes(MineMobsCommand::clientOnly)))
                .then(Commands.literal("holddelay")
                        .executes(MineMobsCommand::clientOnly)
                        .then(Commands.argument("delay", StringArgumentType.word()).executes(MineMobsCommand::clientOnly)))
                .then(Commands.literal("settings")
                        .executes(MineMobsCommand::clientOnly)
                        .then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                                .executes(MineMobsCommand::clientOnly))));
    }

    private static int clientOnly(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(Component.literal("This command needs Minable Mobs installed on your client"));
        return 0;
    }

    private static int set(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        MineMobsConfig.get().enabled = enabled;
        MineMobsConfig.save();
        SettingsSync.sendAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("Mob mining turned " + (enabled ? "on" : "off")), true);
        return 1;
    }
}
