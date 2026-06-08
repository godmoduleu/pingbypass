package eu.client.modules.impl.combat;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.events.impl.PlayerMoveEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.events.impl.UpdateMovementEvent;
import eu.client.mixins.accessors.ClientPlayerInteractionManagerAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.MovementUtils;
import eu.client.utils.minecraft.PositionUtils;
import eu.client.utils.rotations.RotationUtils;
import eu.client.utils.system.Timer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "MaceAura", description = "Automatically maces people after falling.", category = Module.Category.COMBAT)
public class MaceAuraModule extends Module {
    public BooleanSetting silent = new BooleanSetting("Silent", "Silently switch to the mace so others cant see you are holding it.", false);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Whether or not to rotate when attacking the target.", true);
    public NumberSetting range = new NumberSetting("Range", "The maximum distance at which players can be at in order to be a valid target.", 20.0, 10.0, 30.0);
    public NumberSetting attackRange = new NumberSetting("AttackRange", "The maximum distance at which players can be at in order to attack them with the mace.", 6.0, 0.0, 12.0);
    public BooleanSetting rubberband = new BooleanSetting("Rubberband", "Whether or not to rubberband you back to your original position before macing.", false);
    public BooleanSetting movementAssist = new BooleanSetting("MovementAssist", "Moves you towards your target while falling.", true);
    public NumberSetting extrapolation = new NumberSetting("Extrapolation", "Extrapolates the target's position to calculate positions ahead of time.", new BooleanSetting.Visibility(movementAssist, true), 0, 0, 20);

    private PlayerEntity target = null;
    private Timer timer = new Timer();

    private boolean attacking = false;
    private boolean shouldAttack = false;
    private boolean reset = false;

    @Override
    public void onEnable() {
        if(getNull()) return;

        if(mc.player.isGliding()) mc.player.stopGliding();
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if(event.getPacket() instanceof PlayerPositionLookS2CPacket && reset) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
            reset = false;
        }
    }

    @SubscribeEvent
    public void onPlayerMove(PlayerMoveEvent event) {
        if(!movementAssist.getValue() || mc.player.isOnGround() || target == null) return;
        if(mc.player.distanceTo(target) <= attackRange.getValue().floatValue()) return;
        if(!timer.hasTimeElapsed(200)) return;

        Vec3d vec3d = PositionUtils.extrapolate(target, extrapolation.getValue().intValue()).getCenter();
        MovementUtils.moveTowards(event, vec3d, MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED));
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        target = getTarget();

        attacking = false;
        shouldAttack = false;

        if (target == null || mc.player.distanceTo(target) > attackRange.getValue().floatValue()) return;

        if(!silent.getValue() && !mc.player.getMainHandStack().getItem().equals(Items.MACE)) return;

        int slot = InventoryUtils.find(Items.MACE, 0, 8);
        if(slot == -1) return;

        if(rotate.getValue()) EUClient.ROTATION_MANAGER.rotate(RotationUtils.getRotations(target), this);

        attacking = true;

        if(!timer.hasTimeElapsed(1000)) return;

        shouldAttack = true;
    }

    @SubscribeEvent
    public void onUpdateMovement$POST(UpdateMovementEvent.Post event) {
        if (mc.player == null || mc.world == null || !shouldAttack || !attacking || target == null) {
            shouldAttack = false;
            return;
        }

        int slot = InventoryUtils.find(Items.MACE, 0, 8);
        int previousSlot = mc.player.getInventory().selectedSlot;

        Vec3d previous = mc.player.getPos();

        if(silent.getValue()) switchSlot(slot);
        mc.interactionManager.attackEntity(mc.player, target);
        if(silent.getValue()) switchSlot(previousSlot);

        mc.player.swingHand(Hand.MAIN_HAND);

        if(rubberband.getValue()) doRubberband(previous);

        shouldAttack = false;
        timer.reset();
    }

    private void switchSlot(int slot) {
        mc.player.getInventory().selectedSlot = slot;
        ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
    }

    private void doRubberband(Vec3d previous) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ(), false, mc.player.horizontalCollision));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(previous.x, previous.y, previous.z, false, mc.player.horizontalCollision));
        reset = true;
    }

    private PlayerEntity getTarget() {
        PlayerEntity optimalTarget = null;
        for(PlayerEntity player : mc.world.getPlayers()) {
            if(player == mc.player) continue;
            if (!player.isAlive() || player.getHealth() <= 0.0f) continue;
            if (mc.player.squaredDistanceTo(player) > MathHelper.square(range.getValue().doubleValue())) continue;
            if (EUClient.FRIEND_MANAGER.contains(player.getName().getString())) continue;

            if(optimalTarget == null) {
                optimalTarget = player;
                continue;
            }

            if(mc.player.squaredDistanceTo(player) < mc.player.squaredDistanceTo(optimalTarget)) {
                optimalTarget = player;
            }
        }

        return optimalTarget;
    }
}
