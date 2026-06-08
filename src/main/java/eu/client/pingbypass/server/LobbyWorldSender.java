package eu.client.pingbypass.server;

import com.google.common.collect.Sets;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

public class LobbyWorldSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyWorldSender.class);

    private LobbyWorldSender() {
    }

    public static void sendLobbyWorld(ClientConnection toClient, DynamicRegistryManager registryManager) {
        LOGGER.info("Sending lobby world to client...");
        try {
            Registry<DimensionType> dimRegistry = registryManager.getOrThrow(RegistryKeys.DIMENSION_TYPE);
            RegistryEntry<DimensionType> overworldType = dimRegistry.getOrThrow(DimensionTypes.OVERWORLD);

            Set<RegistryKey<World>> dimensionIds = Sets.newHashSet(World.OVERWORLD, World.NETHER, World.END);

            CommonPlayerSpawnInfo spawnInfo = new CommonPlayerSpawnInfo(
                    overworldType, World.OVERWORLD, 0L, GameMode.SPECTATOR,
                    null, false, true, Optional.empty(), 0, 63);

            send(toClient, new GameJoinS2CPacket(
                    1337, false, dimensionIds, 1, 16, 16,
                    false, true, false, spawnInfo, false));

            send(toClient, new PlayerAbilitiesS2CPacket(createLobbyAbilities()));
            send(toClient, new UpdateSelectedSlotS2CPacket(0));
            send(toClient, new PlayerPositionLookS2CPacket(1,
                    new PlayerPosition(new Vec3d(0, 240, 0), Vec3d.ZERO, 0f, 0f),
                    Set.of()));

            send(toClient, new GameStateChangeS2CPacket(GameStateChangeS2CPacket.INITIAL_CHUNKS_COMING, 0.0f));

            LOGGER.info("Lobby world sent successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to send lobby world", e);
        }
    }

    private static void send(ClientConnection connection, Packet<?> packet) {
        if (connection.isOpen()) {
            connection.send(packet);
        }
    }

    private static PlayerAbilities createLobbyAbilities() {
        PlayerAbilities abilities = new PlayerAbilities();
        abilities.allowFlying = true;
        abilities.flying = true;
        abilities.invulnerable = true;
        return abilities;
    }
}
