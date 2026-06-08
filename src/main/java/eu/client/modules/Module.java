package eu.client.modules;

import lombok.Getter;
import eu.client.EUClient;
import eu.client.events.impl.ToggleModuleEvent;
import eu.client.settings.Setting;
import eu.client.settings.impl.*;
import eu.client.utils.IMinecraft;
import eu.client.utils.animations.Animation;
import eu.client.utils.animations.Easing;
import eu.client.utils.chat.ChatUtils;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class Module implements IMinecraft {
    private final String name, description;
    private final Category category;

    private final boolean persistent;
    private final boolean proxyEnhanced;
    private boolean toggled;
    private final List<Setting> settings;

    public BooleanSetting chatNotify;
    public BooleanSetting drawn;
    public BindSetting bind;
    public ModeSetting proxyMode;

    private final Animation animationOffset;

    public Module() {
        RegisterModule annotation = getClass().getAnnotation(RegisterModule.class);

        name = annotation.name();
        description = annotation.description();
        category = annotation.category();
        persistent = annotation.persistent();
        proxyEnhanced = annotation.proxyEnhanced();
        toggled = annotation.toggled();
        settings = new ArrayList<>();
        animationOffset = new Animation(300, Easing.Method.EASE_OUT_CUBIC);

        chatNotify = new BooleanSetting("ChatNotify", "Notifies you in chat whenever the module gets toggled on or off.", true);
        drawn = new BooleanSetting("Drawn", "Renders the module's name on the HUD's module list.", annotation.drawn());
        bind = new BindSetting("Bind", "The keybind that toggles the module on and off.", annotation.bind());

        if (proxyEnhanced) {
            proxyMode = new ModeSetting("ProxyMode", "Where this module executes when connected to a PingBypass proxy.", "Auto", new String[]{"Auto", "Proxy", "Local"});
        }

        if (persistent) toggled = true;
        if (toggled) {
            EUClient.EVENT_HANDLER.subscribe(this);
        }
    }

    public boolean getNull() {
        return (mc.player == null || mc.world == null);
    }

    /**
     * Returns true if this module should run on the proxy server instead of locally.
     * Only applies on the CLIENT side — the proxy server always runs modules normally.
     */
    public boolean shouldRunOnProxy() {
        if (!proxyEnhanced || proxyMode == null) return false;
        if (!eu.client.pingbypass.PingBypassFlags.proxyForwardingActive) return false;
        // Only skip on the CLIENT side, not on the proxy server
        if (EUClient.PINGBYPASS_CONFIG != null && EUClient.PINGBYPASS_CONFIG.isServer()) return false;

        return switch (proxyMode.getValue()) {
            case "Auto", "Proxy" -> true;
            case "Local" -> false;
            default -> true;
        };
    }

    /**
     * Returns true if this module should skip ACTIONS (sending packets, placing blocks,
     * attacking) but should still run targeting/state logic for rendering purposes.
     * Use this instead of shouldRunOnProxy() when you need the module to find targets
     * on the client side so renders work, but not actually execute combat actions.
     */
    public boolean shouldSkipActions() {
        return shouldRunOnProxy();
    }

    /**
     * Returns true if this module is proxy-enhanced and currently executing
     * on the proxy server. Returns false if ProxyMode is set to "Local"
     * (meaning the client handles it instead).
     */
    public boolean isRunningOnProxy() {
        if (!proxyEnhanced) return false;
        if (!eu.client.pingbypass.PingBypassFlags.proxyForwardingActive) return false;
        if (EUClient.PINGBYPASS_CONFIG == null || !EUClient.PINGBYPASS_CONFIG.isServer()) return false;
        // Respect ProxyMode — if set to "Local", the proxy should not run this module
        if (proxyMode != null && proxyMode.getValue().equals("Local")) return false;
        return true;
    }

    public void onEnable() {}
    public void onDisable() {}

    public String getMetaData() {
        return "";
    }

    /**
     * Returns the proxy indicator suffix for the HUD, or empty string if not applicable.
     */
    public String getProxyIndicator() {
        if (shouldRunOnProxy()
                && EUClient.PINGBYPASS_CONFIG != null && EUClient.PINGBYPASS_CONFIG.isServer()) {
            return " §d[PB]";
        }
        return "";
    }

    /**
     * Returns true if this module should NOT execute locally because it runs on the proxy.
     */
    private boolean shouldSkipLocalExecution() {
        return shouldRunOnProxy();
    }

    public void setToggled(boolean toggled) {
        setToggled(toggled, true);
    }

    public void setToggled(boolean toggled, boolean notify) {
        if (persistent) return;
        if (toggled == this.toggled) return;

        this.toggled = toggled;
        EUClient.EVENT_HANDLER.post(new ToggleModuleEvent(this, this.toggled));

        // If this is a proxy-enhanced module and we're connected as a client,
        // send the toggle to the proxy server
        if (shouldRunOnProxy() && notify && EUClient.PINGBYPASS_CONFIG != null
                && !EUClient.PINGBYPASS_CONFIG.isServer()) {
            sendProxyToggle(this.toggled);
        }

        // If this is a proxy-enhanced module on the SERVER side (proxy),
        // sync the toggle state back to the connected client
        if (proxyEnhanced && EUClient.PINGBYPASS_CONFIG != null
                && EUClient.PINGBYPASS_CONFIG.isServer()
                && eu.client.pingbypass.PingBypassFlags.proxyForwardingActive
                && EUClient.PROXY_SERVER != null) {
            var packet = new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                    eu.client.pingbypass.protocol.PbCustomPayload.fromPacket(
                            new eu.client.pingbypass.protocol.packets.S2CModuleStatePacket(name, this.toggled)));
            for (net.minecraft.network.ClientConnection conn : EUClient.PROXY_SERVER.getConnections()) {
                if (conn.isOpen()) conn.send(packet);
            }
        }

        if (this.toggled) {
            animationOffset.setEasing(Easing.Method.EASE_OUT_CUBIC);

            if (notify && chatNotify.getValue()) {
                EUClient.CHAT_MANAGER.message(ChatUtils.getPrimary() + name + ChatUtils.getSecondary() + ".toggled = " + Formatting.GREEN + "true" + ChatUtils.getSecondary() + ";", "toggle-" + getName().toLowerCase());
            }

            onEnable();
            if (this.toggled) EUClient.EVENT_HANDLER.subscribe(this);
        } else {
            animationOffset.setEasing(Easing.Method.EASE_IN_CUBIC);

            EUClient.EVENT_HANDLER.unsubscribe(this);
            onDisable();

            if (notify && chatNotify.getValue()) {
                EUClient.CHAT_MANAGER.message(ChatUtils.getPrimary() + name + ChatUtils.getSecondary() + ".toggled = " + Formatting.RED + "false" + ChatUtils.getSecondary() + ";", "toggle-" + getName().toLowerCase());
            }
        }
    }

    public int getBind() {
        return bind.getValue();
    }

    public void setBind(int bind) {
        this.bind.setValue(bind);
    }

    public void resetValues() {
        for (Setting uncastedSetting : settings) {
            if (uncastedSetting instanceof BooleanSetting setting) setting.resetValue();
            if (uncastedSetting instanceof NumberSetting setting) setting.resetValue();
            if (uncastedSetting instanceof ModeSetting setting) setting.resetValue();
            if (uncastedSetting instanceof StringSetting setting) setting.resetValue();
            if (uncastedSetting instanceof BindSetting setting) setting.resetValue();
            if (uncastedSetting instanceof WhitelistSetting setting) setting.clear();
            if (uncastedSetting instanceof ColorSetting setting) setting.resetValue();
        }
    }

    public Setting getSetting(String name) {
        return settings.stream().filter(s -> s.getName().equalsIgnoreCase(name) && !(s instanceof CategorySetting)).findFirst().orElse(null);
    }

    /**
     * Sends a module toggle packet to the PingBypass proxy server.
     */
    private void sendProxyToggle(boolean enabled) {
        try {
            if (mc.getNetworkHandler() != null) {
                var payload = eu.client.pingbypass.protocol.PbCustomPayload.fromPacket(
                        new eu.client.pingbypass.protocol.packets.C2SModuleTogglePacket(name, enabled));
                mc.getNetworkHandler().getConnection().send(
                        new net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket(payload));
            }
        } catch (Exception e) {
            EUClient.LOGGER.warn("[PingBypass] Failed to send module toggle to proxy", e);
        }
    }

    @Getter
    public enum Category {
        COMBAT("Combat"),
        PLAYER("Player"),
        VISUALS("Visuals"),
        MOVEMENT("Movement"),
        MISCELLANEOUS("Miscellaneous"),
        CORE("Core");

        private final String name;

        Category(String name) {
            this.name = name;
        }
    }
}
