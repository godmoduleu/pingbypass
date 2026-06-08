package eu.client.utils.minecraft;

import eu.client.EUClient;
import eu.client.modules.impl.miscellaneous.FakePlayerModule;
import eu.client.utils.IMinecraft;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EntityUtils implements IMinecraft {
    public static Map<StatusEffect, Color> POTION_COLORS = new HashMap<>();

    static {
        POTION_COLORS.put(StatusEffects.SPEED.value(), new Color(124, 175, 198));
        POTION_COLORS.put(StatusEffects.SLOWNESS.value(), new Color(90, 108, 129));
        POTION_COLORS.put(StatusEffects.HASTE.value(), new Color(217, 192, 67));
        POTION_COLORS.put(StatusEffects.MINING_FATIGUE.value(), new Color(74, 66, 23));
        POTION_COLORS.put(StatusEffects.STRENGTH.value(), new Color(147, 36, 35));
        POTION_COLORS.put(StatusEffects.INSTANT_HEALTH.value(), new Color(67, 10, 9));
        POTION_COLORS.put(StatusEffects.INSTANT_DAMAGE.value(), new Color(67, 10, 9));
        POTION_COLORS.put(StatusEffects.JUMP_BOOST.value(), new Color(34, 255, 76));
        POTION_COLORS.put(StatusEffects.NAUSEA.value(), new Color(85, 29, 74));
        POTION_COLORS.put(StatusEffects.REGENERATION.value(), new Color(205, 92, 171));
        POTION_COLORS.put(StatusEffects.RESISTANCE.value(), new Color(153, 69, 58));
        POTION_COLORS.put(StatusEffects.FIRE_RESISTANCE.value(), new Color(228, 154, 58));
        POTION_COLORS.put(StatusEffects.WATER_BREATHING.value(), new Color(46, 82, 153));
        POTION_COLORS.put(StatusEffects.INVISIBILITY.value(), new Color(127, 131, 146));
        POTION_COLORS.put(StatusEffects.BLINDNESS.value(), new Color(31, 31, 35));
        POTION_COLORS.put(StatusEffects.NIGHT_VISION.value(), new Color(31, 31, 161));
        POTION_COLORS.put(StatusEffects.HUNGER.value(), new Color(88, 118, 83));
        POTION_COLORS.put(StatusEffects.WEAKNESS.value(), new Color(72, 77, 72));
        POTION_COLORS.put(StatusEffects.POISON.value(), new Color(78, 147, 49));
        POTION_COLORS.put(StatusEffects.WITHER.value(), new Color(53, 42, 39));
        POTION_COLORS.put(StatusEffects.HEALTH_BOOST.value(), new Color(248, 125, 35));
        POTION_COLORS.put(StatusEffects.ABSORPTION.value(), new Color(37, 82, 165));
        POTION_COLORS.put(StatusEffects.SATURATION.value(), new Color(248, 36, 35));
        POTION_COLORS.put(StatusEffects.GLOWING.value(), new Color(148, 160, 97));
        POTION_COLORS.put(StatusEffects.LEVITATION.value(), new Color(206, 255, 255));
        POTION_COLORS.put(StatusEffects.LUCK.value(), new Color(51, 153, 0));
        POTION_COLORS.put(StatusEffects.UNLUCK.value(), new Color(192, 164, 77));
    }

    public static boolean isBot(PlayerEntity player) {
        if (EUClient.MODULE_MANAGER.getModule(FakePlayerModule.class).isToggled() && player == EUClient.MODULE_MANAGER.getModule(FakePlayerModule.class).getPlayer()) {
            return false;
        }

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry == null || entry.getProfile() == null || player.getUuid().toString().startsWith(player.getName().getString()) || !player.getGameProfile().getName().equals(player.getName().getString());
    }

    public static int getLatency(PlayerEntity player) {
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return playerListEntry == null ? 0 : playerListEntry.getLatency();
    }

    public static GameMode getGameMode(PlayerEntity player) {
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return playerListEntry == null ? GameMode.CREATIVE : playerListEntry.getGameMode();
    }

    public static String getGameModeName(GameMode gameMode) {
        return switch (gameMode) {
            case CREATIVE -> "C";
            case ADVENTURE -> "A";
            case SPECTATOR -> "SP";
            default -> "S";
        };
    }

    public static double getSpeed(Entity entity, SpeedUnit unit) {
        double speed = Math.sqrt(MathHelper.square(Math.abs(entity.getX() - entity.lastRenderX)) + MathHelper.square(Math.abs(entity.getZ() - entity.lastRenderZ)));

        if (unit == SpeedUnit.KILOMETERS) return (speed * 3.6 * EUClient.WORLD_MANAGER.getTimerMultiplier()) * 20;
        else return speed / 0.05 * EUClient.WORLD_MANAGER.getTimerMultiplier();
    }

    public static boolean hasNegativeEffects(PlayerEntity player) {
        for (StatusEffectInstance statusEffectInstance : new ArrayList<>(player.getStatusEffects())) {
            if (!statusEffectInstance.getEffectType().value().isBeneficial()) return true;
        }

        return false;
    }

    public static Vec3d getRenderPos(Entity entity, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
        return new Vec3d(x, y, z);
    }

    public static LivingEntity getClosestEntity(Entity entity) {
        LivingEntity closestEntity = null;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity livingEntity)) continue;

            if (!(entity.distanceTo(livingEntity) <= 10.0f)) continue;
            if (livingEntity.getHealth() <= 0.0f || !livingEntity.isAlive()) continue;
            if (entity == livingEntity) continue;

            if (closestEntity == null) {
                closestEntity = livingEntity;
                continue;
            }

            if (entity.distanceTo(livingEntity) < entity.distanceTo(closestEntity)) {
                closestEntity = livingEntity;
            }
        }

        return closestEntity;
    }

    public static Direction getPearlDirection(EnderPearlEntity pearl) {
        Direction direction = pearl.getHorizontalFacing();

        if (direction.equals(Direction.WEST)) return Direction.EAST;
        else if (direction.equals(Direction.EAST)) return Direction.WEST;

        return direction;
    }

    public static boolean isThrowable(Item item) {
        return item instanceof EnderPearlItem || item instanceof TridentItem || item instanceof ExperienceBottleItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem;
    }

    public static boolean isInWeb(Entity entity) {
        for (float x : new float[]{0, 0.3F, -0.3f}) {
            for (float z : new float[]{0, 0.3F, -0.3f}) {
                for (int y : new int[]{-1, 0, 1, 2}) {
                    BlockPos pos = BlockPos.ofFloored(entity.getX() + x, entity.getY(), entity.getZ() + z).up(y);
                    if (new Box(pos).intersects(entity.getBoundingBox()) && mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public enum SpeedUnit {
        METERS, KILOMETERS
    }
}
