package eu.client.utils.minecraft;

import com.google.common.collect.Sets;
import eu.client.Pingbypass;
import eu.client.utils.IMinecraft;
import eu.client.utils.rotations.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class WorldUtils implements IMinecraft {
    public static Set<Block> RIGHT_CLICKABLE_BLOCKS = Sets.newHashSet(Blocks.CHEST, Blocks.TRAPPED_CHEST,
            Blocks.ENDER_CHEST, Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX,
            Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.PINK_SHULKER_BOX,
            Blocks.GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX,
            Blocks.BLACK_SHULKER_BOX, Blocks.ANVIL, Blocks.BELL, Blocks.OAK_BUTTON, Blocks.ACACIA_BUTTON,
            Blocks.BIRCH_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.JUNGLE_BUTTON, Blocks.SPRUCE_BUTTON,
            Blocks.STONE_BUTTON, Blocks.COMPARATOR, Blocks.REPEATER, Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE,
            Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.ACACIA_FENCE_GATE,
            Blocks.BREWING_STAND, Blocks.DISPENSER, Blocks.DROPPER, Blocks.LEVER, Blocks.NOTE_BLOCK, Blocks.JUKEBOX,
            Blocks.BEACON, Blocks.BLACK_BED, Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.CYAN_BED, Blocks.GRAY_BED,
            Blocks.GREEN_BED, Blocks.LIGHT_BLUE_BED, Blocks.LIGHT_GRAY_BED, Blocks.LIME_BED, Blocks.MAGENTA_BED,
            Blocks.ORANGE_BED, Blocks.PINK_BED, Blocks.PURPLE_BED, Blocks.RED_BED, Blocks.WHITE_BED, Blocks.YELLOW_BED,
            Blocks.FURNACE, Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
            Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.CAKE, Blocks.ENCHANTING_TABLE, Blocks.DRAGON_EGG,
            Blocks.HOPPER, Blocks.REPEATING_COMMAND_BLOCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.CRAFTING_TABLE, Blocks.ACACIA_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
            Blocks.JUNGLE_TRAPDOOR, Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.CAKE, Blocks.ACACIA_SIGN,
            Blocks.ACACIA_WALL_SIGN, Blocks.BIRCH_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.DARK_OAK_SIGN,
            Blocks.DARK_OAK_WALL_SIGN, Blocks.JUNGLE_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.OAK_SIGN,
            Blocks.OAK_WALL_SIGN, Blocks.SPRUCE_SIGN, Blocks.SPRUCE_WALL_SIGN, Blocks.CRIMSON_SIGN,
            Blocks.CRIMSON_WALL_SIGN, Blocks.WARPED_SIGN, Blocks.WARPED_WALL_SIGN, Blocks.BLAST_FURNACE, Blocks.SMOKER,
            Blocks.CARTOGRAPHY_TABLE, Blocks.GRINDSTONE, Blocks.LECTERN, Blocks.LOOM, Blocks.STONECUTTER,
            Blocks.SMITHING_TABLE);
    private static final ItemStack NETHERITE_PICKAXE = new ItemStack(Items.NETHERITE_PICKAXE);

    public static void placeBlock(BlockPos position, Direction direction, Hand hand, boolean rotate,
            boolean crystalDestruction) {
        placeBlock(position, direction, hand, rotate, crystalDestruction, false);
    }

    public static void placeBlock(BlockPos position, Direction direction, Hand hand, boolean rotate,
            boolean crystalDestruction, boolean render) {
        placeBlock(position, direction, hand, null, rotate, crystalDestruction, render);
    }

    public static void placeBlock(BlockPos position, Direction direction, Hand hand, Runnable runnable, boolean rotate,
            boolean crystalDestruction, boolean render) {
        Vec3d vec3d = position.toCenterPos();
        BlockPos offsetPosition;

        if (direction == null) {
            direction = Direction.UP;
            offsetPosition = position;
        } else {
            offsetPosition = position.offset(direction);
            vec3d = vec3d.add(direction.getOffsetX() / 2.0, direction.getOffsetY() / 2.0, direction.getOffsetZ() / 2.0);
        }

        float prevYaw = Pingbypass.ROTATION_MANAGER.getServerYaw();
        float prevPitch = Pingbypass.ROTATION_MANAGER.getServerPitch();

        if (rotate)
            Pingbypass.ROTATION_MANAGER
                    .packetRotate(RotationUtils.getRotations(vec3d.getX(), vec3d.getY(), vec3d.getZ()));
        if (crystalDestruction)
            destroyCrystals(position);
        if (runnable != null)
            runnable.run();

        boolean sprint = mc.player.isSprinting();
        boolean sneak = WorldUtils.RIGHT_CLICKABLE_BLOCKS.contains(mc.world.getBlockState(offsetPosition).getBlock())
                && !mc.player.isSneaking();

        // On the proxy, skip sprint stop/start — the client controls sprint state.
        // This saves 2 packets per block placement and significantly speeds up
        // Surround.
        boolean isProxy = eu.client.pingbypass.PingBypassFlags.proxyForwardingActive
                && Pingbypass.PINGBYPASS_CONFIG != null && Pingbypass.PINGBYPASS_CONFIG.isServer();
        if (!isProxy && sprint)
            mc.player.networkHandler
                    .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        if (sneak)
            mc.player.networkHandler
                    .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));

        BlockHitResult blockHitResult = new BlockHitResult(vec3d, direction.getOpposite(), offsetPosition, false);
        NetworkUtils.sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(hand, blockHitResult, sequence));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        if (rotate)
            Pingbypass.ROTATION_MANAGER.packetRotate(prevYaw, prevPitch);

        Pingbypass.WORLD_MANAGER.getPlaceTimer().reset();

        if (sneak)
            mc.player.networkHandler
                    .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        if (!isProxy && sprint)
            mc.player.networkHandler
                    .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

        if (render) {
            // Render features removed for proxy
            // We just sync to proxy client
            if (eu.client.pingbypass.PingBypassFlags.proxyForwardingActive
                    && Pingbypass.PINGBYPASS_CONFIG != null && Pingbypass.PINGBYPASS_CONFIG.isServer()
                    && Pingbypass.PROXY_SERVER != null) {
                var packet = new net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket(
                        eu.client.pingbypass.protocol.PbCustomPayload.fromPacket(
                                new eu.client.pingbypass.protocol.packets.S2CBlockRenderPacket(position)));
                for (net.minecraft.network.ClientConnection conn : Pingbypass.PROXY_SERVER.getConnections()) {
                    if (conn.isOpen())
                        conn.send(packet);
                }
            }
        }
    }

    public static boolean isPlaceable(BlockPos position) {
        return isPlaceable(position, false);
    }

    public static boolean isPlaceable(BlockPos position, boolean excludeSelf) {
        if (!mc.world.getBlockState(position).isReplaceable())
            return false;
        return mc.world.getOtherEntities(null, new Box(position)).stream()
                .noneMatch(entity -> !(entity instanceof EndCrystalEntity) && !(entity instanceof ExperienceOrbEntity)
                        && !(entity instanceof ItemEntity) && !(entity.equals(mc.player) && excludeSelf));
    }

    public static boolean isCrystalPlaceable(BlockPos position) {
        if (!mc.world.getBlockState(position).isReplaceable())
            return false;
        return mc.world.getOtherEntities(null, new Box(position)).stream()
                .noneMatch(entity -> !(entity instanceof EndCrystalEntity) && !(entity instanceof ExperienceOrbEntity));
    }

    public static void destroyCrystals(BlockPos position) {
        List<Entity> surroundingCrystals = mc.world.getOtherEntities(null, new Box(position)).stream()
                .filter(entity -> entity instanceof EndCrystalEntity).toList();
        if (surroundingCrystals.isEmpty())
            return;

        for (Entity entity : surroundingCrystals) {
            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            break;
        }
    }

    public static Vec3d getHitVector(BlockPos position, Direction direction) {
        return position.toCenterPos().add(direction.getOffsetX() / 2.0, direction.getOffsetY() / 2.0,
                direction.getOffsetZ() / 2.0);
    }

    public static Direction getClosestDirection(BlockPos position, boolean strictDirection) {
        if (strictDirection) {
            if (mc.player.getY() >= position.getY())
                return Direction.UP;

            BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), Vec3d.ofCenter(position),
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (result == null || result.getType() != HitResult.Type.BLOCK || result.getSide() == null) {
                return getClosestDirection(position);
            }

            return result.getSide();
        } else {
            return getClosestDirection(position);
        }
    }

    private static Direction getClosestDirection(BlockPos position) {
        Direction closestDirection = null;
        Vec3d offsetPosition = null;

        for (Direction direction : Direction.values()) {
            Vec3d newOffset = getHitVector(position, direction);
            if (closestDirection == null) {
                closestDirection = direction;
                offsetPosition = newOffset;
                continue;
            }

            if (mc.player.squaredDistanceTo(newOffset) < mc.player.squaredDistanceTo(offsetPosition)) {
                closestDirection = direction;
                offsetPosition = newOffset;
            }
        }

        return closestDirection;
    }

    public static Direction getDirection(BlockPos position, boolean strictDirection) {
        return getDirection(position, null, strictDirection);
    }

    public static Direction getDirection(BlockPos position, List<BlockPos> exceptions, boolean strictDirection) {
        List<Direction> strictDirections = new ArrayList<>();
        if (strictDirection)
            strictDirections = getStrictDirections(mc.player.getEyePos(), Vec3d.ofCenter(position));

        Direction fallback = null;
        for (Direction direction : Direction.values()) {
            BlockPos offset = position.offset(direction);
            if (strictDirection && !strictDirections.contains(direction.getOpposite()))
                continue;
            if (mc.world.getBlockState(offset).isReplaceable()
                    && (exceptions == null || !exceptions.contains(offset))) {
                continue;
            }

            // Prefer non-interactable blocks to avoid opening containers
            if (RIGHT_CLICKABLE_BLOCKS.contains(mc.world.getBlockState(offset).getBlock())) {
                if (fallback == null)
                    fallback = direction;
                continue;
            }

            return direction;
        }

        // Fall back to interactable block if no other option (sneak packet will handle
        // it)
        return fallback;
    }

    public static List<Direction> getStrictDirections(Vec3d eyePos, Vec3d blockPos) {
        List<Direction> directions = new ArrayList<>();

        double differenceX = eyePos.getX() - blockPos.getX();
        double differenceY = eyePos.getY() - blockPos.getY();
        double differenceZ = eyePos.getZ() - blockPos.getZ();

        if (differenceY > 0.5) {
            directions.add(Direction.UP);
        } else if (differenceY < -0.5) {
            directions.add(Direction.DOWN);
        } else {
            directions.add(Direction.UP);
            directions.add(Direction.DOWN);
        }

        if (differenceX > 0.5) {
            directions.add(Direction.EAST);
        } else if (differenceX < -0.5) {
            directions.add(Direction.WEST);
        } else {
            directions.add(Direction.EAST);
            directions.add(Direction.WEST);
        }

        if (differenceZ > 0.5) {
            directions.add(Direction.SOUTH);
        } else if (differenceZ < -0.5) {
            directions.add(Direction.NORTH);
        } else {
            directions.add(Direction.SOUTH);
            directions.add(Direction.NORTH);
        }

        return directions;
    }

    public static HitResult getRaytraceTarget(float yaw, float pitch, double x, double y, double z) {
        Vec3d rotationVector = new Vec3d(MathHelper.sin(-yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F),
                -MathHelper.sin(pitch * 0.017453292F),
                MathHelper.cos(-yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F));
        HitResult result = mc.world.raycast(new RaycastContext(new Vec3d(x, y, z),
                new Vec3d(x + rotationVector.x * 5, y + rotationVector.y * 5, z + rotationVector.z * 5),
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

        Vec3d vec3d = new Vec3d(x, y + mc.player.getEyeHeight(mc.player.getPose()), z);
        double distance = 25;
        if (result != null)
            distance = result.getPos().squaredDistanceTo(vec3d);

        Vec3d multipliedVector = vec3d.add(rotationVector.x * 5, rotationVector.y * 5, rotationVector.z * 5);
        Box box = new Box(x - .3, y, z - .3, x + .3, y + 1.8, z + .3).stretch(rotationVector.multiply(5)).expand(1.0,
                1.0, 1.0);

        EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, vec3d, multipliedVector, box,
                (entity) -> !entity.isSpectator() && entity.canHit(), distance);
        if (entityHitResult != null) {
            if (vec3d.squaredDistanceTo(entityHitResult.getPos()) < distance || result == null) {
                if (entityHitResult.getEntity() instanceof LivingEntity) {
                    return entityHitResult;
                }
            }
        }

        return result;
    }

    public static boolean canSee(Entity entity) {
        return canSee(entity.getX(), entity.getY(), entity.getZ());
    }

    public static boolean canSee(BlockPos position) {
        return canSee(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
    }

    public static boolean canSee(Vec3d vec3d) {
        return canSee(vec3d.getX(), vec3d.getY(), vec3d.getZ());
    }

    public static boolean canSee(double x, double y, double z) {
        return mc.world.raycast(new RaycastContext(new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()),
                new Vec3d(x, y, z), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player))
                .getType() == HitResult.Type.MISS;
    }

    public static boolean canBreak(BlockPos... pos) {
        return Arrays.stream(pos).allMatch(blockPos -> mc.world.getBlockState(blockPos).getBlock().getHardness() != -1);
    }

    public static boolean isReplaceable(BlockPos... pos) {
        return Arrays.stream(pos).allMatch(blockPos -> mc.world.getBlockState(blockPos).isReplaceable());
    }

    public static Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }

    public static int getNetherPosition(int position) {
        return mc.player.getWorld().getRegistryKey() == World.NETHER ? position * 8 : position / 8;
    }

    public static String getMovementDirection(Direction direction) {
        if (direction.getName().equalsIgnoreCase("North"))
            return "-Z";
        if (direction.getName().equalsIgnoreCase("East"))
            return "+X";
        if (direction.getName().equalsIgnoreCase("South"))
            return "+Z";
        if (direction.getName().equalsIgnoreCase("West"))
            return "-X";
        return "N/A";
    }

    public static float getBreakTime(PlayerEntity player, BlockState blockState) {
        if (player == null)
            return 0.0f;

        float speed = NETHERITE_PICKAXE.getMiningSpeedMultiplier(blockState) + 26;
        return (1.0f / (speed / blockState.getBlock().getHardness() / 30)) * 50f;
    }

    public static double getBreakDelta(BlockState blockState, int slot) {
        if (slot == -1)
            return 0.0f;
        float speed = mc.player.getInventory().main.get(slot).getMiningSpeedMultiplier(blockState);

        if (speed > 1.0f) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            int efficiency = EnchantmentHelper.getLevel(mc.world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), stack);
            if (efficiency > 0 && !stack.isEmpty())
                speed += efficiency * efficiency + 1;
        }

        if (StatusEffectUtil.hasHaste(mc.player))
            speed *= 1.0f + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2f;
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            speed *= (switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1E-4f;
            });
        }

        if (mc.player.isSubmergedIn(FluidTags.WATER)
                && !(EnchantmentHelper.getEquipmentLevel(mc.world.getRegistryManager()
                        .getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.AQUA_AFFINITY), mc.player) > 0))
            speed /= 5.0f;
        if (!mc.player.isOnGround())
            speed /= 5.0f;

        return speed / blockState.getBlock().getHardness()
                / (!blockState.isToolRequired() || mc.player.getInventory().main.get(slot).isSuitableFor(blockState)
                        ? 30
                        : 100);
    }

    public static float getMineSpeed(BlockState state, int slot) {
        if (mc.player == null)
            return 0;
        float speed = mc.player.getInventory().main.get(slot).getMiningSpeedMultiplier(state);

        if (speed > 1) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            int efficiency = EnchantmentHelper.getLevel(mc.world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), stack);
            if (efficiency > 0 && !stack.isEmpty())
                speed += (float) (StrictMath.pow(efficiency, 2) + 1);
        }

        if (mc.player.hasStatusEffect(StatusEffects.HASTE))
            speed *= 1 + (mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE))
            speed *= (float) Math.pow(0.3f, mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() + 1);
        if (mc.player.isSubmergedIn(FluidTags.WATER))
            speed *= (float) mc.player.getAttributeInstance(EntityAttributes.SUBMERGED_MINING_SPEED).getValue();
        if (!mc.player.isOnGround())
            speed /= 5;

        speed = speed < 0 ? 0 : speed;
        return speed / state.getBlock().getHardness()
                / (!state.isToolRequired() || mc.player.getInventory().main.get(slot).isSuitableFor(state) ? 30 : 100);
    }

    public static boolean blocksMovement(BlockState state) {
        return state.getBlock() != Blocks.COBWEB && state.getBlock() != Blocks.BAMBOO_SAPLING && !state.isReplaceable();
    }

    public static boolean equals(BlockPos x, BlockPos y) {
        if (x == null && y == null) {
            return true;
        } else if (x == null || y == null) {
            return false;
        } else {
            return x.equals(y);
        }
    }

    public static String getDimension() {
        return mc.player.getWorld().getRegistryKey().getValue().toString().replace("minecraft:", "");
    }

    public static List<PlayerEntity> getCollisions(BlockPos pos) {
        List<PlayerEntity> collisions = new ArrayList<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.isDead())
                continue;
            if (player.getBoundingBox().intersects(new Box(pos)))
                collisions.add(player);
        }
        return collisions;
    }
}
