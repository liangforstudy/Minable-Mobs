package io.github.liangforstudy.minemobs.network;

import io.github.liangforstudy.minemobs.MineMobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> nearby clients: how far an entity has been mined, 0-99 percent, or -1 once nobody is mining
 * it. Clients turn it into a crack stage (0-9) or, for iron golems, a crackiness level.
 */
public record MiningProgressPayload(int entityId, int percent) implements CustomPacketPayload {
    public static final Type<MiningProgressPayload> TYPE = new Type<>(MineMobs.id("mining_progress"));
    public static final StreamCodec<FriendlyByteBuf, MiningProgressPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId);
                buf.writeByte(payload.percent);
            },
            buf -> new MiningProgressPayload(buf.readVarInt(), buf.readByte()));

    @Override
    public Type<MiningProgressPayload> type() {
        return TYPE;
    }
}
