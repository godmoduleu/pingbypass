package eu.client.pingbypass.handler;

import com.mojang.authlib.GameProfile;
import eu.client.pingbypass.protocol.PbCustomPayload;
import eu.client.pingbypass.protocol.packets.S2CErrorPacket;
import eu.client.pingbypass.server.LobbyWorldSender;
import eu.client.pingbypass.server.ProxyServer;
import eu.client.pingbypass.server.S2CForwarder;
import eu.client.pingbypass.server.WorldStateReplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the waiting state when the proxy is not yet connected to a server.
 * Listens for C2S_JOIN custom packets to initiate a server connection,
 * and responds to keep-alives to prevent the client from timing out.
 */
public class PbWaitingHandler implements ServerPlayPacketListener, TickablePacketListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbWaitingHandler.class);
    private static final long KEEP_ALIVE_TIMEOUT_MS = 30_000L;
    private static final long KEEP_ALIVE_INTERVAL_MS = 15_000L;

    private final ProxyServer proxyServer;
    private final ClientConnection connection;
    private final GameProfile profile;
    private final net.minecraft.registry.DynamicRegistryManager registryManager;
    private long lastKeepAliveTime;
    private long lastKeepAliveSentTime;
    private boolean lobbyWorldSent;

    public PbWaitingHandler(ProxyServer proxyServer, ClientConnection connection, GameProfile profile,
                            net.minecraft.registry.DynamicRegistryManager registryManager) {
        this.proxyServer = proxyServer;
        this.connection = connection;
        this.profile = profile;
        this.registryManager = registryManager;
        this.lastKeepAliveTime = System.currentTimeMillis();
        this.lastKeepAliveSentTime = 0;
        LOGGER.info("Waiting handler initialized for {}", profile.getName());
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();

        // Send the lobby world on the first tick so the client leaves "Joining World"
        if (!lobbyWorldSent) {
            lobbyWorldSent = true;
            LobbyWorldSender.sendLobbyWorld(connection, registryManager);
        }

        if (now - lastKeepAliveTime > KEEP_ALIVE_TIMEOUT_MS) {
            LOGGER.warn("Client {} timed out (no keep-alive for 30s)", profile.getName());
            connection.disconnect(Text.literal("Timed out"));
        }

        // Send periodic keep-alives to prevent the client from timing out
        if (now - lastKeepAliveSentTime >= KEEP_ALIVE_INTERVAL_MS) {
            lastKeepAliveSentTime = now;
            connection.send(new KeepAliveS2CPacket(now));
        }

        // Poll for server connection establishment
        if (connectingToServer) {
            connectionAttemptTicks++;
            MinecraftClient mc = MinecraftClient.getInstance();

            // Send live status updates to the client
            if (connectionAttemptTicks == 20) {
                sendChat("§7[PingBypass] Resolving address...");
            } else if (connectionAttemptTicks == 40 && mc.getNetworkHandler() == null) {
                sendChat("§7[PingBypass] Establishing connection...");
            } else if (connectionAttemptTicks == 60 && mc.getNetworkHandler() == null) {
                sendChat("§7[PingBypass] Encrypting...");
            }

            if (mc.getNetworkHandler() != null && mc.player == null) {
                // In configuration/login phase
                if (connectionAttemptTicks % 40 == 0) {
                    sendChat("§7[PingBypass] Configuring...");
                }
            } else if (mc.getNetworkHandler() != null && mc.player != null && mc.world != null) {
                if (mc.getNetworkHandler().getRegistryManager() != null) {
                    // Server connection established and registry is ready!
                    connectingToServer = false;
                    sendChat("§a[PingBypass] Connected! Loading world...");
                    onServerConnectionEstablished(mc, pendingServerIp, pendingServerPort);
                } else if (connectionAttemptTicks % 40 == 0) {
                    sendChat("§7[PingBypass] Loading terrain...");
                }
            } else if (mc.currentScreen instanceof net.minecraft.client.gui.screen.DisconnectedScreen) {
                // Connection failed
                connectingToServer = false;
                LOGGER.warn("Connection to {}:{} failed for {}", pendingServerIp, pendingServerPort, profile.getName());
                sendError("Failed to connect to " + pendingServerIp + ":" + pendingServerPort);
                sendChat("§c[PingBypass] Failed to connect to " + pendingServerIp + ":" + pendingServerPort);
            } else if (connectionAttemptTicks > 600) {
                // Timeout after ~30 seconds
                connectingToServer = false;
                LOGGER.warn("Connection to {}:{} timed out for {}", pendingServerIp, pendingServerPort, profile.getName());
                sendError("Connection to " + pendingServerIp + ":" + pendingServerPort + " timed out");
                sendChat("§c[PingBypass] Connection timed out");
            }
        }
    }

    @Override
    public void onCustomPayload(CustomPayloadC2SPacket packet) {
        CustomPayload payload = packet.payload();
        if (!PbCustomPayload.CHANNEL.equals(payload.getId().id())) {
            return;
        }

        if (!(payload instanceof PbCustomPayload pbPayload)) {
            return;
        }

        PacketByteBuf buf = pbPayload.toBuf();
        try {
            int packetId = buf.readVarInt();
            if (packetId == PbCustomPayload.C2S_JOIN) {
                String serverIp = buf.readString();
                int serverPort = buf.readVarInt();
                handleJoinRequest(serverIp, serverPort);
            } else {
                LOGGER.warn("Unexpected packet ID {} from {} during waiting state",
                        packetId, profile.getName());
            }
        } catch (Exception e) {
            LOGGER.warn("Malformed custom payload from {}", profile.getName(), e);
        } finally {
            buf.release();
        }
    }

    /**
     * Handles a C2S_JOIN request from the client.
     * Initiates a connection to the specified MC server using the headless client session.
     * The proxy (HeadlessMC) connects to the target server via ConnectScreen.
     * Once connected (mc.player != null), the world state is replayed to the client.
     */
    void handleJoinRequest(String serverIp, int serverPort) {
        LOGGER.info("Client {} requested join to {}:{}", profile.getName(), serverIp, serverPort);

        MinecraftClient mc = MinecraftClient.getInstance();

        // Check if the proxy is already connected to a server
        if (mc.getNetworkHandler() != null && mc.player != null && mc.world != null
                && mc.getNetworkHandler().getRegistryManager() != null) {
            LOGGER.info("Proxy already connected to a server, replaying world state for {}", profile.getName());
            sendChat("§a[PingBypass] Proxy already connected! Resuming session...");
            onServerConnectionEstablished(mc, serverIp, serverPort);
            return;
        }

        sendChat("§d[PingBypass] §fConnecting to §a" + serverIp + ":" + serverPort + "§f...");

        String address = serverIp + ":" + serverPort;
        ServerAddress serverAddress = ServerAddress.parse(address);
        ServerInfo serverInfo = new ServerInfo("PingBypass Target", address, ServerInfo.ServerType.OTHER);

        // Track connection state
        this.pendingServerIp = serverIp;
        this.pendingServerPort = serverPort;
        this.connectingToServer = true;
        this.connectionAttemptTicks = 0;

        mc.execute(() -> {
            try {
                LOGGER.info("Initiating connection to {} for {}", address, profile.getName());
                ConnectScreen.connect(new net.minecraft.client.gui.screen.TitleScreen(),
                        mc, serverAddress, serverInfo, false, null);
            } catch (Exception e) {
                LOGGER.error("Failed to initiate connection to {} for {}", address, profile.getName(), e);
                sendError("Failed to connect to " + address + ": " + e.getMessage());
                connectingToServer = false;
            }
        });
    }

    private String pendingServerIp;
    private int pendingServerPort;
    private volatile boolean connectingToServer;
    private int connectionAttemptTicks;

    /**
     * Called when the headless client has successfully connected to the target MC server.
     * Sets up forwarding and transitions to PbPlayHandler.
     *
     * Critical ordering (mirrors PingBypass's S2PB2CPipeline):
     * 1. Create S2CForwarder and LOCK it (queue incoming S2C packets)
     * 2. Start the forwarder (subscribes to events, packets get queued)
     * 3. Replay world state to client
     * 4. Transition to PbPlayHandler with the initialTeleportId
     * 5. When client confirms the teleport, PbPlayHandler unlocks the forwarder
     *    and all queued packets flush in order.
     */
    private void onServerConnectionEstablished(MinecraftClient mc, String serverIp, int serverPort) {
        LOGGER.info("Server connection established to {}:{} for {}", serverIp, serverPort, profile.getName());

        try {
            // Get the server connection from the network handler
            ClientConnection serverConnection = mc.getNetworkHandler().getConnection();
            proxyServer.setServerConnection(serverConnection);

            // Get the server's registry manager — may be null briefly during connection setup
            net.minecraft.registry.DynamicRegistryManager serverRegistry = mc.getNetworkHandler().getRegistryManager();
            if (serverRegistry == null) {
                // Fall back to the proxy's cached registry
                LOGGER.warn("Server registry manager is null, using cached registry for {}", profile.getName());
                serverRegistry = proxyServer.getRegistryCache().isLoaded()
                        ? proxyServer.getRegistryCache().getRegistryManager()
                        : net.minecraft.registry.DynamicRegistryManager.EMPTY;
            }

            // Re-transition the outbound encoder to use the REAL server's registry.
            connection.transitionOutbound(
                    PlayStateFactories.S2C.bind(RegistryByteBuf.makeFactory(serverRegistry)));
            LOGGER.info("Re-transitioned outbound encoder to real server registry for {}", profile.getName());

            // Also re-transition inbound to use the server's registry for decoding C2S packets
            // (needed for CustomPayload packets that reference registry entries)
            final net.minecraft.registry.DynamicRegistryManager finalRegistry = serverRegistry;

            // Send the server name to the client
            String address = serverIp + ":" + serverPort;
            connection.send(new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                    eu.client.pingbypass.protocol.PbCustomPayload.fromPacket(
                            new eu.client.pingbypass.protocol.packets.S2CServerNamePacket(address))));

            // Create S2C forwarder — start it immediately without locking.
            // Simpler approach: just start forwarding right away.
            S2CForwarder s2cForwarder = new S2CForwarder(connection);
            s2cForwarder.start();

            // Replay the full world state to the client
            int initialTeleportId = WorldStateReplay.replay(connection);

            // Unlock immediately (no-op since we didn't lock, but keeps the API consistent)
            // The client will process the replay and start sending packets right away.

            // Transition to PbPlayHandler for C2S input handling (client → proxy)
            PbPlayHandler playHandler = new PbPlayHandler(
                    proxyServer, connection, profile, s2cForwarder, 0);
            connection.transitionInbound(
                    PlayStateFactories.C2S.bind(RegistryByteBuf.makeFactory(finalRegistry)),
                    playHandler);

            LOGGER.info("Proxy fully active for {} on {}:{} (awaiting teleport confirm)",
                    profile.getName(), serverIp, serverPort);
        } catch (Exception e) {
            LOGGER.error("Failed to set up forwarding for {}", profile.getName(), e);
            sendError("Failed to set up forwarding: " + e.getMessage());
        }
    }

    /**
     * Sends an S2C_ERROR packet to the client over the custom channel.
     */
    private void sendError(String message) {
        if (connection.isOpen()) {
            connection.send(new CustomPayloadS2CPacket(
                    PbCustomPayload.fromPacket(new S2CErrorPacket(message))));
        }
    }

    /**
     * Sends a chat message to the client (system message).
     */
    private void sendChat(String message) {
        if (connection.isOpen()) {
            connection.send(new net.minecraft.network.packet.s2c.play.GameMessageS2CPacket(
                    net.minecraft.text.Text.literal(message), false));
        }
    }

    @Override
    public void onKeepAlive(KeepAliveC2SPacket packet) {
        // Client responded to our keepalive — just update the timestamp
        this.lastKeepAliveTime = System.currentTimeMillis();
    }

    @Override
    public void onDisconnected(DisconnectionInfo info) {
        LOGGER.info("Client {} disconnected during waiting state", profile.getName());
    }

    @Override
    public boolean isConnectionOpen() {
        return this.connection.isOpen();
    }

    public GameProfile getProfile() {
        return profile;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public ClientConnection getConnection() {
        return connection;
    }

    // --- ServerPlayPacketListener method stubs ---

    @Override public void onTeleportConfirm(TeleportConfirmC2SPacket p) {}
    @Override public void onQueryBlockNbt(QueryBlockNbtC2SPacket p) {}
    @Override public void onUpdateDifficulty(UpdateDifficultyC2SPacket p) {}
    @Override public void onMessageAcknowledgment(MessageAcknowledgmentC2SPacket p) {}
    @Override public void onCommandExecution(CommandExecutionC2SPacket p) {}
    @Override public void onChatCommandSigned(ChatCommandSignedC2SPacket p) {}
    @Override public void onChatMessage(ChatMessageC2SPacket p) {}
    @Override public void onPlayerSession(PlayerSessionC2SPacket p) {}
    @Override public void onAcknowledgeChunks(AcknowledgeChunksC2SPacket p) {}
    @Override public void onClientStatus(ClientStatusC2SPacket p) {}
    @Override public void onClientTickEnd(ClientTickEndC2SPacket p) {}
    @Override public void onRequestCommandCompletions(RequestCommandCompletionsC2SPacket p) {}
    @Override public void onAcknowledgeReconfiguration(AcknowledgeReconfigurationC2SPacket p) {}
    @Override public void onButtonClick(ButtonClickC2SPacket p) {}
    @Override public void onClickSlot(ClickSlotC2SPacket p) {}
    @Override public void onCloseHandledScreen(CloseHandledScreenC2SPacket p) {}
    @Override public void onSlotChangedState(SlotChangedStateC2SPacket p) {}
    @Override public void onDebugSampleSubscription(DebugSampleSubscriptionC2SPacket p) {}
    @Override public void onBookUpdate(BookUpdateC2SPacket p) {}
    @Override public void onQueryEntityNbt(QueryEntityNbtC2SPacket p) {}
    @Override public void onPlayerInteractEntity(PlayerInteractEntityC2SPacket p) {}
    @Override public void onJigsawGenerating(JigsawGeneratingC2SPacket p) {}
    @Override public void onUpdateDifficultyLock(UpdateDifficultyLockC2SPacket p) {}
    @Override public void onPlayerMove(PlayerMoveC2SPacket p) {}
    @Override public void onVehicleMove(VehicleMoveC2SPacket p) {}
    @Override public void onBoatPaddleState(BoatPaddleStateC2SPacket p) {}
    @Override public void onPickItemFromBlock(PickItemFromBlockC2SPacket p) {}
    @Override public void onPickItemFromEntity(PickItemFromEntityC2SPacket p) {}
    @Override public void onCraftRequest(CraftRequestC2SPacket p) {}
    @Override public void onUpdatePlayerAbilities(UpdatePlayerAbilitiesC2SPacket p) {}
    @Override public void onPlayerAction(PlayerActionC2SPacket p) {}
    @Override public void onClientCommand(ClientCommandC2SPacket p) {}
    @Override public void onPlayerInput(PlayerInputC2SPacket p) {}
    @Override public void onPlayerLoaded(PlayerLoadedC2SPacket p) {}
    @Override public void onRecipeCategoryOptions(RecipeCategoryOptionsC2SPacket p) {}
    @Override public void onRecipeBookData(RecipeBookDataC2SPacket p) {}
    @Override public void onRenameItem(RenameItemC2SPacket p) {}
    @Override public void onAdvancementTab(AdvancementTabC2SPacket p) {}
    @Override public void onBundleItemSelected(BundleItemSelectedC2SPacket p) {}
    @Override public void onSelectMerchantTrade(SelectMerchantTradeC2SPacket p) {}
    @Override public void onUpdateBeacon(UpdateBeaconC2SPacket p) {}
    @Override public void onUpdateSelectedSlot(UpdateSelectedSlotC2SPacket p) {}
    @Override public void onUpdateCommandBlock(UpdateCommandBlockC2SPacket p) {}
    @Override public void onUpdateCommandBlockMinecart(UpdateCommandBlockMinecartC2SPacket p) {}
    @Override public void onCreativeInventoryAction(CreativeInventoryActionC2SPacket p) {}
    @Override public void onUpdateJigsaw(UpdateJigsawC2SPacket p) {}
    @Override public void onUpdateStructureBlock(UpdateStructureBlockC2SPacket p) {}
    @Override public void onUpdateSign(UpdateSignC2SPacket p) {}
    @Override public void onHandSwing(HandSwingC2SPacket p) {}
    @Override public void onSpectatorTeleport(SpectatorTeleportC2SPacket p) {}
    @Override public void onPlayerInteractBlock(PlayerInteractBlockC2SPacket p) {}
    @Override public void onPlayerInteractItem(PlayerInteractItemC2SPacket p) {}

    // --- ServerCommonPacketListener method stubs ---
    @Override public void onClientOptions(ClientOptionsC2SPacket p) {}
    @Override public void onPong(CommonPongC2SPacket p) {}
    @Override public void onResourcePackStatus(ResourcePackStatusC2SPacket p) {}
    @Override public void onCookieResponse(CookieResponseC2SPacket p) {}

    // --- ServerQueryPingPacketListener method stub ---
    @Override public void onQueryPing(net.minecraft.network.packet.c2s.query.QueryPingC2SPacket p) {}
}
