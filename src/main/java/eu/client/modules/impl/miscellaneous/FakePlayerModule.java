package eu.client.modules.impl.miscellaneous;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.*;
import eu.client.utils.minecraft.WorldUtils;
import eu.client.utils.rotations.RotationUtils;
import eu.client.utils.system.MathUtils;
import eu.client.utils.system.Timer;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

@RegisterModule(name = "FakePlayer", description = "Spawns in a fake player entity that you can use to test modules on.", category = Module.Category.MISCELLANEOUS, proxyEnhanced = true)
public class FakePlayerModule extends Module {
    public StringSetting name = new StringSetting("Name", "The name that will be assigned to the fake player.", "Dummy");
    public NumberSetting health = new NumberSetting("Health", "The amount of health that will be assigned to the fake player.", 20.0f, 1.0f, 20.0f);
    public NumberSetting absorption = new NumberSetting("Absorption", "The amount of absorption that will be assigned to the fake player.", 16, 0, 16);

    public CategorySetting movementCategory = new CategorySetting("Movement", "The category that contains settings related to movement.");
    public ModeSetting movementMode = new ModeSetting("Movement", "Mode", "The mode that will be used for the fake player's movement.", new CategorySetting.Visibility(movementCategory), "None", new String[]{"None", "Random"});
    public NumberSetting velocity = new NumberSetting("Velocity", "Velocity", "The velocity to apply on the fake player.", new CategorySetting.Visibility(movementCategory), 0.3f, 0.1f, 0.4f);
    public BooleanSetting changeDirection = new BooleanSetting("ChangeDirection", "Changes the player's direction when a specified amount of time has passed.", new CategorySetting.Visibility(movementCategory), false);
    public NumberSetting timeout = new NumberSetting("Timeout", "The amount of time that it takes for the player to change direction.", new BooleanSetting.Visibility(changeDirection, true), 5, 1, 15);

    @Getter private OtherClientPlayerEntity player = null;

    private double[] direction = generateDirection();

    private final Timer timer = new Timer();

    private boolean stepping = false;
    private final Timer stepTimer = new Timer();

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (player == null) return;
        if (!movementMode.getValue().equalsIgnoreCase("Random")) return;

        BlockPos position = player.getBlockPos();
        boolean changeDirection = false;

        if (this.changeDirection.getValue() && timer.hasTimeElapsed(timeout.getValue().longValue() * 1000L)) {
            changeDirection = true;
            timer.reset();
        }

        if (hasObstruction(position)) {
            for (Direction direction : Direction.values()) {
                BlockPos offsetPosition = position.offset(direction);
                if ((WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition)) || WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition.up()))) && WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition.up().up())) && player.getHorizontalFacing().equals(direction)) {
                    changeDirection = true;
                    timer.reset();
                }
            }
        }

        if (changeDirection) {
            double[] newDirection = generateDirection();
            if (direction == newDirection) newDirection = generateDirection();

            direction = newDirection;
        }

        if (stepping && stepTimer.hasTimeElapsed(500L)) {
            stepping = false;
            stepTimer.reset();
        }

        player.jump();

        float[] rotations = RotationUtils.getRotations(player, player.getX() + direction[0], player.getY() + fixAxisY(player), player.getZ() + direction[1]);

        player.setYaw(rotations[0]);
        player.headYaw = rotations[0];
        player.setPitch(0.0f);

        player.setPosition(player.getX() + direction[0], player.getY() + fixAxisY(player), player.getZ() + direction[1]);
    }

    @Override
    public void onEnable() {
        if (mc.world == null) {
            setToggled(false);
            return;
        }

        player = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), name.getValue()));
        player.copyPositionAndRotation(mc.player);
        player.setId(-673);
        player.copyFrom(mc.player);
        player.setHealth(health.getValue().floatValue());
        player.setAbsorptionAmount(absorption.getValue().floatValue());

        NbtCompound compoundTag = new NbtCompound();
        mc.player.writeCustomDataToNbt(compoundTag);
        player.readCustomDataFromNbt(compoundTag);

        mc.world.addEntity(player);

        player.tick();
        timer.reset();
    }

    @Override
    public void onDisable() {
        if (mc.world == null || player == null) return;
        mc.world.removeEntity(player.getId(), Entity.RemovalReason.DISCARDED);
    }

    public boolean hasObstruction(BlockPos position) {
        for (Direction direction : Direction.values()) {
            BlockPos offsetPosition = position.offset(direction);
            if (WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition))) return true;
            if (WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition.up()))) return true;
        }

        return false;
    }

    public float fixAxisY(PlayerEntity player) {
        if (mc.world.getBlockState(player.getBlockPos().down()).getBlock() == Blocks.AIR && !stepping) return -1;

        if (hasObstruction(player.getBlockPos())) {
            for (Direction direction : Direction.values()) {
                BlockPos offsetPosition = player.getBlockPos().offset(direction);

                if (WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition)) && !WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition.up())) && player.getHorizontalFacing().equals(direction) && !WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition.up().up()))) {
                    stepping = true;
                    stepTimer.reset();

                    return 1;
                }

                if (WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition.up())) && player.getHorizontalFacing().equals(direction) && !WorldUtils.blocksMovement(mc.world.getBlockState(offsetPosition.up().up()))) {
                    stepping = true;
                    stepTimer.reset();

                    return 2;
                }
            }

            return 0;
        }

        return 0;
    }

    public double[] generateDirection() {
        double angle = MathUtils.random(2 * Math.PI, 0);
        double[] direction = new double[]{-Math.sin(angle), Math.cos(angle)};

        return new double[]{direction[0] * velocity.getValue().floatValue(), direction[1] * velocity.getValue().floatValue()};
    }
}
