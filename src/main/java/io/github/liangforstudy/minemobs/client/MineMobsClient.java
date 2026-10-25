package io.github.liangforstudy.minemobs.client;

import io.github.liangforstudy.minemobs.MineMobs;
import io.github.liangforstudy.minemobs.client.overlay.BreakOverlayMappings;
import io.github.liangforstudy.minemobs.config.MineMobsConfig;
import io.github.liangforstudy.minemobs.network.MiningProgressPayload;
import io.github.liangforstudy.minemobs.network.ServerSettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public class MineMobsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MineMobsClientConfig.load();

        // Fabric runs play payload handlers on the render thread
        ClientPlayNetworking.registerGlobalReceiver(ServerSettingsPayload.TYPE, (payload, context) -> {
            try {
                ClientMiningState.setServerSettings(MineMobsConfig.fromJson(payload.json()), payload.canEdit());
            } catch (RuntimeException e) {
                MineMobs.LOGGER.error("Couldn't read the server's Minable Mobs settings", e);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(MiningProgressPayload.TYPE,
                (payload, context) -> ClientMiningState.setProgress(payload.entityId(), payload.percent()));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientMiningState.reset();
            ClientMiningController.reset();
        });

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new BreakOverlayMappings());
        ClientCommandRegistrationCallback.EVENT.register(MineMobsClientCommand::register);
    }
}
