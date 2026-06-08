package eu.client.pingbypass.handler;

import com.mojang.authlib.GameProfile;
import eu.client.Pingbypass;
import eu.client.pingbypass.protocol.PbCustomPayload;
import eu.client.pingbypass.server.ProxyServer;
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
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles password verification for the PingBypass proxy connection.
 * Sends S2C_PASSWORD_REQUEST on init, then waits for C2S_PASSWORD response.
 * On correct password, transitions to PbWaitingHandler.
 * On wrong password, disconnects with timing attack mitigation.
 */
public class PbPasswordHandler implements ServerPlayPacketListener, TickablePacketListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbPasswordHandler.class);

    private static final long KEEP_ALIVE_INTERVAL_MS = 15_000L;

    private final ProxyServer proxyServer;
    private final ClientConnection connection;
    private final GameProfile profile;
    private final net.minecraft.registry.DynamicRegistryManager registryManager;
    private boolean passwordRequestSent;
    private boolean lobbyWorldSent;
    private boolean clientInPlayState;
    private long lastKeepAliveTime;

    public PbPasswordHandler(ProxyServer proxyServer, ClientConnection connection, GameProfile profile,
                             net.minecraft.registry.DynamicRegistryManager registryManager) {
        this.proxyServer = proxyServer;
        this.connection = connection;
        this.profile = profile;
        this.registryManager = registryManager;
        this.lastKeepAliveTime = 0;
        LOGGER.info("Password handler initialized for {}", profile.getName());
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();

        // Send the lobby world first so the client can transition to PLAY state
        // and process custom payloads. Without GameJoinS2CPacket, the client
        // can't send packets through Fabric's networking layer.
        if (!lobbyWorldSent) {
            lobbyWorldSent = true;
            eu.client.pingbypass.server.LobbyWorldSender.sendLobbyWorld(connection, registryManager);
        }

        // Send periodic keep-alives
        if (now - lastKeepAliveTime >= KEEP_ALIVE_INTERVAL_MS) {
            lastKeepAliveTime = now;
            connection.send(new net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket(now));
        }

        // Only send the password request after the client has confirmed it's in
        // PLAY state by responding to a keep-alive.
        // too early, the client may still be in CONFIGURATION and drop it.
        if (!passwordRequestSent && clientInPlayState) {
            passwordRequestSent = true;
            connection.send(new CustomPayloadS2CPacket(PbCustomPayload.passwordRequest()));
            LOGGER.info("Sent password request to {}", profile.getName());
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
            if (packetId == PbCustomPayload.C2S_PASSWORD) {
                handlePassword(buf.readString());
            } else {
                LOGGER.warn("Unexpected packet ID {} from {} during password verification",
                        packetId, profile.getName());
            }
        } finally {
            buf.release();
        }
    }

    private void handlePassword(String password) {
        String expected = Pingbypass.PINGBYPASS_CONFIG.getPassword();
        if (expected.equals(password)) {
            LOGGER.info("Password accepted for {}", profile.getName());
            // Transition to PbWaitingHandler (proxy not connected to server yet)
            // Re-use the same play state protocol since we're already in play state
            connection.transitionInbound(
                    PlayStateFactories.C2S.bind(RegistryByteBuf.makeFactory(DynamicRegistryManager.EMPTY)),
                    new PbWaitingHandler(proxyServer, connection, profile, registryManager));
        } else {
            LOGGER.warn("Wrong password from {}", profile.getName());
            // Timing attack mitigation: sleep random 1-10ms
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(1, 11));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            connection.disconnect(Text.literal("Wrong password"));
        }
    }

    @Override
    public void onDisconnected(DisconnectionInfo info) {
        LOGGER.info("Client {} disconnected during password verification", profile.getName());
    }

    @Override
    public boolean isConnectionOpen() {
        return this.connection.isOpen();
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
    @Override public void onKeepAlive(KeepAliveC2SPacket p) {
        // Client responded to our keep-alive — it's in PLAY state and ready
        clientInPlayState = true;
    }
    @Override public void onPong(CommonPongC2SPacket p) {}
    @Override public void onResourcePackStatus(ResourcePackStatusC2SPacket p) {}
    @Override public void onCookieResponse(CookieResponseC2SPacket p) {}

    // --- ServerQueryPingPacketListener method stub ---
    @Override public void onQueryPing(net.minecraft.network.packet.c2s.query.QueryPingC2SPacket p) {}
}
