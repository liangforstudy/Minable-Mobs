package io.github.liangforstudy.minemobs.network;

import io.github.liangforstudy.minemobs.MineMobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the server's settings (minemobs.json) and whether this player may change them.
 * Sent on join and after every change; drives the client's on/off state and the settings screen.
 */
public record ServerSettingsPayload(String json, boolean canEdit) implements CustomPacketPayload {
    public static final int MAX_LENGTH = 1 << 18;
    public static final Type<ServerSettingsPayload> TYPE = new Type<>(MineMobs.id("server_settings"));
    public static final StreamCodec<FriendlyByteBuf, ServerSettingsPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.json, MAX_LENGTH);
                buf.writeBoolean(payload.canEdit);
            },
            buf -> new ServerSettingsPayload(buf.readUtf(MAX_LENGTH), buf.readBoolean()));

    @Override
    public Type<ServerSettingsPayload> type() {
        return TYPE;
    }
}
