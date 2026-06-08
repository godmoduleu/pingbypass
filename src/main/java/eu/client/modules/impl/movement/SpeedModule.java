package eu.client.modules.impl.movement;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerMoveEvent;
import eu.client.events.impl.TickEvent;
import eu.client.mixins.accessors.Vec3dAccessor;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.modules.impl.miscellaneous.FakePlayerModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.MovementUtils;
import eu.client.utils.system.Timer;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2d;

@RegisterModule(name = "Speed", description = "Makes it so that you move faster than normal.", category = Module.Category.MOVEMENT)
public class SpeedModule extends Module {
    public ModeSetting mode = new ModeSetting("Mode", "The method that will be used to increase your speed.", "Strafe", new String[]{"Vanilla", "Strafe", "StrafeStrict", "Grim"});

    public NumberSetting vanillaSpeed = new NumberSetting("VanillaSpeed", "Speed", "The speed that will be applied to your movement.", new ModeSetting.Visibility(mode, "Vanilla"), 10.0, 0.0, 20.0);
    public BooleanSetting vanillaOnGround = new BooleanSetting("VanillaOnGround", "OnGround", "Only applies the speed when you are on ground.", new ModeSetting.Visibility(mode, "Vanilla"), false);

    public BooleanSetting useTimer = new BooleanSetting("UseTimer", "Adds a timer multiplier when strafing.", new ModeSetting.Visibility(mode, "Strafe", "StrafeStrict"), false);
    public BooleanSetting timerBypass = new BooleanSetting("Bypass", "Allows you to use timer on certain servers.", new BooleanSetting.Visibility(useTimer, true), true);
    public NumberSetting bypassThreshold = new NumberSetting("Threshold", "The threshold value for the timer bypass.", new BooleanSetting.Visibility(timerBypass, true), 25, 15, 30);
    public NumberSetting timerMultiplier = new NumberSetting("TimerMultiplier", "Multiplier", "The timer multiplier that will be applied to the timer.", new BooleanSetting.Visibility(useTimer, true), 1.08f, 1.0f, 1.2f);
    public BooleanSetting speedInWater = new BooleanSetting("SpeedInWater", "Increases your speed while in water.", new ModeSetting.Visibility(mode, "Strafe", "StrafeStrict"), false);

    public BooleanSetting autoJump = new BooleanSetting("AutoJump", "Automatically jumps for you when on ground.", new ModeSetting.Visibility(mode, "Grim"), false);

    private double distance, speed, forward;
    private int stage, ticks;
    private boolean pressed = false;

    @Override
    public void onEnable() {
        stage = 1;
        ticks = 0;
    }

    @Override
    public void onDisable() {
        EUClient.WORLD_MANAGER.setTimerMultiplier(1.0f);
        if(pressed) mc.options.jumpKey.setPressed(false);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.getValue().equalsIgnoreCase("Strafe") || mode.getValue().equalsIgnoreCase("StrafeStrict")) {
            distance = Math.sqrt(MathHelper.square(mc.player.getX() - mc.player.prevX) + MathHelper.square(mc.player.getZ() - mc.player.prevZ));
            boolean flag = MovementUtils.isMoving() && !mc.player.isSneaking() && !mc.player.isInFluid() && mc.player.fallDistance < 5.0f;
            EUClient.WORLD_MANAGER.setTimerMultiplier(useTimer.getValue() && flag && (ticks > bypassThreshold.getValue().intValue() || !timerBypass.getValue()) ? timerMultiplier.getValue().floatValue() : 1.0f);
        }

        if (mode.getValue().equalsIgnoreCase("Grim")) {
            if(autoJump.getValue() && MovementUtils.isMoving() && mc.player.isOnGround() && !pressed) {
                mc.options.jumpKey.setPressed(true);
                pressed = true;
            }

            if(!mc.player.isOnGround() && pressed) {
                mc.options.jumpKey.setPressed(false);
                pressed = false;
            }

            int collisions = 0;
            for (Entity entity : mc.world.getEntities()) {
                if (entity != null && entity != mc.player && entity instanceof LivingEntity && !(EUClient.MODULE_MANAGER.getModule(FakePlayerModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(FakePlayerModule.class).getPlayer() == entity) && !(entity instanceof ArmorStandEntity) && MathHelper.sqrt((float) mc.player.squaredDistanceTo(entity)) <= 1.5) {
                    collisions++;
                }
            }

            if (collisions > 0) {
                Vector2d vector2d = MovementUtils.forward(0.08 * collisions);
                mc.player.setVelocity(mc.player.getVelocity().x + vector2d.x, mc.player.getVelocity().y, mc.player.getVelocity().z + vector2d.y);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mode.getValue().equalsIgnoreCase("Strafe") || mode.getValue().equalsIgnoreCase("StrafeStrict")) {
            if ((EUClient.MODULE_MANAGER.getModule(HoleSnapModule.class).isToggled() && EUClient.MODULE_MANAGER.getModule(HoleSnapModule.class).hole != null)) return;

            if (mc.player.fallDistance >= 5.0f || mc.player.isSneaking() || mc.player.isClimbing() || mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.COBWEB || mc.player.getAbilities().flying || (mc.player.isInFluid() && !speedInWater.getValue()))
                return;

            speed = MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED) * (mc.player.input.movementForward <= 0 && forward > 0 ? 0.66 : 1);

            if(stage == 1 && MovementUtils.isMoving() && mc.player.verticalCollision) {
                ((Vec3dAccessor) mc.player.getVelocity()).setY(MovementUtils.getPotionJump(0.3999999463558197));
                event.setMovement(new Vec3d(event.getMovement().getX(), mc.player.getVelocity().getY(), event.getMovement().getZ()));
                speed *= 2.149;
                stage = 2;
            } else if(stage == 2) {
                speed = distance - (0.66 * (distance - MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED)));
                stage = 3;
            } else {
                if (!mc.world.getEntityCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)).isEmpty() || mc.player.verticalCollision)
                    stage = 1;

                speed = distance - distance / 159.0;
            }

            speed = Math.max(speed, MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED));

            double ncp = MovementUtils.getPotionSpeed(mode.getValue().equalsIgnoreCase("StrafeStrict") || mc.player.input.movementForward < 1 ? 0.465 : 0.576);
            double bypass = MovementUtils.getPotionSpeed(mode.getValue().equalsIgnoreCase("StrafeStrict") || mc.player.input.movementForward < 1 ? 0.44 : 0.57);

            speed = Math.min(speed, ticks > 25 ? ncp : bypass);

            if (ticks++ > 50) ticks = 0;

            Vector2d velocity = MovementUtils.forward(speed);
            event.setMovement(new Vec3d(velocity.x, event.getMovement().getY(), event.getMovement().getZ()));
            event.setMovement(new Vec3d(event.getMovement().getX(), event.getMovement().getY(), velocity.y));
            forward = mc.player.input.movementForward;

            event.setCancelled(true);
        }
    }

    @Override
    public String getMetaData() {
        return mode.getValue();
    }
}
