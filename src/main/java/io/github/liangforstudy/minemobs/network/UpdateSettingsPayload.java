package io.github.liangforstudy.minemobs.network;

import io.github.liangforstudy.minemobs.MineMobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: new server settings from the settings screen. Only ops (or the singleplayer owner) are listened to. */
public record UpdateSettingsPayload(String json) implements CustomPacketPayload {
    public static final Type<UpdateSettingsPayload> TYPE = new Type<>(MineMobs.id("update_settings"));
    public static final StreamCodec<FriendlyByteBuf, UpdateSettingsPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.json, ServerSettingsPayload.MAX_LENGTH),
            buf -> new UpdateSettingsPayload(buf.readUtf(ServerSettingsPayload.MAX_LENGTH)));

    @Override
    public Type<UpdateSettingsPayload> type() {
        return TYPE;
    }
}
