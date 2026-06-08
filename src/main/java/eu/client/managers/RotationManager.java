package eu.client.managers;

import lombok.Getter;
import eu.client.Pingbypass;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.mixins.accessors.EntityAccessor;
import eu.client.modules.Module;
import eu.client.utils.IMinecraft;
import eu.client.utils.animations.Easing;
import eu.client.utils.rotations.Rotation;
import eu.client.utils.system.MathUtils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class RotationManager implements IMinecraft {
    private final PriorityBlockingQueue<Rotation> queue = new PriorityBlockingQueue<>(11, this::compareRotations);
    @Getter
    private Rotation rotation = null;

    private float prevFixYaw;

    private float prevYaw;
    private float prevPitch;

    @Getter
    private float serverYaw;
    @Getter
    private float serverPitch;

    private float prevRenderYaw, prevRenderPitch;
    private long lastRenderTime = 0L;

    private static final HashMap<String, Integer> PRIORITIES = new HashMap<>();
    static {
        PRIORITIES.put("KillAura", 1);
        PRIORITIES.put("AutoCrystal", 2);
        PRIORITIES.put("SpeedMine", 3);
        PRIORITIES.put("SelfFill", 4);
    }

    public RotationManager() {
        Pingbypass.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent(priority = Integer.MIN_VALUE)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        queue.removeIf(rotation -> System.currentTimeMillis() - rotation.getTime() > 100);
        rotation = queue.peek();

        if (rotation == null)
            return;
        lastRenderTime = System.currentTimeMillis();
    }

    @SubscribeEvent(priority = Integer.MAX_VALUE)
    public void onUpdateMovement(UpdateMovementEvent event) {
        if (rotation == null)
            return;
        // On the proxy, don't modify mc.player's yaw/pitch — the client sends
        // its own movement packets and we don't want the proxy's player entity
        // to visibly rotate. Packet rotations are sent directly to the server.
        if (eu.client.pingbypass.PingBypassFlags.proxyForwardingActive
                && Pingbypass.PINGBYPASS_CONFIG != null && Pingbypass.PINGBYPASS_CONFIG.isServer()) {
            return;
        }

        prevYaw = mc.player.getYaw();
        prevPitch = mc.player.getPitch();

        mc.player.setYaw(rotation.getYaw());
        mc.player.setPitch(rotation.getPitch());
    }

    @SubscribeEvent(priority = Integer.MIN_VALUE)
    public void onUpdateMovement$POST(UpdateMovementEvent.Post event) {
        if (rotation == null)
            return;
        if (eu.client.pingbypass.PingBypassFlags.proxyForwardingActive
                && Pingbypass.PINGBYPASS_CONFIG != null && Pingbypass.PINGBYPASS_CONFIG.isServer()) {
            return;
        }

        mc.player.setYaw(prevYaw);
        mc.player.setPitch(prevPitch);
    }

    @SubscribeEvent
    public void onUpdateVelocity(UpdateVelocityEvent event) {
        if (mc.player == null)
            return;
    }

    @SubscribeEvent
    public void onKeyboardTick(KeyboardTickEvent event) {
    }

    @SubscribeEvent
    public void onPlayerJump(PlayerJumpEvent event) {
    }

    @SubscribeEvent
    public void onPlayerJump$POST(PlayerJumpEvent.Post event) {
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null)
            return;

        if (event.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (!packet.changesLook())
                return;

            serverYaw = packet.getYaw(mc.player.getYaw());
            serverPitch = packet.getPitch(mc.player.getPitch());
        }
    }

    public void rotate(float[] rotations, int priority) {
        rotate(rotations[0], rotations[1], priority);
    }

    public void rotate(float yaw, float pitch, int priority) {
        queue.removeIf(rotation -> rotation.getModule() == null && rotation.getPriority() == priority);
        queue.add(new Rotation(yaw, pitch, priority));
    }

    public void rotate(float[] rotations, Module module) {
        rotate(rotations[0], rotations[1], module);
    }

    public void rotate(float yaw, float pitch, Module module) {
        queue.removeIf(rotation -> rotation.getModule() == module);
        queue.add(new Rotation(yaw, pitch, module, getModulePriority(module)));
    }

    public void rotate(float[] rotations, Module module, int priority) {
        rotate(rotations[0], rotations[1], module, priority);
    }

    public void rotate(float yaw, float pitch, Module module, int priority) {
        queue.removeIf(rotation -> rotation.getModule() == module);
        queue.add(new Rotation(yaw, pitch, module, priority));
    }

    public void packetRotate(float[] rotations) {
        packetRotate(rotations[0], rotations[1]);
    }

    public void packetRotate(float yaw, float pitch) {
        if (serverYaw == yaw && serverPitch == pitch)
            return;
        // On the proxy, send the rotation directly to the server connection,
        // bypassing mc.getNetworkHandler() so the proxy's local player state
        // is never touched. The proxy's player entity doesn't visibly rotate
        // because onUpdateMovement is also skipped when proxy is active.
        if (eu.client.pingbypass.PingBypassFlags.proxyForwardingActive
                && Pingbypass.PINGBYPASS_CONFIG != null && Pingbypass.PINGBYPASS_CONFIG.isServer()
                && Pingbypass.PROXY_SERVER != null) {
            var serverConn = Pingbypass.PROXY_SERVER.getServerConnection();
            if (serverConn != null && serverConn.isOpen()) {
                var packet = new PlayerMoveC2SPacket.Full(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision);
                eu.client.pingbypass.server.ProxyServerTickListener.allowSend(() -> serverConn.send(packet));
                return;
            }
        }
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                Pingbypass.POSITION_MANAGER.getServerX(), Pingbypass.POSITION_MANAGER.getServerY(),
                Pingbypass.POSITION_MANAGER.getServerZ(), yaw, pitch,
                Pingbypass.POSITION_MANAGER.isServerOnGround(), mc.player.horizontalCollision));
    }

    public boolean inRenderTime() {
        return System.currentTimeMillis() - lastRenderTime < 1000;
    }

    public float[] getRenderRotations() {
        float from = MathUtils.wrapAngle(prevRenderYaw),
                to = MathUtils.wrapAngle(rotation == null ? mc.player.getYaw() : getServerYaw());
        float delta = to - from;
        if (delta > 180)
            delta -= 380;
        else if (delta < -180)
            delta += 360;

        float yaw = MathHelper.lerp(Easing.toDelta(lastRenderTime, 1000), from, from + delta);
        float pitch = MathHelper.lerp(Easing.toDelta(lastRenderTime, 1000), prevRenderPitch,
                rotation == null ? mc.player.getPitch() : getServerPitch());
        prevRenderYaw = yaw;
        prevRenderPitch = pitch;

        return new float[] { yaw, pitch };
    }

    public int getModulePriority(Module module) {
        return PRIORITIES.getOrDefault(module.getName(), 0);
    }

    private int compareRotations(Rotation target, Rotation rotation) {
        if (target.getPriority() == rotation.getPriority())
            return -Long.compare(target.getTime(), rotation.getTime());
        return -Integer.compare(target.getPriority(), rotation.getPriority());
    }
}
