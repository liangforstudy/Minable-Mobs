package io.github.liangforstudy.minemobs.config;

import io.github.liangforstudy.minemobs.MineMobs;
import io.github.liangforstudy.minemobs.network.ServerSettingsPayload;
import io.github.liangforstudy.minemobs.network.UpdateSettingsPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Keeps every client's copy of the server settings current, and applies edits from the settings screen. */
public final class SettingsSync {
    private SettingsSync() {
    }

    /** Ops, and the owner of a singleplayer world (even with cheats off). */
    public static boolean canEdit(ServerPlayer player) {
        return player.hasPermissions(2) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    public static void send(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, ServerSettingsPayload.TYPE)) {
            ServerPlayNetworking.send(player, new ServerSettingsPayload(MineMobsConfig.get().toJson(), canEdit(player)));
        }
    }

    public static void sendAll(MinecraftServer server) {
        for (ServerPlayer player : PlayerLookup.all(server)) send(player);
    }

    public static void handleUpdate(UpdateSettingsPayload payload, ServerPlayer player) {
        if (!canEdit(player)) {
            send(player); // put their screen back to the real values
            return;
        }
        try {
            MineMobsConfig.set(MineMobsConfig.fromJson(payload.json()));
            MineMobs.LOGGER.info("{} changed the Minable Mobs settings", player.getGameProfile().getName());
            sendAll(player.server);
        } catch (RuntimeException e) {
            player.sendSystemMessage(Component.literal("Minable Mobs: those settings couldn't be read (" + e.getMessage() + ")"));
            send(player);
        }
    }
}
