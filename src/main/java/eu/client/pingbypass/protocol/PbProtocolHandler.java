package eu.client.pingbypass.protocol;

import eu.client.pingbypass.protocol.packets.*;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registry and dispatcher for the PingBypass custom packet protocol.
 * Maps packet IDs to factories (for deserialization) and handlers (for processing).
 * Packets are read from the euclient:pingbypass plugin channel.
 */
public class PbProtocolHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbProtocolHandler.class);

    private final Map<Integer, Function<PacketByteBuf, PbPacket>> factories = new HashMap<>();
    private final Map<Integer, PbPacketHandler<?>> handlers = new HashMap<>();

    public PbProtocolHandler() {
        registerFactories();
    }

    /**
     * Registers a packet factory and handler for the given packet ID.
     */
    @SuppressWarnings("unchecked")
    public <T extends PbPacket> void register(int packetId, Function<PacketByteBuf, T> factory, PbPacketHandler<T> handler) {
        factories.put(packetId, (Function<PacketByteBuf, PbPacket>) (Function<?, ?>) factory);
        handlers.put(packetId, handler);
    }

    /**
     * Registers only a packet factory (no handler yet).
     * Handlers are registered later when the actual logic is wired up.
     */
    public <T extends PbPacket> void registerFactory(int packetId, Function<PacketByteBuf, T> factory) {
        factories.put(packetId, (Function<PacketByteBuf, PbPacket>) (Function<?, ?>) factory);
    }

    /**
     * Registers a handler for an already-registered packet ID.
     */
    public <T extends PbPacket> void registerHandler(int packetId, PbPacketHandler<T> handler) {
        handlers.put(packetId, handler);
    }

    /**
     * Reads a VarInt packet ID from the buffer, constructs the packet via the
     * registered factory, and dispatches it to the registered handler.
     * Malformed or unknown packets are logged as warnings without crashing.
     */
    @SuppressWarnings("unchecked")
    public void handle(PacketByteBuf buf, ClientConnection connection) {
        int packetId;
        try {
            packetId = buf.readVarInt();
        } catch (Exception e) {
            LOGGER.warn("[PbProtocol] Failed to read packet ID from buffer", e);
            return;
        }

        Function<PacketByteBuf, PbPacket> factory = factories.get(packetId);
        if (factory == null) {
            LOGGER.warn("[PbProtocol] Unknown packet ID: {}", packetId);
            return;
        }

        PbPacket packet;
        try {
            packet = factory.apply(buf);
        } catch (Exception e) {
            LOGGER.warn("[PbProtocol] Failed to deserialize packet ID {}", packetId, e);
            return;
        }

        PbPacketHandler<PbPacket> handler = (PbPacketHandler<PbPacket>) handlers.get(packetId);
        if (handler == null) {
            LOGGER.warn("[PbProtocol] No handler registered for packet ID: {}", packetId);
            return;
        }

        try {
            handler.handle(packet, connection);
        } catch (Exception e) {
            LOGGER.warn("[PbProtocol] Error handling packet ID {}", packetId, e);
        }
    }

    /**
     * Registers factories for all 10 packet types so they can be deserialized.
     * Actual handlers are registered later by Tasks 12.1 and 12.2.
     * C2S_STAY handler is registered here as it's handled in Task 10.1.
     */
    private void registerFactories() {
        registerFactory(C2SJoinPacket.ID, C2SJoinPacket::new);
        registerFactory(S2CPasswordRequestPacket.ID, S2CPasswordRequestPacket::new);
        registerFactory(C2SPasswordPacket.ID, C2SPasswordPacket::new);
        registerFactory(C2SModuleTogglePacket.ID, C2SModuleTogglePacket::new);
        registerFactory(C2SSettingChangePacket.ID, C2SSettingChangePacket::new);
        registerFactory(S2CModuleStatePacket.ID, S2CModuleStatePacket::new);
        registerFactory(S2CSettingStatePacket.ID, S2CSettingStatePacket::new);
        registerFactory(S2CErrorPacket.ID, S2CErrorPacket::new);
        registerFactory(S2CServerNamePacket.ID, S2CServerNamePacket::new);
        registerFactory(C2SStayPacket.ID, C2SStayPacket::new);
    }

    /**
     * Registers the C2S_STAY handler that toggles the stayConnected flag on the ProxyServer.
     * Called during proxy initialization when the ProxyServer instance is available.
     */
    public void registerStayHandler(eu.client.pingbypass.server.ProxyServer proxyServer) {
        register(C2SStayPacket.ID, C2SStayPacket::new, (packet, connection) -> {
            proxyServer.setStayConnected(packet.isStay());
            LOGGER.info("[PbProtocol] Stay Connected set to {} via protocol handler", packet.isStay());
        });
    }

    /**
     * Registers handlers for C2S_MODULE_TOGGLE (ID 3) and C2S_SETTING_CHANGE (ID 4).
     * On toggle: looks up the module, toggles it, sends S2C_MODULE_STATE back (or S2C_ERROR).
     * On setting change: looks up module and setting, applies value, sends S2C_SETTING_STATE back (or S2C_ERROR).
     *
     * <p><b>Validates: Requirements 9.2, 9.3, 9.4</b></p>
     */
    public void registerModuleHandlers(eu.client.pingbypass.modules.ProxyModuleManager moduleManager) {
        register(C2SModuleTogglePacket.ID, C2SModuleTogglePacket::new, (packet, connection) -> {
            eu.client.modules.Module module = moduleManager.getModule(packet.getModuleName());
            if (module == null) {
                LOGGER.warn("[PbProtocol] Module not found: {}", packet.getModuleName());
                connection.send(new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                        PbCustomPayload.fromPacket(new S2CErrorPacket("Module not found: " + packet.getModuleName()))));
                return;
            }

            module.setToggled(packet.isEnabled());
            LOGGER.info("[PbProtocol] Module {} set to {}", packet.getModuleName(), packet.isEnabled());
            connection.send(new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                    PbCustomPayload.fromPacket(new S2CModuleStatePacket(module.getName(), module.isToggled()))));
        });

        register(C2SSettingChangePacket.ID, C2SSettingChangePacket::new, (packet, connection) -> {
            eu.client.modules.Module module = moduleManager.getModule(packet.getModuleName());
            if (module == null) {
                LOGGER.warn("[PbProtocol] Module not found: {}", packet.getModuleName());
                connection.send(new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                        PbCustomPayload.fromPacket(new S2CErrorPacket("Module not found: " + packet.getModuleName()))));
                return;
            }

            eu.client.settings.Setting setting = module.getSetting(packet.getSettingName());
            if (setting == null) {
                LOGGER.warn("[PbProtocol] Setting not found: {} on module {}", packet.getSettingName(), packet.getModuleName());
                connection.send(new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                        PbCustomPayload.fromPacket(new S2CErrorPacket("Setting not found: " + packet.getSettingName() + " on " + packet.getModuleName()))));
                return;
            }

            applySettingValue(setting, packet.getValue());
            LOGGER.info("[PbProtocol] Setting {}.{} set to {}", packet.getModuleName(), packet.getSettingName(), packet.getValue());
            connection.send(new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                    PbCustomPayload.fromPacket(new S2CSettingStatePacket(module.getName(), setting.getName(), packet.getValue()))));
        });
    }

    /**
     * Deserializes and applies a string value to a Setting based on its runtime type.
     * Supports BooleanSetting, NumberSetting, StringSetting, ModeSetting, and ColorSetting.
     */
    private void applySettingValue(eu.client.settings.Setting setting, String value) {
        if (setting instanceof eu.client.settings.impl.BooleanSetting boolSetting) {
            boolSetting.setValue(Boolean.parseBoolean(value));
        } else if (setting instanceof eu.client.settings.impl.NumberSetting numSetting) {
            try {
                switch (numSetting.getType()) {
                    case INTEGER -> numSetting.setValue(Integer.parseInt(value));
                    case LONG -> numSetting.setValue(Long.parseLong(value));
                    case DOUBLE -> numSetting.setValue(Double.parseDouble(value));
                    case FLOAT -> numSetting.setValue(Float.parseFloat(value));
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("[PbProtocol] Invalid number value '{}' for setting {}", value, setting.getName());
            }
        } else if (setting instanceof eu.client.settings.impl.StringSetting strSetting) {
            strSetting.setValue(value);
        } else if (setting instanceof eu.client.settings.impl.ModeSetting modeSetting) {
            modeSetting.setValue(value);
        } else if (setting instanceof eu.client.settings.impl.ColorSetting colorSetting) {
            // Format: "r,g,b,a,sync,rainbow"
            String[] parts = value.split(",");
            if (parts.length == 6) {
                try {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    int a = Integer.parseInt(parts[3].trim());
                    boolean sync = Boolean.parseBoolean(parts[4].trim());
                    boolean rainbow = Boolean.parseBoolean(parts[5].trim());
                    colorSetting.setValue(new eu.client.settings.impl.ColorSetting.Color(
                            new java.awt.Color(r, g, b, a), sync, rainbow));
                } catch (NumberFormatException e) {
                    LOGGER.warn("[PbProtocol] Invalid color value '{}' for setting {}", value, setting.getName());
                }
            } else {
                LOGGER.warn("[PbProtocol] Invalid color format '{}' for setting {}", value, setting.getName());
            }
        }
    }
}
