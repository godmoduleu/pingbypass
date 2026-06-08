package eu.client.modules.impl.miscellaneous;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.StringSetting;
import eu.client.utils.system.Timer;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@RegisterModule(name = "AutoLogin", description = "Automatically logs in on cracked servers.", category = Module.Category.MISCELLANEOUS)
public class AutoLoginModule extends Module {
    public StringSetting password = new StringSetting("Password", "The password to use when logging in.", "password");

    private final Timer timer = new Timer();

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if(getNull() || !timer.hasTimeElapsed(10000)) return;

        if(event.getPacket() instanceof GameMessageS2CPacket packet) {
            String s = packet.content().getString().toLowerCase();

            if(s.contains("/register")) {
                mc.getNetworkHandler().sendChatCommand("register " + password.getValue() + " " + password.getValue());
                EUClient.CHAT_MANAGER.tagged("Registered successfully.", getName());
                timer.reset();
            } else if(s.contains("/login")) {
                mc.getNetworkHandler().sendChatCommand("login " + password.getValue());
                EUClient.CHAT_MANAGER.tagged("Logged in as " + mc.getSession().getUsername() + ".", getName());
                timer.reset();
            }
        }
    }
}
