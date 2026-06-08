package eu.client.modules.impl.miscellaneous;

import lombok.Getter;
import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.chat.ChatUtils;
import eu.client.utils.system.Timer;
import eu.client.utils.system.ZeroTimer;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;

@RegisterModule(name = "FastLatency", description = "Makes ping resolving much faster.", category = Module.Category.MISCELLANEOUS)
public class FastLatencyModule extends Module {
    public NumberSetting delay = new NumberSetting("Delay", "The amount of milliseconds that have to be waited for before resolving your ping again.", 100L, 0, 1000L);

    public BooleanSetting spikeNotifier = new BooleanSetting("SpikeNotifier", "Notifies you in chat whenever your ping spikes.", false);
    public NumberSetting threshold = new NumberSetting("Threshold", "The amount of milliseconds that your ping has to increase for before notifying you.", new BooleanSetting.Visibility(spikeNotifier, true), 30, 0, 1000);

    private final Timer timer = new Timer();
    private final ZeroTimer receivedTimer = new ZeroTimer();

    private long time;
    @Getter private int latency;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        if (receivedTimer.hasTimeElapsed(1000L) && timer.hasTimeElapsed(delay.getValue().longValue())) {
            mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(1000, "/w "));
            time = System.currentTimeMillis();

            receivedTimer.reset();
            timer.reset();
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof CommandSuggestionsS2CPacket packet) {
            if (packet.id() == 1000) {
                int ping = (int) (System.currentTimeMillis() - time);
                if (spikeNotifier.getValue() && ping - latency > threshold.getValue().intValue()) {
                    EUClient.CHAT_MANAGER.message("Your ping has spiked to " + ChatUtils.getPrimary() + ping + "ms" + ChatUtils.getSecondary() + " from " + ChatUtils.getPrimary() + latency + "ms" + ChatUtils.getSecondary() + "!", "module-" + getName().toLowerCase() + "-spike");
                }

                latency = ping;
                receivedTimer.zero();
            }
        }
    }

    @Override
    public String getMetaData() {
        return latency + "ms";
    }
}
