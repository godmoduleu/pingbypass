package eu.client.managers;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import lombok.Getter;
import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.modules.impl.miscellaneous.FastLatencyModule;
import eu.client.utils.IMinecraft;
import eu.client.utils.system.Timer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.util.Arrays;
import java.util.UUID;

@Getter
public class ServerManager implements IMinecraft {
    private final Timer setbackTimer = new Timer();
    private final Timer responseTimer = new Timer();

    private final float[] tickRates = new float[20];
    private int nextIndex = 0;
    private long lastUpdate = -1;
    private long timeJoined;

    private Pair<ServerAddress, ServerInfo> lastConnection;

    public ServerManager() {
        EUClient.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        responseTimer.reset();

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            setbackTimer.reset();
        }

        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            tickRates[nextIndex] = Math.clamp(20.0f / ((System.currentTimeMillis() - lastUpdate) / 1000.0F), 0.0f, 20.0f);
            nextIndex = (nextIndex + 1) % tickRates.length;
            lastUpdate = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onClientConnect(ClientConnectEvent event) {
        Arrays.fill(tickRates, 0);
        nextIndex = 0;
        timeJoined = System.currentTimeMillis();
        lastUpdate = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void handleConnections(PacketReceiveEvent event) {
        if (mc.world == null) return;

        if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for(PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
                    EUClient.EVENT_HANDLER.post(new PlayerConnectEvent(entry.profile().getId()));
                }
            }
        } else if(event.getPacket() instanceof PlayerRemoveS2CPacket packet) {
            for(UUID id : packet.profileIds()) {
                EUClient.EVENT_HANDLER.post(new PlayerDisconnectEvent(id));
            }
        }
    }

    @SubscribeEvent
    public void onServerConnect(ServerConnectEvent event) {
        lastConnection = new ObjectObjectImmutablePair<>(event.getAddress(), event.getInfo());
    }

    public float getTickRate() {
        if (mc.player == null) return 0;
        if (System.currentTimeMillis() - timeJoined < 4000) return 20;

        int ticks = 0;
        float tickRates = 0.0f;

        for (float tickRate : this.tickRates) {
            if (tickRate > 0) {
                tickRates += tickRate;
                ticks++;
            }
        }

        return tickRates / ticks;
    }

    public int getPingDelay() {
        return (int) (getPing() / 25.0f);
    }

    public int getPing() {
        if (EUClient.MODULE_MANAGER.getModule(FastLatencyModule.class).isToggled()) {
            return EUClient.MODULE_MANAGER.getModule(FastLatencyModule.class).getLatency();
        }

        if (mc.getNetworkHandler() == null || mc.player == null) return 0;

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (entry != null) return entry.getLatency();

        // Fallback: when connected via PingBypass proxy, the player list entry
        // might be under the proxy's UUID. Find our own entry by name or use
        // the first entry with non-zero latency.
        if (eu.client.pingbypass.PingBypassFlags.proxyForwardingActive) {
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                if (e.getProfile().getName().equals(mc.player.getName().getString())) {
                    return e.getLatency();
                }
            }
            // Last resort: return the first entry's latency
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                if (e.getLatency() > 0) return e.getLatency();
            }
        }

        return 0;
    }

    public String getServerBrand() {
        if (mc.getCurrentServerEntry() == null || mc.getNetworkHandler() == null || mc.getNetworkHandler().getBrand() == null) return "Vanilla";
        return mc.getNetworkHandler().getBrand();
    }

    public String getServer() {
        return mc.isInSingleplayer() ? "Singleplayer" : ServerAddress.parse(mc.getCurrentServerEntry().address).getAddress();
    }
}
