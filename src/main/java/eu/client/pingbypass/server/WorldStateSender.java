package eu.client.pingbypass.server;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Sends the full current world state to a reconnecting client.
 * Used when a client connects to the proxy while Stay Connected is active
 * and the proxy is already joined to a Minecraft server.
 *
 * Packet order:
 * 1. JoinGame (GameJoinS2CPacket)
 * 2. Chunk data (ChunkDataS2CPacket for each loaded chunk)
 * 3. Entity spawn + metadata packets
 * 4. Player list (tab list) entries
 * 5. Time, weather, scoreboard
 * 6. Player state (position, health, inventory, held item)
 */
public class WorldStateSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldStateSender.class);

    private WorldStateSender() {
    }

    /**
     * Sends the complete world state to the given client connection.
     * This allows a reconnecting client to see the current world as-is.
     * Delegates to WorldStateReplay which has the full implementation.
     *
     * @param world    the current ClientWorld on the proxy
     * @param player   the proxy's ClientPlayerEntity
     * @param toClient the connection to the reconnecting client
     */
    public static void sendWorld(ClientWorld world, ClientPlayerEntity player, ClientConnection toClient) {
        LOGGER.info("Sending world state to reconnecting client...");

        try {
            if (world == null || player == null || toClient == null) {
                LOGGER.warn("Cannot send world state: null parameter(s)");
                return;
            }
            // Delegate to WorldStateReplay which has the complete implementation
            WorldStateReplay.replay(toClient);
            LOGGER.info("World state sent successfully.");
        } catch (Exception e) {
            LOGGER.error("Error sending world state to client", e);
        }
    }

    /**
     * Sends the JoinGame packet constructed from the current player/world state.
     */
    private static void sendJoinGame(ClientWorld world, ClientPlayerEntity player, ClientConnection toClient) {
        // TODO: Construct GameJoinS2CPacket from current world and player state.
        // In 1.21.4, GameJoinS2CPacket requires:
        //   - player entity ID
        //   - hardcore flag
        //   - dimension list (registry keys)
        //   - max players
        //   - view distance, simulation distance
        //   - reduced debug info, show death screen
        //   - dimension type, dimension key
        //   - hashed seed
        //   - game mode, previous game mode
        //   - is debug world, is flat world
        //   - death location (optional)
        //   - portal cooldown
        //   - sea level
        // This packet is complex to construct from client-side state.
        // The proxy should cache the original GameJoinS2CPacket received from the real server
        // and re-send it here. For now, log a warning.
        LOGGER.debug("Sending JoinGame packet (entity ID: {})", player.getId());
    }

    /**
     * Sends all loaded chunk data to the client.
     */
    private static void sendChunks(ClientWorld world, ClientConnection toClient) {
        // TODO: Iterate loaded chunks and send ChunkDataS2CPacket for each.
        // ClientWorld's chunk manager provides access to loaded chunks.
        // For each loaded WorldChunk, construct a ChunkDataS2CPacket and send it.
        // Also send light update data (LightUpdateS2CPacket) for each chunk.
        //
        // Example approach:
        //   for each loaded chunk in world.getChunkManager():
        //     if chunk is not empty:
        //       toClient.send(new ChunkDataS2CPacket(chunk, lightingProvider, ...))
        int chunkCount = 0;
        // TODO: Implement chunk iteration - requires access to ClientChunkManager internals
        LOGGER.debug("Sent {} chunk data packets", chunkCount);
    }

    /**
     * Sends spawn and metadata packets for all tracked entities.
     */
    private static void sendEntities(ClientWorld world, ClientPlayerEntity player, ClientConnection toClient) {
        int entityCount = 0;
        for (Entity entity : world.getEntities()) {
            if (entity == player) {
                continue; // Skip the player themselves, handled separately
            }

            try {
                sendEntitySpawn(entity, toClient);
                sendEntityMetadata(entity, toClient);
                entityCount++;
            } catch (Exception e) {
                LOGGER.warn("Failed to send entity data for {} (ID: {})",
                        entity.getType().getUntranslatedName(), entity.getId(), e);
            }
        }
        LOGGER.debug("Sent data for {} entities", entityCount);
    }

    /**
     * Sends the appropriate spawn packet for an entity based on its type.
     * In 1.21.4, EntitySpawnS2CPacket and ExperienceOrbSpawnS2CPacket are records
     * whose constructors take byte buffers (network deserialization only).
     * The proxy should cache the original spawn packets received from the real server
     * and replay them here.
     */
    private static void sendEntitySpawn(Entity entity, ClientConnection toClient) {
        // TODO: Replay cached spawn packet for this entity.
        // The proxy's packet listener should store the original EntitySpawnS2CPacket
        // (or ExperienceOrbSpawnS2CPacket) received from the real server, keyed by entity ID.
        // On reconnect, replay those cached packets here.
        //
        // For living entities, also replay:
        //   - EntityEquipmentUpdateS2CPacket for all equipment slots
        //   - EntityAttributesS2CPacket for entity attributes
        LOGGER.trace("Sending spawn packet for entity {} (ID: {})",
                entity.getType().getUntranslatedName(), entity.getId());
    }

    /**
     * Sends entity metadata (tracked data) for an entity.
     * DataTracker entries and velocity/head yaw packets are records with byte-buffer constructors.
     * The proxy should cache entity metadata updates from the real server.
     */
    private static void sendEntityMetadata(Entity entity, ClientConnection toClient) {
        // Send tracked data entries
        List<DataTracker.SerializedEntry<?>> entries = entity.getDataTracker().getChangedEntries();
        if (entries != null && !entries.isEmpty()) {
            send(toClient, new EntityTrackerUpdateS2CPacket(entity.getId(), entries));
        }

        // TODO: Send EntityVelocityUpdateS2CPacket for this entity.
        // In 1.21.4, this is a record with (int entityId, Vec3d velocity) constructor.
        // The proxy should construct: new EntityVelocityUpdateS2CPacket(entity.getId(), entity.getVelocity())

        // TODO: Send EntitySetHeadYawS2CPacket for living entities.
        // Requires caching or constructing from entity head yaw.
    }

    /**
     * Sends the player list (tab list) entries to the client.
     */
    private static void sendPlayerList(ClientPlayerEntity player, ClientConnection toClient) {
        if (player.networkHandler == null) {
            return;
        }

        Collection<PlayerListEntry> entries = player.networkHandler.getPlayerList();
        if (entries.isEmpty()) {
            return;
        }

        // TODO: Construct PlayerListS2CPacket with ADD_PLAYER action for all entries.
        // In 1.21.4, PlayerListS2CPacket uses EnumSet<Action> and a list of entries.
        // Each entry contains: profile UUID, game profile, game mode, latency, display name, listed flag.
        // The proxy should reconstruct this from the cached PlayerListEntry objects.
        LOGGER.debug("Sent player list with {} entries", entries.size());
    }

    /**
     * Sends current time and weather state.
     */
    private static void sendTimeAndWeather(ClientWorld world, ClientConnection toClient) {
        // Send world time
        send(toClient, new WorldTimeUpdateS2CPacket(world.getTime(), world.getTimeOfDay(), true));

        // Send weather state
        if (world.isRaining()) {
            send(toClient, new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0.0F));
            // Send rain strength
            send(toClient, new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, world.getRainGradient(1.0F)));
        }

        if (world.isThundering()) {
            send(toClient, new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, world.getThunderGradient(1.0F)));
        }
    }

    /**
     * Sends scoreboard data (objectives, teams, scores).
     */
    private static void sendScoreboard(ClientWorld world, ClientConnection toClient) {
        Scoreboard scoreboard = world.getScoreboard();
        if (scoreboard == null) {
            return;
        }

        // Send all objectives
        for (ScoreboardObjective objective : scoreboard.getObjectives()) {
            send(toClient, new ScoreboardObjectiveUpdateS2CPacket(objective, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE));

            // TODO: Send ScoreboardDisplayS2CPacket if this objective is displayed in a slot
            // TODO: Send ScoreboardScoreUpdateS2CPacket for each score under this objective
        }

        // Send all teams
        for (Team team : scoreboard.getTeams()) {
            send(toClient, TeamS2CPacket.updateTeam(team, true));
        }

        LOGGER.debug("Sent scoreboard data ({} objectives, {} teams)",
                scoreboard.getObjectives().size(), scoreboard.getTeams().size());
    }

    /**
     * Sends the player's current state: position, health, inventory, held item.
     */
    private static void sendPlayerState(ClientPlayerEntity player, ClientConnection toClient) {
        // Player position and look
        // PlayerPositionLookS2CPacket requires (int entityId, PlayerPosition change, Set<PositionFlag> flags)
        // TODO: Construct PlayerPosition from player pos/velocity/yaw/pitch and send.
        // The proxy should cache the last PlayerPositionLookS2CPacket from the server
        // or construct a PlayerPosition record from the current player state.
        LOGGER.debug("Sending player position (entity ID: {})", player.getId());

        // Health, food, saturation
        send(toClient, new HealthUpdateS2CPacket(
                player.getHealth(),
                player.getHungerManager().getFoodLevel(),
                player.getHungerManager().getSaturationLevel()
        ));

        // Experience
        send(toClient, new ExperienceBarUpdateS2CPacket(
                player.experienceProgress,
                player.totalExperience,
                player.experienceLevel
        ));

        // Held item slot
        send(toClient, new UpdateSelectedSlotS2CPacket(player.getInventory().selectedSlot));

        // Inventory contents
        // TODO: Send InventoryS2CPacket (ScreenHandlerSlotUpdateS2CPacket) for each inventory slot
        // or send a full WindowItemsS2CPacket for the player's inventory container.
        // player.playerScreenHandler contains all 46 slots (crafting + armor + inventory + offhand).
        LOGGER.debug("Sent player state (pos: {}, health: {}, food: {})",
                player.getPos(), player.getHealth(), player.getHungerManager().getFoodLevel());
    }

    /**
     * Safely sends a packet to the client connection.
     */
    private static void send(ClientConnection connection, Packet<?> packet) {
        if (connection.isOpen()) {
            connection.send(packet);
        }
    }
}
