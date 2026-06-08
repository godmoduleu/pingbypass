package eu.client.managers;

import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.utils.IMinecraft;
import eu.client.utils.system.Timer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorldManager implements IMinecraft {
    private static final Vec3i[] SPHERE = new Vec3i[4187707];
    private static final int[] INDICES = new int[101];

    @Getter private final Map<UUID, Integer> poppedTotems = new ConcurrentHashMap<>();
    private final List<UUID> deadPlayers = new ArrayList<>();

    @Getter @Setter private float timerMultiplier = 1.0f;

    @Getter private final Timer placeTimer = new Timer();

    public WorldManager() {
        EUClient.EVENT_HANDLER.subscribe(this);

        BlockPos origin = BlockPos.ORIGIN;
        Set<BlockPos> positions = new TreeSet<>((o, p) -> {
            if (o.equals(p)) return 0;

            int compare = Double.compare(origin.getSquaredDistance(o), origin.getSquaredDistance(p));
            if (compare == 0) compare = Integer.compare(Math.abs(o.getX()) + Math.abs(o.getY()) + Math.abs(o.getZ()), Math.abs(p.getX()) + Math.abs(p.getY()) + Math.abs(p.getZ()));

            return compare == 0 ? 1 : compare;
        });

        for (int x = origin.getX() - 100; x <= origin.getX() + 100; x++) {
            for (int z = origin.getZ() - 100; z <= origin.getZ() + 100; z++) {
                for (int y = origin.getY() - 100; y < origin.getY() + 100; y++) {
                    double distance = (origin.getX() - x) * (origin.getX() - x) + (origin.getZ() - z) * (origin.getZ() - z) + (origin.getY() - y) * (origin.getY() - y);
                    if (distance < MathHelper.square(100)) positions.add(new BlockPos(x, y, z));
                }
            }
        }

        int i = 0;
        int currentDistance = 0;

        for (BlockPos position : positions) {
            if (Math.sqrt(origin.getSquaredDistance(position)) > currentDistance) INDICES[currentDistance++] = i;
            SPHERE[i++] = position;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.world == null) return;

        for (UUID uuid : new ArrayList<>(poppedTotems.keySet())) {
            if (mc.world.getPlayers().stream().noneMatch(player -> player.getUuid().equals(uuid))) {
                poppedTotems.remove(uuid);
            }
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.deathTime <= 0 && player.getHealth() > 0) {
                deadPlayers.remove(player.getUuid());
                continue;
            }

            if (deadPlayers.contains(player.getUuid())) continue;

            EUClient.EVENT_HANDLER.post(new PlayerDeathEvent(player));
            deadPlayers.add(player.getUuid());

            poppedTotems.remove(player.getUuid());
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.world == null) return;

        if (event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;

            int pops = poppedTotems.getOrDefault(player.getUuid(), 0);
            poppedTotems.put(player.getUuid(), ++pops);

            EUClient.EVENT_HANDLER.post(new PlayerPopEvent(player, pops));
        }
    }

    public int getRadius(double radius) {
        return INDICES[MathHelper.clamp((int) Math.ceil(radius), 0, INDICES.length)];
    }

    public Vec3i getOffset(int index) {
        return SPHERE[index];
    }
}
