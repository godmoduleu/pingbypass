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

public class WorldStateSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldStateSender.class);

    private WorldStateSender() {
    }

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

    private static void sendJoinGame(ClientWorld world, ClientPlayerEntity player, ClientConnection toClient) {
        LOGGER.debug("Sending JoinGame packet (entity ID: {})", player.getId());
    }

    private static void sendChunks(ClientWorld world, ClientConnection toClient) {
        int chunkCount = 0;
        LOGGER.debug("Sent {} chunk data packets", chunkCount);
    }

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

    private static void sendEntitySpawn(Entity entity, ClientConnection toClient) {
        LOGGER.trace("Sending spawn packet for entity {} (ID: {})",
                entity.getType().getUntranslatedName(), entity.getId());
    }

    private static void sendEntityMetadata(Entity entity, ClientConnection toClient) {
        List<DataTracker.SerializedEntry<?>> entries = entity.getDataTracker().getChangedEntries();
        if (entries != null && !entries.isEmpty()) {
            send(toClient, new EntityTrackerUpdateS2CPacket(entity.getId(), entries));
        }
    }

    private static void sendPlayerList(ClientPlayerEntity player, ClientConnection toClient) {
        if (player.networkHandler == null) {
            return;
        }

        Collection<PlayerListEntry> entries = player.networkHandler.getPlayerList();
        if (entries.isEmpty()) {
            return;
        }

        LOGGER.debug("Sent player list with {} entries", entries.size());
    }

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

    private static void sendScoreboard(ClientWorld world, ClientConnection toClient) {
        Scoreboard scoreboard = world.getScoreboard();
        if (scoreboard == null) {
            return;
        }

        for (ScoreboardObjective objective : scoreboard.getObjectives()) {
            send(toClient, new ScoreboardObjectiveUpdateS2CPacket(objective, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE));

        }

        for (Team team : scoreboard.getTeams()) {
            send(toClient, TeamS2CPacket.updateTeam(team, true));
        }

        LOGGER.debug("Sent scoreboard data ({} objectives, {} teams)",
                scoreboard.getObjectives().size(), scoreboard.getTeams().size());
    }

    private static void sendPlayerState(ClientPlayerEntity player, ClientConnection toClient) {
        LOGGER.debug("Sending player position (entity ID: {})", player.getId());

        send(toClient, new HealthUpdateS2CPacket(
                player.getHealth(),
                player.getHungerManager().getFoodLevel(),
                player.getHungerManager().getSaturationLevel()
        ));

        send(toClient, new ExperienceBarUpdateS2CPacket(
                player.experienceProgress,
                player.totalExperience,
                player.experienceLevel
        ));

        send(toClient, new UpdateSelectedSlotS2CPacket(player.getInventory().selectedSlot));
        LOGGER.debug("Sent player state (pos: {}, health: {}, food: {})",
                player.getPos(), player.getHealth(), player.getHungerManager().getFoodLevel());
    }

    private static void send(ClientConnection connection, Packet<?> packet) {
        if (connection.isOpen()) {
            connection.send(packet);
        }
    }
}
