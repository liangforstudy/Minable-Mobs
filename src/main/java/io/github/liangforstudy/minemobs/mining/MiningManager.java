package io.github.liangforstudy.minemobs.mining;

import io.github.liangforstudy.minemobs.MineMobs;
import io.github.liangforstudy.minemobs.config.MineMobsConfig;
import io.github.liangforstudy.minemobs.network.MineEntityPayload;
import io.github.liangforstudy.minemobs.network.MiningProgressPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-owned mining state, like vanilla block breaking: the client only says start / still mining /
 * stop, and the server advances progress every tick, freezes the target, and breaks it.
 */
public final class MiningManager {
    /** A session ends if the client stops sending CONTINUE for this many ticks. */
    private static final int TIMEOUT_TICKS = 5;
    /** Same slack the server allows when attacking an entity. */
    private static final double REACH_BUFFER = 3.0;

    /** data/minemobs/damage_type/mined.json — "X was mined by Y". */
    private static final ResourceKey<DamageType> MINED = ResourceKey.create(Registries.DAMAGE_TYPE, MineMobs.id("mined"));

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Entities being mined by at least one player. In memory only, so nothing is ever saved frozen. */
    private static final Set<Entity> FROZEN = Collections.newSetFromMap(new WeakHashMap<>());

    private MiningManager() {
    }

    private static final class Session {
        final ServerPlayer player;
        final LivingEntity target;
        float progress;
        /** Last progress sent to clients, 0-99, or -1 before the first send. */
        int sentPercent = -1;
        int lastSignalTick;

        Session(ServerPlayer player, LivingEntity target, int now) {
            this.player = player;
            this.target = target;
            this.lastSignalTick = now;
        }
    }

    public static boolean isFrozen(Entity entity) {
        return !FROZEN.isEmpty() && FROZEN.contains(entity);
    }

    public static void handle(MineEntityPayload payload, ServerPlayer player) {
        int now = player.server.getTickCount();
        Session session = SESSIONS.get(player.getUUID());

        if (payload.action() == MineEntityPayload.Action.ABORT) {
            if (session != null && session.target.getId() == payload.entityId()) end(session);
            return;
        }

        if (session != null && session.target.getId() == payload.entityId()) {
            session.lastSignalTick = now;
            return;
        }

        // START, or a CONTINUE for a target we have no session for (treated as a start)
        if (session != null) end(session);
        if (!MineMobsConfig.get().enabled) return;
        Entity entity = player.serverLevel().getEntity(payload.entityId());
        if (entity == null || !canKeepMining(player, entity)) return;
        LivingEntity target = (LivingEntity) entity;

        if (player.getAbilities().instabuild) {
            breakEntity(player, target);
            return;
        }

        session = new Session(player, target, now);
        SESSIONS.put(player.getUUID(), session);
        freeze(target);
        // Time the client spent telling a tap from a hold counts, up to the server's cap
        int credit = Math.max(0, Math.min(payload.heldTicks(), MineMobsConfig.get().maxHoldCreditTicks));
        session.progress = credit * Mining.progressPerTick(player, target);
        tickSession(session); // this tick counts too, like vanilla's startDestroyBlock
    }

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        int now = server.getTickCount();
        for (Session session : SESSIONS.values().toArray(Session[]::new)) {
            if (SESSIONS.get(session.player.getUUID()) != session) continue; // ended this tick
            if (now - session.lastSignalTick > TIMEOUT_TICKS
                    || !MineMobsConfig.get().enabled
                    || session.player.isRemoved()
                    || !canKeepMining(session.player, session.target)) {
                end(session);
                continue;
            }
            tickSession(session);
        }
    }

    public static void onDisconnect(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null) end(session);
    }

    /** A player came into range of an entity that's already cracked. */
    public static void onStartTracking(Entity entity, ServerPlayer player) {
        if (!isFrozen(entity)) return;
        ServerPlayNetworking.send(player, new MiningProgressPayload(entity.getId(), percentOf(entity)));
    }

    /** Server stopped: drop everything without touching entities (they're being unloaded anyway). */
    public static void clear() {
        SESSIONS.clear();
        FROZEN.clear();
    }

    private static boolean canKeepMining(ServerPlayer player, Entity target) {
        return target.level() == player.level()
                && !target.isRemoved()
                && Mining.canMine(player, target)
                && player.canInteractWithEntity(target.getBoundingBox(), REACH_BUFFER);
    }

    private static void tickSession(Session session) {
        session.progress += Mining.progressPerTick(session.player, session.target);
        if (session.progress >= 1.0F) {
            ServerPlayer player = session.player;
            LivingEntity target = session.target;
            end(session);
            breakEntity(player, target);
            return;
        }
        int percent = Math.min(99, (int) (session.progress * 100.0F));
        // Only send when something visible changes: the crack stage (tenths) or a golem's crack level (quarters)
        if (session.sentPercent < 0 || percent / 10 != session.sentPercent / 10 || percent / 25 != session.sentPercent / 25) {
            session.sentPercent = percent;
            broadcastProgress(session.target);
        }
    }

    private static void end(Session session) {
        SESSIONS.remove(session.player.getUUID());
        LivingEntity target = session.target;
        if (SESSIONS.values().stream().noneMatch(s -> s.target == target)) {
            FROZEN.remove(target);
        }
        if (!target.isRemoved()) broadcastProgress(target);
    }

    private static void freeze(LivingEntity target) {
        // Players move on their own client, so the server can't hold them still
        if (target instanceof Player) return;
        if (FROZEN.add(target)) {
            target.setDeltaMovement(target.getDeltaMovement().multiply(0.0, 0.0, 0.0));
            target.hurtMarked = true; // push the zeroed velocity to clients
        }
    }

    /** The furthest-along player's progress when several mine the same entity, or -1. */
    private static int percentOf(Entity target) {
        int percent = -1;
        for (Session session : SESSIONS.values()) {
            if (session.target == target) percent = Math.max(percent, session.sentPercent);
        }
        return percent;
    }

    private static void broadcastProgress(LivingEntity target) {
        MiningProgressPayload payload = new MiningProgressPayload(target.getId(), percentOf(target));
        for (ServerPlayer player : PlayerLookup.tracking(target)) {
            ServerPlayNetworking.send(player, payload);
        }
        // A mined player sees their own cracks in third person
        if (target instanceof ServerPlayer self) ServerPlayNetworking.send(self, payload);
    }

    private static void breakEntity(ServerPlayer player, LivingEntity target) {
        // Riders and mounts stay in the world; only the mined entity goes
        target.ejectPassengers();
        target.stopRiding();

        SoundType sound = SoundType.STONE;
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound.getBreakSound(),
                SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

        if (!player.getAbilities().instabuild) {
            player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        if (target instanceof Player) {
            // Players can't be removed from the world, so mining one kills them (bypasses armor and totems, like /kill)
            target.hurt(new DamageSource(player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MINED), player), Float.MAX_VALUE);
            return;
        }
        // TODO: drop a mob_figure holding the entity's NBT (next step)
        target.discard();
    }
}
