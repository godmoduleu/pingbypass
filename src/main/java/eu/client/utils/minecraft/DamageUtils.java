package eu.client.utils.minecraft;

import eu.client.utils.IMinecraft;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.Map;

public class DamageUtils implements IMinecraft {
    private static final Map<String, Integer> PROTECTION_MAP = new HashMap<>() {{
        put("protection", 1);
        put("blast_protection", 2);
        put("projectile_protection", 1);
        put("feather_falling", 1);
        put("fire_protection", 1);
    }};

    public static float getCrystalDamage(Entity entity, Box box, EndCrystalEntity crystal, boolean ignoreTerrain) {
        return getDamage(entity, box, Vec3d.ofCenter(crystal.getBlockPos(), 0), 6.0f, null, ignoreTerrain);
    }

    public static float getCrystalDamage(Entity entity, Box box, BlockPos position, BlockPos exception, boolean ignoreTerrain) {
        return getDamage(entity, box, Vec3d.ofCenter(position, 1), 6.0f, exception, ignoreTerrain);
    }

    public static float getDamage(Entity entity, Box box, Vec3d vec3d, float power, BlockPos exception, boolean ignoreTerrain) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL) return 0.0f;
        if (!(entity instanceof OtherClientPlayerEntity) && entity instanceof PlayerEntity player && EntityUtils.getGameMode(player) == GameMode.CREATIVE) return 0.0f;

        float diameter = power * 2.0f;

        double distance = Math.sqrt(box != null ? box.getCenter().add(0, -0.9, 0).squaredDistanceTo(vec3d) : entity.squaredDistanceTo(vec3d)) / diameter;
        if (distance > 1.0) return 0.0f;

        double exposure = (1.0 - distance) * getExposure(vec3d, entity.getBoundingBox(), exception, ignoreTerrain);
        float damage = (int) ((exposure * exposure + exposure) / 2.0 * 7.0 * diameter + 1.0);

        if (damage <= 0.0f) return 0.0f;

        if (entity instanceof LivingEntity livingEntity) {
            damage = mc.world.getDifficulty() == Difficulty.EASY ? Math.min(damage / 2 + 1, damage) : mc.world.getDifficulty() == Difficulty.HARD ? damage * 3 / 2 : damage;
            damage = DamageUtil.getDamageLeft(livingEntity, damage, mc.world.getDamageSources().explosion(null), (float) livingEntity.getArmor(), (float) livingEntity.getAttributeInstance(EntityAttributes.ARMOR_TOUGHNESS).getValue());
            damage *= livingEntity.hasStatusEffect(StatusEffects.RESISTANCE) ? 1 - ((livingEntity.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1) * 0.2) : 1;
            damage = DamageUtil.getInflictedDamage(damage, getProtectionAmount(livingEntity.getArmorItems()));
        }

        return Math.max(damage, 0.0f);
    }

    public static int getProtectionAmount(Iterable<ItemStack> armor) {
        int x = 0;
        for (ItemStack stack : armor) {
            x += getProtectionAmount(stack);
        }

        return x;
    }

    public static int getProtectionAmount(ItemStack armor) {
        int x = 0;
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(armor);
        for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
            String id = enchantment.getIdAsString().replace("minecraft:", "");
            if (PROTECTION_MAP.containsKey(id)) {
                x += enchantments.getLevel(enchantment) * PROTECTION_MAP.get(id);
                break;
            }
        }

        return x;
    }

    private static float getExposure(Vec3d source, Box box, BlockPos exception, boolean ignoreTerrain) {
        int hitCount = 0;
        int count = 0;

        for (double k = 0.0; k <= 1.0; k += 0.4545454446934474) {
            for (double l = 0.0; l <= 1.0; l += 0.21739130885479366) {
                for (double m = 0.0; m <= 1.0; m += 0.4545454446934474) {
                    Vec3d vec3d = new Vec3d(MathHelper.lerp(k, box.minX, box.maxX) + 0.045454555306552624, MathHelper.lerp(l, box.minY, box.maxY), MathHelper.lerp(m, box.minZ, box.maxZ) + 0.045454555306552624);
                    if (raycast(vec3d, source, exception, ignoreTerrain) == HitResult.Type.MISS) ++hitCount;
                    ++count;
                }
            }
        }

        return (float) hitCount / (float) count;
    }

    private static HitResult.Type raycast(Vec3d start, Vec3d end, BlockPos exception, boolean ignoreTerrain) {
        return BlockView.raycast(start, end, null, (innerContext, blockPos) -> {
            BlockState blockState;
            if (blockPos.equals(exception)) {
                blockState = Blocks.AIR.getDefaultState();
            } else {
                blockState = mc.world.getBlockState(blockPos);
                if (blockState.getBlock().getBlastResistance() < 600 && ignoreTerrain) blockState = Blocks.AIR.getDefaultState();
            }

            BlockHitResult hitResult = blockState.getCollisionShape(mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (innerContext) -> HitResult.Type.MISS);
    }
}
