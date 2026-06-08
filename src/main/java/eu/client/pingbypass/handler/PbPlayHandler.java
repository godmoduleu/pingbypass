package eu.client.pingbypass.handler;

import com.mojang.authlib.GameProfile;
import eu.client.Pingbypass;
import eu.client.modules.Module;
import eu.client.pingbypass.server.ProxyServer;
import eu.client.pingbypass.server.ProxyServerTickListener;
import eu.client.pingbypass.server.S2CForwarder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dumb pipe approach: forward ALL client packets to the real server as-is.
 * The proxy doesn't interpret or replay anything — it just passes packets through.
 * 
 * Modules inject their own packets independently via mc.getNetworkHandler().sendPacket().
 * The proxy's own player.tick() movement is suppressed by the ClientPlayerEntityMixin.
 */
public class PbPlayHandler implements ServerPlayPacketListener, TickablePacketListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbPlayHandler.class);
    private static final long KEEP_ALIVE_INTERVAL_MS = 15_000L;

    private final ProxyServer proxyServer;
    private final ClientConnection clientConnection;
    private final GameProfile profile;
    private final S2CForwarder s2cForwarder;
    private long lastKeepAliveSent;

    public PbPlayHandler(ProxyServer proxyServer, ClientConnection clientConnection,
                         GameProfile profile, S2CForwarder s2cForwarder, int initialTeleportId) {
        this.proxyServer = proxyServer;
        this.clientConnection = clientConnection;
        this.profile = profile;
        this.s2cForwarder = s2cForwarder;
        LOGGER.info("Play handler initialized for {} — dumb pipe mode", profile.getName());
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastKeepAliveSent >= KEEP_ALIVE_INTERVAL_MS) {
            lastKeepAliveSent = now;
            clientConnection.send(new KeepAliveS2CPacket(now));
        }
    }

    @Override
    public void onDisconnected(DisconnectionInfo info) {
        LOGGER.info("Client {} disconnected: {}", profile.getName(), info.reason());
        s2cForwarder.stop();

        // Disconnect the proxy from the server too
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.world != null) {
                mc.world.disconnect();
            }
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().getConnection().disconnect(
                        net.minecraft.text.Text.literal("Client disconnected"));
            }
        });
    }

    @Override
    public boolean isConnectionOpen() { return clientConnection.isOpen(); }

    // ═══════════════════════════════════════════════════════════════════
    // CORE: Forward client packets directly to the real server connection.
    // Uses allowSend() so the ProxyServerTickListener filter lets the
    // packet through if it happens to fire a PacketSendEvent.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * When the client tries to interact (eat/place), sync the server's slot
     * to the client's actual slot. If SpeedMine is actively mining, the
     * server thinks we're holding a pickaxe — but the client wants to eat.
     * We send the client's real slot and tell SpeedMine to pause so it
     * doesn't switch back to pickaxe (which would cancel eating).
     */
    private void syncSlotForInteract() {
        var speedMine = Pingbypass.MODULE_MANAGER.getModule(
                eu.client.modules.impl.player.SpeedMineModule.class);
        if (speedMine != null && speedMine.isToggled() && speedMine.isRunningOnProxy()
                && (speedMine.getPrimary() != null || speedMine.getSecondary() != null)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                int clientSlot = mc.player.getInventory().selectedSlot;
                // Switch server to client's actual slot for the interact
                forward(new UpdateSelectedSlotC2SPacket(clientSlot));
                // Tell SpeedMine to pause — don't switch back to pickaxe
                speedMine.setInteractPaused(true);
            }
        }
    }

    private void forward(Packet<?> packet) {
        ClientConnection serverConn = proxyServer.getServerConnection();
        if (serverConn != null && serverConn.isOpen()) {
            ProxyServerTickListener.allowSend(() -> serverConn.send(packet));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // MOVEMENT — forward as-is, also sync proxy position for modules
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void onPlayerMove(PlayerMoveC2SPacket p) {
        // Sync proxy state so modules can read player position
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            double prevY = mc.player.getY();
            if (p.changesPosition()) {
                mc.player.setPosition(
                        p.getX(mc.player.getX()),
                        p.getY(mc.player.getY()),
                        p.getZ(mc.player.getZ()));

                // Track fallDistance from Y changes (proxy physics don't run)
                double newY = mc.player.getY();
                if (newY < prevY && !p.isOnGround()) {
                    mc.player.fallDistance += (float)(prevY - newY);
                } else if (p.isOnGround()) {
                    mc.player.fallDistance = 0;
                }
            }
            if (p.changesLook()) {
                mc.player.setYaw(p.getYaw(mc.player.getYaw()));
                mc.player.setPitch(p.getPitch(mc.player.getPitch()));
            }
            mc.player.setOnGround(p.isOnGround());
        }
        forward(p);
    }

    // ═══════════════════════════════════════════════════════════════════
    // ALL OTHER PACKETS — just forward directly
    // ═══════════════════════════════════════════════════════════════════

    @Override public void onPlayerInteractBlock(PlayerInteractBlockC2SPacket p) {
        syncSlotForInteract();
        forward(p);
    }
    @Override public void onPlayerInteractItem(PlayerInteractItemC2SPacket p) {
        syncSlotForInteract();
        forward(p);
    }
    @Override public void onPlayerInteractEntity(PlayerInteractEntityC2SPacket p) { forward(p); }
    @Override public void onPlayerAction(PlayerActionC2SPacket p) {
        // When the client releases item use (finishes eating), unpause SpeedMine
        if (p.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
            var speedMine = Pingbypass.MODULE_MANAGER.getModule(
                    eu.client.modules.impl.player.SpeedMineModule.class);
            if (speedMine != null && speedMine.isInteractPaused()) {
                speedMine.setInteractPaused(false);
            }
            forward(p);
            return;
        }
        // When the client starts mining a block, fire AttackBlockEvent on the
        // proxy so proxy-sided modules (like SpeedMine) can pick it up.
        if (p.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.execute(() -> {
                    var event = new eu.client.events.impl.AttackBlockEvent(p.getPos(), p.getDirection());
                    Pingbypass.EVENT_HANDLER.post(event);
                    if (!event.isCancelled()) {
                        forward(p);
                    }
                });
                return;
            }
        }
        forward(p);
    }

    @Override public void onHandSwing(HandSwingC2SPacket p) { forward(p); }
    @Override public void onClickSlot(ClickSlotC2SPacket p) {
        // Replay the click on the proxy's container so its inventory stays in sync
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.execute(() -> {
                try {
                    net.minecraft.screen.ScreenHandler handler =
                            p.getSyncId() == mc.player.currentScreenHandler.syncId
                                    ? mc.player.currentScreenHandler
                                    : mc.player.playerScreenHandler;
                    handler.onSlotClick(p.getSlot(), p.getButton(), p.getActionType(), mc.player);
                } catch (Exception ignored) {}
            });
        }
        forward(p);
    }
    @Override public void onCloseHandledScreen(CloseHandledScreenC2SPacket p) { forward(p); }
    @Override public void onUpdateSelectedSlot(UpdateSelectedSlotC2SPacket p) {
        // Sync proxy's local slot (so modules can read the client's real slot).
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.getInventory().selectedSlot = p.getSelectedSlot();
        }
        // If SpeedMine is actively mining on the proxy, DON'T forward the
        // client's slot change to the real server — SpeedMine controls the
        // server's slot state directly via serverSend(). Forwarding would
        // override the pickaxe with food and break mining.
        var speedMine = Pingbypass.MODULE_MANAGER.getModule(
                eu.client.modules.impl.player.SpeedMineModule.class);
        if (speedMine != null && speedMine.isToggled() && speedMine.isRunningOnProxy()
                && (speedMine.getPrimary() != null || speedMine.getSecondary() != null)) {
            // Don't forward — SpeedMine owns the server's slot state
            return;
        }
        forward(p);
    }
    @Override public void onClientCommand(ClientCommandC2SPacket p) {
        // Sync sprint/sneak state on proxy
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            switch (p.getMode()) {
                case START_SPRINTING -> mc.player.setSprinting(true);
                case STOP_SPRINTING -> mc.player.setSprinting(false);
                case PRESS_SHIFT_KEY -> mc.player.setSneaking(true);
                case RELEASE_SHIFT_KEY -> mc.player.setSneaking(false);
                default -> {}
            }
        }
        forward(p);
    }
    @Override public void onChatMessage(ChatMessageC2SPacket p) { forward(p); }
    @Override public void onCommandExecution(CommandExecutionC2SPacket p) { forward(p); }
    @Override public void onChatCommandSigned(ChatCommandSignedC2SPacket p) { forward(p); }
    @Override public void onMessageAcknowledgment(MessageAcknowledgmentC2SPacket p) { forward(p); }
    @Override public void onPlayerSession(PlayerSessionC2SPacket p) { forward(p); }
    @Override public void onRequestCommandCompletions(RequestCommandCompletionsC2SPacket p) { forward(p); }
    @Override public void onAdvancementTab(AdvancementTabC2SPacket p) { forward(p); }
    @Override public void onSpectatorTeleport(SpectatorTeleportC2SPacket p) { forward(p); }
    @Override public void onResourcePackStatus(ResourcePackStatusC2SPacket p) { forward(p); }
    @Override public void onCookieResponse(CookieResponseC2SPacket p) { forward(p); }
    @Override public void onTeleportConfirm(TeleportConfirmC2SPacket p) { /* proxy already confirmed */ }
    @Override public void onAcknowledgeChunks(AcknowledgeChunksC2SPacket p) { forward(p); }
    @Override public void onClientTickEnd(ClientTickEndC2SPacket p) { forward(p); }
    @Override public void onClientStatus(ClientStatusC2SPacket p) { forward(p); }
    @Override public void onAcknowledgeReconfiguration(AcknowledgeReconfigurationC2SPacket p) { forward(p); }
    @Override public void onClientOptions(ClientOptionsC2SPacket p) { forward(p); }
    @Override public void onQueryBlockNbt(QueryBlockNbtC2SPacket p) { forward(p); }
    @Override public void onUpdateDifficulty(UpdateDifficultyC2SPacket p) { forward(p); }
    @Override public void onSlotChangedState(SlotChangedStateC2SPacket p) { forward(p); }
    @Override public void onDebugSampleSubscription(DebugSampleSubscriptionC2SPacket p) { forward(p); }
    @Override public void onBookUpdate(BookUpdateC2SPacket p) { forward(p); }
    @Override public void onQueryEntityNbt(QueryEntityNbtC2SPacket p) { forward(p); }
    @Override public void onJigsawGenerating(JigsawGeneratingC2SPacket p) { forward(p); }
    @Override public void onUpdateDifficultyLock(UpdateDifficultyLockC2SPacket p) { forward(p); }
    @Override public void onVehicleMove(VehicleMoveC2SPacket p) { forward(p); }
    @Override public void onBoatPaddleState(BoatPaddleStateC2SPacket p) { forward(p); }
    @Override public void onPickItemFromBlock(PickItemFromBlockC2SPacket p) { forward(p); }
    @Override public void onPickItemFromEntity(PickItemFromEntityC2SPacket p) { forward(p); }
    @Override public void onCraftRequest(CraftRequestC2SPacket p) { forward(p); }
    @Override public void onUpdatePlayerAbilities(UpdatePlayerAbilitiesC2SPacket p) { forward(p); }
    @Override public void onPlayerInput(PlayerInputC2SPacket p) { forward(p); }
    @Override public void onPlayerLoaded(PlayerLoadedC2SPacket p) { forward(p); }
    @Override public void onRecipeCategoryOptions(RecipeCategoryOptionsC2SPacket p) { forward(p); }
    @Override public void onRecipeBookData(RecipeBookDataC2SPacket p) { forward(p); }
    @Override public void onRenameItem(RenameItemC2SPacket p) { forward(p); }
    @Override public void onBundleItemSelected(BundleItemSelectedC2SPacket p) { forward(p); }
    @Override public void onSelectMerchantTrade(SelectMerchantTradeC2SPacket p) { forward(p); }
    @Override public void onUpdateBeacon(UpdateBeaconC2SPacket p) { forward(p); }
    @Override public void onUpdateCommandBlock(UpdateCommandBlockC2SPacket p) { forward(p); }
    @Override public void onUpdateCommandBlockMinecart(UpdateCommandBlockMinecartC2SPacket p) { forward(p); }
    @Override public void onCreativeInventoryAction(CreativeInventoryActionC2SPacket p) { forward(p); }
    @Override public void onUpdateJigsaw(UpdateJigsawC2SPacket p) { forward(p); }
    @Override public void onUpdateStructureBlock(UpdateStructureBlockC2SPacket p) { forward(p); }
    @Override public void onUpdateSign(UpdateSignC2SPacket p) { forward(p); }
    @Override public void onButtonClick(ButtonClickC2SPacket p) { forward(p); }

    // ═══════════════════════════════════════════════════════════════════
    // KEEPALIVE — proxy handles its own keepalive with the client,
    // but forwards the client's keepalive response to the server too
    // ═══════════════════════════════════════════════════════════════════

    @Override public void onKeepAlive(KeepAliveC2SPacket p) { /* proxy handles keepalive with client */ }
    @Override public void onPong(CommonPongC2SPacket p) { forward(p); }
    @Override public void onQueryPing(net.minecraft.network.packet.c2s.query.QueryPingC2SPacket p) { }

    // ═══════════════════════════════════════════════════════════════════
    // CUSTOM PAYLOAD — handle PingBypass protocol, forward others
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void onCustomPayload(CustomPayloadC2SPacket p) {
        if (p.payload() instanceof eu.client.pingbypass.protocol.PbCustomPayload pbPayload) {
            handlePbPayload(pbPayload);
            return;
        }
        forward(p);
    }

    private void handlePbPayload(eu.client.pingbypass.protocol.PbCustomPayload pbPayload) {
        net.minecraft.network.PacketByteBuf buf = pbPayload.toBuf();
        try {
            int packetId = buf.readVarInt();
            switch (packetId) {
                case eu.client.pingbypass.protocol.packets.C2SModuleTogglePacket.ID -> {
                    var pkt = new eu.client.pingbypass.protocol.packets.C2SModuleTogglePacket(buf);
                    Module module = Pingbypass.MODULE_MANAGER.getModule(pkt.getModuleName());
                    if (module != null) {
                        MinecraftClient.getInstance().execute(() -> {
                            module.setToggled(pkt.isEnabled(), false);
                            LOGGER.info("[PB] Module {} toggled to {}", pkt.getModuleName(), pkt.isEnabled());
                        });
                    }
                }
                case eu.client.pingbypass.protocol.packets.C2SSettingChangePacket.ID -> {
                    var pkt = new eu.client.pingbypass.protocol.packets.C2SSettingChangePacket(buf);
                    handleSettingChange(pkt);
                }
                default -> LOGGER.debug("[PB] Unknown packet ID: {}", packetId);
            }
        } catch (Exception e) {
            LOGGER.warn("[PB] Failed to handle payload", e);
        } finally {
            buf.release();
        }
    }

    private void handleSettingChange(eu.client.pingbypass.protocol.packets.C2SSettingChangePacket pkt) {
        Module module = Pingbypass.MODULE_MANAGER.getModule(pkt.getModuleName());
        if (module == null) return;

        eu.client.settings.Setting setting = module.getSetting(pkt.getSettingName());
        if (setting == null) return;

        MinecraftClient.getInstance().execute(() -> {
            try {
                String value = pkt.getValue();
                if (setting instanceof eu.client.settings.impl.BooleanSetting s) {
                    s.setValue(Boolean.parseBoolean(value));
                } else if (setting instanceof eu.client.settings.impl.NumberSetting s) {
                    switch (s.getType()) {
                        case INTEGER -> s.setValue(Integer.parseInt(value));
                        case LONG -> s.setValue(Long.parseLong(value));
                        case FLOAT -> s.setValue(Float.parseFloat(value));
                        case DOUBLE -> s.setValue(Double.parseDouble(value));
                    }
                } else if (setting instanceof eu.client.settings.impl.ModeSetting s) {
                    s.setValue(value);
                } else if (setting instanceof eu.client.settings.impl.StringSetting s) {
                    s.setValue(value);
                } else if (setting instanceof eu.client.settings.impl.ColorSetting s) {
                    String[] parts = value.split(",");
                    if (parts.length >= 4) {
                        s.setColor(new java.awt.Color(
                                Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
                        if (parts.length >= 5) s.setSync(Boolean.parseBoolean(parts[4]));
                        if (parts.length >= 6) s.setRainbow(Boolean.parseBoolean(parts[5]));
                    }
                }
                LOGGER.info("[PB] Setting {}.{} = {}", pkt.getModuleName(), pkt.getSettingName(), value);
            } catch (Exception e) {
                LOGGER.warn("[PB] Failed to apply setting {}.{}", pkt.getModuleName(), pkt.getSettingName(), e);
            }
        });
    }
}
