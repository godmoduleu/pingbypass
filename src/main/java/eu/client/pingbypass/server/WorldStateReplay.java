package eu.client.pingbypass.server;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Replays the current world state from the HeadlessMC proxy to the client.
 * Modeled after PingBypass's JoinWorldService — sends packets in the exact
 * order that a vanilla server would during PlayerList#placeNewPlayer.
 *
 * Returns a negative teleport ID that the client must confirm before the
 * proxy starts forwarding live S2C packets.
 */
public class WorldStateReplay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldStateReplay.class);
    private static final Random RANDOM = new Random();

    /**
     * Replays the full world state. Returns the initialTeleportId that the
     * client will send back in a TeleportConfirmC2SPacket.
     */
    public static int replay(ClientConnection toClient) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();

        if (player == null || world == null || handler == null) {
            LOGGER.error("Cannot replay world state: player/world/handler is null");
            return 0;
        }

        LOGGER.info("Replaying world state to client...");
        long start = System.currentTimeMillis();

        // Generate a negative teleport ID (vanilla never uses negative IDs)
        int initialTeleportId = -Math.abs(RANDOM.nextInt());
        if (initialTeleportId == 0) initialTeleportId = -1;

        GameMode gameMode = mc.interactionManager != null
                ? mc.interactionManager.getCurrentGameMode() : GameMode.SURVIVAL;
        GameMode prevGameMode = mc.interactionManager != null
                ? mc.interactionManager.getPreviousGameMode() : null;

        // 1. GameJoin
        Set<RegistryKey<World>> dimensionIds = handler.getWorldKeys();
        CommonPlayerSpawnInfo spawnInfo = createSpawnInfo(world, gameMode, prevGameMode, player);

        send(toClient, new GameJoinS2CPacket(
                player.getId(), world.getLevelProperties().isHardcore(), dimensionIds,
                1, 16, 16,
                false, true, false, spawnInfo, false));

        // 2. Respawn to clear lobby world state (clears view entity, prevents falling)
        send(toClient, new PlayerRespawnS2CPacket(spawnInfo, (byte) 0));

        // 3. Difficulty
        send(toClient, new DifficultyS2CPacket(world.getLevelProperties().getDifficulty(),
                world.getLevelProperties().isDifficultyLocked()));

        // 4. Abilities + slot
        send(toClient, new PlayerAbilitiesS2CPacket(player.getAbilities()));
        send(toClient, new UpdateSelectedSlotS2CPacket(player.getInventory().selectedSlot));

        // 5. Player list (must come before chunks so skins load)
        sendPlayerInfo(toClient, handler);

        // 6. Level info: world border, time, spawn, weather
        sendLevelInfo(toClient, world);

        // 7. Signal chunk loading start
        send(toClient, new GameStateChangeS2CPacket(GameStateChangeS2CPacket.INITIAL_CHUNKS_COMING, 0.0f));

        // 8. Chunks
        sendChunks(toClient, player, world);

        // 9. Health, food, experience
        send(toClient, new HealthUpdateS2CPacket(player.getHealth(),
                player.getHungerManager().getFoodLevel(),
                player.getHungerManager().getSaturationLevel()));
        send(toClient, new ExperienceBarUpdateS2CPacket(player.experienceProgress,
                player.totalExperience, player.experienceLevel));

        // 10. Inventory (full container contents)
        send(toClient, new InventoryS2CPacket(player.playerScreenHandler.syncId,
                player.playerScreenHandler.nextRevision(),
                player.playerScreenHandler.getStacks(),
                player.playerScreenHandler.getCursorStack()));

        // 11. Entities
        sendEntities(toClient, world, player);

        // 12. Initial teleport with our negative ID — client must confirm this
        send(toClient, new PlayerPositionLookS2CPacket(initialTeleportId,
                new PlayerPosition(player.getPos(), Vec3d.ZERO, player.getYaw(), player.getPitch()),
                Set.of()));

        // 13. Motion
        send(toClient, new EntityVelocityUpdateS2CPacket(player));

        long elapsed = System.currentTimeMillis() - start;
        LOGGER.info("World state replay completed in {}ms (teleportId={})", elapsed, initialTeleportId);
        return initialTeleportId;
    }

    private static void sendLevelInfo(ClientConnection toClient, ClientWorld world) {
        send(toClient, new WorldBorderInitializeS2CPacket(world.getWorldBorder()));
        send(toClient, new WorldTimeUpdateS2CPacket(world.getTime(), world.getTimeOfDay(), true));
        send(toClient, new PlayerSpawnPositionS2CPacket(world.getSpawnPos(), world.getSpawnAngle()));

        if (world.isRaining()) {
            send(toClient, new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0.0f));
            send(toClient, new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED,
                    world.getRainGradient(1.0f)));
            send(toClient, new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED,
                    world.getThunderGradient(1.0f)));
        }
    }

    private static void sendPlayerInfo(ClientConnection toClient, ClientPlayNetworkHandler handler) {
        var playerList = handler.getPlayerList();
        if (playerList.isEmpty()) return;

        var entries = new ArrayList<PlayerListS2CPacket.Entry>();
        for (var info : playerList) {
            entries.add(new PlayerListS2CPacket.Entry(
                    info.getProfile().getId(),
                    info.getProfile(),
                    handler.getListedPlayerListEntries().contains(info),
                    info.getLatency(),
                    info.getGameMode(),
                    info.getDisplayName(),
                    false, 0, null));
        }

        EnumSet<PlayerListS2CPacket.Action> actions = EnumSet.of(
                PlayerListS2CPacket.Action.ADD_PLAYER,
                PlayerListS2CPacket.Action.UPDATE_LISTED,
                PlayerListS2CPacket.Action.UPDATE_GAME_MODE,
                PlayerListS2CPacket.Action.UPDATE_LATENCY,
                PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME);

        // Use mixin accessor to set private final fields
        var packet = new PlayerListS2CPacket(
                EnumSet.of(PlayerListS2CPacket.Action.ADD_PLAYER),
                Collections.emptyList());
        ((eu.client.mixins.accessors.PlayerListS2CPacketAccessor) packet).setActions(actions);
        ((eu.client.mixins.accessors.PlayerListS2CPacketAccessor) packet).setEntries(entries);

        send(toClient, packet);
        LOGGER.info("Sent {} player list entries to client", entries.size());
    }

    private static CommonPlayerSpawnInfo createSpawnInfo(ClientWorld world, GameMode gameMode,
                                                         GameMode prevGameMode, ClientPlayerEntity player) {
        return new CommonPlayerSpawnInfo(
                world.getDimensionEntry(),
                world.getRegistryKey(),
                0L,
                gameMode, prevGameMode,
                world.isDebugWorld(),
                world.getLevelProperties() instanceof ClientWorld.Properties props && isFlat(props),
                player.getLastDeathPos(),
                player.getPortalCooldown(),
                world.getSeaLevel());
    }

    private static boolean isFlat(ClientWorld.Properties props) {
        // getSkyDarknessHeight requires a non-null HeightLimitView when flatWorld=true,
        // so use getHorizonShadingRatio() which doesn't need any parameter
        return props.getHorizonShadingRatio() >= 1.0F;
    }

    private static void sendEntities(ClientConnection toClient, ClientWorld world, ClientPlayerEntity localPlayer) {
        int count = 0;
        for (Entity entity : world.getEntities()) {
            if (entity == localPlayer) continue;

            try {
                // Send spawn packet using the full constructor
                send(toClient, new EntitySpawnS2CPacket(
                        entity.getId(), entity.getUuid(),
                        entity.getX(), entity.getY(), entity.getZ(),
                        entity.getPitch(), entity.getYaw(),
                        entity.getType(), 0,
                        entity.getVelocity(), entity.getHeadYaw()));

                // Send tracked data (metadata)
                List<DataTracker.SerializedEntry<?>> entries = entity.getDataTracker().getChangedEntries();
                if (entries != null && !entries.isEmpty()) {
                    send(toClient, new EntityTrackerUpdateS2CPacket(entity.getId(), entries));
                }

                // Send velocity
                send(toClient, new EntityVelocityUpdateS2CPacket(entity));

                // Send head yaw for living entities
                if (entity instanceof LivingEntity living) {
                    send(toClient, new EntitySetHeadYawS2CPacket(entity, (byte) (living.getHeadYaw() * 256.0f / 360.0f)));

                    // Send equipment
                    var equipment = new ArrayList<com.mojang.datafixers.util.Pair<net.minecraft.entity.EquipmentSlot, net.minecraft.item.ItemStack>>();
                    for (net.minecraft.entity.EquipmentSlot slot : net.minecraft.entity.EquipmentSlot.values()) {
                        var stack = living.getEquippedStack(slot);
                        if (!stack.isEmpty()) {
                            equipment.add(new com.mojang.datafixers.util.Pair<>(slot, stack));
                        }
                    }
                    if (!equipment.isEmpty()) {
                        send(toClient, new EntityEquipmentUpdateS2CPacket(entity.getId(), equipment));
                    }
                }

                count++;
            } catch (Exception e) {
                LOGGER.warn("Failed to send entity {} (ID: {})", entity.getType().getUntranslatedName(), entity.getId(), e);
            }
        }
        LOGGER.info("Sent {} entities to client", count);
    }

    private static void sendChunks(ClientConnection toClient, ClientPlayerEntity player, ClientWorld world) {
        int cx = player.getChunkPos().x;
        int cz = player.getChunkPos().z;

        // Chunk center must come before chunk data
        send(toClient, new ChunkRenderDistanceCenterS2CPacket(cx, cz));

        int radius = Math.min(MinecraftClient.getInstance().options.getViewDistance().getValue(), 8);
        int sent = 0;
        for (int z = cz - radius; z <= cz + radius; z++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                var chunk = world.getChunkManager().getWorldChunk(x, z);
                if (chunk != null) {
                    send(toClient, new ChunkDataS2CPacket(chunk, world.getLightingProvider(), null, null));
                    sent++;
                }
            }
        }
        LOGGER.info("Sent {} chunks to client", sent);
    }

    private static void send(ClientConnection connection, Packet<?> packet) {
        if (connection.isOpen()) {
            connection.send(packet);
        }
    }
}
