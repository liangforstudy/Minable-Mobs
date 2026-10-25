package io.github.liangforstudy.minemobs.network;

import io.github.liangforstudy.minemobs.MineMobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: the player started, is still, or stopped mining an entity.
 *
 * @param heldTicks START only: how long attack was already held while the client waited to tell a tap
 *                  from a hold. The server counts it toward mining, up to its own cap.
 */
public record MineEntityPayload(int entityId, Action action, int heldTicks) implements CustomPacketPayload {
    public static final Type<MineEntityPayload> TYPE = new Type<>(MineMobs.id("mine_entity"));
    public static final StreamCodec<FriendlyByteBuf, MineEntityPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId);
                buf.writeEnum(payload.action);
                buf.writeVarInt(payload.heldTicks);
            },
            buf -> new MineEntityPayload(buf.readVarInt(), buf.readEnum(Action.class), buf.readVarInt()));

    public enum Action { START, CONTINUE, ABORT }

    @Override
    public Type<MineEntityPayload> type() {
        return TYPE;
    }
}
