package io.github.liangforstudy.minemobs;

import io.github.liangforstudy.minemobs.config.MineMobsConfig;
import io.github.liangforstudy.minemobs.config.SettingsSync;
import io.github.liangforstudy.minemobs.mining.MiningManager;
import io.github.liangforstudy.minemobs.network.MineEntityPayload;
import io.github.liangforstudy.minemobs.network.MiningProgressPayload;
import io.github.liangforstudy.minemobs.network.ServerSettingsPayload;
import io.github.liangforstudy.minemobs.network.UpdateSettingsPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MineMobs implements ModInitializer {
    public static final String MOD_ID = "minemobs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        MineMobsConfig.load();

        PayloadTypeRegistry.playC2S().register(MineEntityPayload.TYPE, MineEntityPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MiningProgressPayload.TYPE, MiningProgressPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateSettingsPayload.TYPE, UpdateSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerSettingsPayload.TYPE, ServerSettingsPayload.CODEC);

        // Fabric runs play payload handlers on the server thread
        ServerPlayNetworking.registerGlobalReceiver(MineEntityPayload.TYPE,
                (payload, context) -> MiningManager.handle(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(UpdateSettingsPayload.TYPE,
                (payload, context) -> SettingsSync.handleUpdate(payload, context.player()));

        ServerTickEvents.END_SERVER_TICK.register(MiningManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SettingsSync.send(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> MiningManager.onDisconnect(handler.player));
        EntityTrackingEvents.START_TRACKING.register(MiningManager::onStartTracking);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MiningManager.clear());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                MineMobsCommand.register(dispatcher, registryAccess));
    }
}
