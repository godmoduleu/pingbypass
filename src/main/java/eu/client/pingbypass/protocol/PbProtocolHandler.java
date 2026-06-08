package eu.client.pingbypass.protocol;

import eu.client.pingbypass.protocol.packets.*;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PbProtocolHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbProtocolHandler.class);

    private final Map<Integer, Function<PacketByteBuf, PbPacket>> factories = new HashMap<>();
    private final Map<Integer, PbPacketHandler<?>> handlers = new HashMap<>();

    public PbProtocolHandler() {
        registerFactories();
    }


    @SuppressWarnings("unchecked")
    public <T extends PbPacket> void register(int packetId, Function<PacketByteBuf, T> factory, PbPacketHandler<T> handler) {
        factories.put(packetId, (Function<PacketByteBuf, PbPacket>) (Function<?, ?>) factory);
        handlers.put(packetId, handler);
    }

    public <T extends PbPacket> void registerFactory(int packetId, Function<PacketByteBuf, T> factory) {
        factories.put(packetId, (Function<PacketByteBuf, PbPacket>) (Function<?, ?>) factory);
    }

    public <T extends PbPacket> void registerHandler(int packetId, PbPacketHandler<T> handler) {
        handlers.put(packetId, handler);
    }

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

    public void registerStayHandler(eu.client.pingbypass.server.ProxyServer proxyServer) {
        register(C2SStayPacket.ID, C2SStayPacket::new, (packet, connection) -> {
            proxyServer.setStayConnected(packet.isStay());
            LOGGER.info("[PbProtocol] Stay Connected set to {} via protocol handler", packet.isStay());
        });
    }

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
