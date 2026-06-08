package eu.client.utils.minecraft;

import eu.client.Pingbypass;
import eu.client.utils.IMinecraft;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HoleUtils implements IMinecraft {
    private static final Vec3i[] holeOffsets = new Vec3i[] { new Vec3i(0, -1, 0), new Vec3i(1, 0, 0),
            new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1) };
    private static final Vec3i[] fullTrapOffsets = new Vec3i[] { new Vec3i(1, 1, 0), new Vec3i(0, 1, 1),
            new Vec3i(-1, 1, 0), new Vec3i(0, 1, -1), new Vec3i(1, 2, 0), new Vec3i(0, 2, 0) };

    private static final Vec3i[] singleOffsets = { new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0), new Vec3i(0, 0, -1),
            new Vec3i(0, 0, 1), new Vec3i(0, -1, 0) };
    private static final Vec3i[] doubleXOffsets = { new Vec3i(-1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, 0, 1),
            new Vec3i(0, -1, 0), new Vec3i(2, 0, 0), new Vec3i(1, 0, -1), new Vec3i(1, 0, 1), new Vec3i(1, -1, 0) };
    private static final Vec3i[] doubleZOffsets = { new Vec3i(0, 0, -1), new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0),
            new Vec3i(0, -1, 0), new Vec3i(0, 0, 2), new Vec3i(-1, 0, 1), new Vec3i(1, 0, 1), new Vec3i(0, -1, 1) };
    private static final Vec3i[] quadOffsets = { new Vec3i(-1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, -1, 0),
            new Vec3i(2, 0, 0), new Vec3i(1, 0, -1), new Vec3i(1, -1, 0), new Vec3i(-1, 0, 1), new Vec3i(0, 0, 2),
            new Vec3i(0, -1, 1), new Vec3i(2, 0, 1), new Vec3i(1, 0, 2), new Vec3i(1, -1, 1) };

    public static boolean isPlayerInHole(PlayerEntity player) {
        return HoleUtils.getFeetPositions(player, true, true, false).stream()
                .noneMatch(position -> mc.world.getBlockState(position).isReplaceable());
    }

    public static List<BlockPos> getInsidePositions(Entity targetEntity) {
        List<BlockPos> targetPositions = new ArrayList<>();
        BlockPos targetPosition = PositionUtils.getFlooredPosition(targetEntity);

        for (Vec3i vec3i : holeOffsets) {
            if (!(vec3i.getY() < targetPosition.getY()))
                continue;
            BlockPos offsetPosition = targetPosition.add(vec3i);

            List<Entity> collidingEntities = mc.world.getOtherEntities(null, new Box(offsetPosition)).stream()
                    .filter(entity -> entity == targetEntity).toList();
            if (collidingEntities.isEmpty())
                continue;

            Box box = collidingEntities.getFirst().getBoundingBox();

            for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    BlockPos pos = new BlockPos(x, targetPosition.getY(), z);
                    if (!targetPositions.contains(pos))
                        targetPositions.add(pos);
                }
            }
        }
        if (targetPositions.isEmpty())
            targetPositions.add(targetPosition);

        return targetPositions;
    }

    public static HashSet<BlockPos> getFeetPositions(PlayerEntity target, boolean extension, boolean floor,
            boolean targetOnly) {
        HashSet<BlockPos> positions = new HashSet<>();
        HashSet<BlockPos> blacklist = new HashSet<>();

        BlockPos feetPos = PositionUtils.getFlooredPosition(target);
        blacklist.add(feetPos);

        if (extension) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis().isVertical())
                    continue;
                BlockPos off = feetPos.offset(dir);

                List<PlayerEntity> collisions = WorldUtils.getCollisions(off);
                if (collisions.isEmpty())
                    continue;

                for (PlayerEntity player : collisions) {
                    if (false)
                        continue;
                    if (targetOnly && player != target)
                        continue;

                    Box box = player.getBoundingBox();
                    for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
                        for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                            blacklist.add(new BlockPos(x, feetPos.getY(), z));
                        }
                    }
                }
            }
        }

        for (BlockPos pos : blacklist) {
            if (floor)
                positions.add(pos.down());

            for (Direction dir : Direction.values()) {
                if (!dir.getAxis().isHorizontal())
                    continue;
                BlockPos off = pos.offset(dir);
                if (!blacklist.contains(off))
                    positions.add(off);
            }
        }

        if (false) {
            List<BlockPos> desyncPositions = new ArrayList<>();

            Vec3d vec3d = mc.player.getBlockPos().toCenterPos();
            boolean flagX = (vec3d.x - mc.player.getX()) > 0;
            boolean flagZ = (vec3d.z - mc.player.getZ()) > 0;

            if (flagX && flagZ) {
                desyncPositions.add(new BlockPos(mc.player.getBlockPos().add(-1, 0, 0)));
                desyncPositions.add(new BlockPos(mc.player.getBlockPos().add(0, 0, -1)));
            }

            if (!flagX && flagZ) {
                desyncPositions.add(new BlockPos(mc.player.getBlockPos().add(1, 0, 0)));
                desyncPositions.add(new BlockPos(mc.player.getBlockPos().add(0, 0, -1)));
            }

            if (flagX && !flagZ) {
                desyncPositions.add(new BlockPos(mc.player.getBlockPos().add(-1, 0, 0)));
                desyncPositions.add(new BlockPos(mc.player.getBlockPos().add(0, 0, 1)));
            }

            if (!flagX && !flagZ) {
                desyncPositions.add(new BlockPos(mc.player.getBlockPos().add(1, 0, 0)));
                desyncPositions.add(new BlockPos(mc.player.getBlockPos().add(0, 0, 1)));
            }

            positions.removeIf(desyncPositions::contains);
        }

        return positions;
    }

    public static List<BlockPos> getTrapPositions(PlayerEntity player, boolean partial, boolean head, boolean antiStep,
            boolean antiBomb, boolean strictDirection) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos position = PositionUtils.getFlooredPosition(player);

        if (antiStep) {
            positions.add(position.add(1, 2, 0));
            positions.add(position.add(-1, 2, 0));
            positions.add(position.add(0, 2, 1));
            positions.add(position.add(0, 2, -1));
        }

        if (antiBomb) {
            positions.add(position.add(0, 3, 0));
        }

        if (partial) {
            BlockPos headPosition = position.add(0, 2, 0);
            if (WorldUtils.getDirection(headPosition, strictDirection) != null) {
                positions.add(headPosition);
                return positions;
            }

            Vec3i[] offsets = new Vec3i[] { new Vec3i(1, 1, 0), new Vec3i(1, 2, 0), new Vec3i(0, 2, 0) };
            for (Vec3i vec3i : offsets)
                positions.add(position.add(vec3i));
        } else {
            for (Vec3i vec3i : fullTrapOffsets) {
                if (!head && vec3i.getY() == 2)
                    continue;
                positions.add(position.add(vec3i));
            }
        }

        return positions;
    }

    public static Hole getSingleHole(BlockPos position, double height) {
        return getSingleHole(position, height, true);
    }

    public static Hole getSingleHole(BlockPos position, double height, boolean reachable) {
        if (!mc.world.getBlockState(position).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.up()).getBlock().equals(Blocks.AIR) && reachable)
            return null;
        if (!mc.world.getBlockState(position.up().up()).getBlock().equals(Blocks.AIR) && reachable)
            return null;

        HoleSafety safety = null;
        for (Vec3i offset : singleOffsets) {
            if (!(mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.BEDROCK)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.OBSIDIAN)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.RESPAWN_ANCHOR)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.ENDER_CHEST))) {
                return null;
            }

            if (mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.BEDROCK)) {
                if (safety == HoleSafety.OBSIDIAN)
                    safety = HoleSafety.MIXED;
                else if (safety != HoleSafety.MIXED)
                    safety = HoleSafety.BEDROCK;
            }

            if (mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.OBSIDIAN)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.RESPAWN_ANCHOR)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.ENDER_CHEST)) {
                if (safety == HoleSafety.BEDROCK)
                    safety = HoleSafety.MIXED;
                else if (safety != HoleSafety.MIXED)
                    safety = HoleSafety.OBSIDIAN;
            }
        }

        if (safety == null)
            safety = HoleSafety.OBSIDIAN;

        return new Hole(new Box(position.getX(), position.getY(), position.getZ(), position.getX() + 1,
                position.getY() + height, position.getZ() + 1), HoleType.SINGLE, safety);
    }

    public static Hole getDoubleHole(BlockPos position, double height) {
        if (!mc.world.getBlockState(position).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.up()).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.up().up()).getBlock().equals(Blocks.AIR))
            return null;

        boolean x = mc.world.getBlockState(position.add(new Vec3i(1, 0, 0))).getBlock().equals(Blocks.AIR)
                && mc.world.getBlockState(position.add(new Vec3i(1, 0, 0)).up()).getBlock().equals(Blocks.AIR)
                && mc.world.getBlockState(position.add(new Vec3i(1, 0, 0)).up().up()).getBlock().equals(Blocks.AIR);
        boolean z = mc.world.getBlockState(position.add(new Vec3i(0, 0, 1))).getBlock().equals(Blocks.AIR)
                && mc.world.getBlockState(position.add(new Vec3i(0, 0, 1)).up()).getBlock().equals(Blocks.AIR)
                && mc.world.getBlockState(position.add(new Vec3i(0, 0, 1)).up().up()).getBlock().equals(Blocks.AIR);

        if (!x && !z)
            return null;

        Box box = null;
        HoleSafety safety = null;

        if (x) {
            boolean valid = true;
            for (Vec3i offset : doubleXOffsets) {
                if (!(mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.BEDROCK)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.OBSIDIAN)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.RESPAWN_ANCHOR)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.ENDER_CHEST))) {
                    valid = false;
                    break;
                }

                if (mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.BEDROCK)) {
                    if (safety == HoleSafety.OBSIDIAN)
                        safety = HoleSafety.MIXED;
                    else if (safety != HoleSafety.MIXED)
                        safety = HoleSafety.BEDROCK;
                }

                if (mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.OBSIDIAN)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.RESPAWN_ANCHOR)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.ENDER_CHEST)) {
                    if (safety == HoleSafety.BEDROCK)
                        safety = HoleSafety.MIXED;
                    else if (safety != HoleSafety.MIXED)
                        safety = HoleSafety.OBSIDIAN;
                }
            }

            if (valid)
                box = new Box(position.getX(), position.getY(), position.getZ(), position.getX() + 2,
                        position.getY() + height, position.getZ() + 1);
        }

        if (z && box == null) {
            boolean valid = true;
            for (Vec3i offset : doubleZOffsets) {
                if (!(mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.BEDROCK)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.OBSIDIAN)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.RESPAWN_ANCHOR)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.ENDER_CHEST))) {
                    valid = false;
                    break;
                }

                if (mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.BEDROCK)) {
                    if (safety == HoleSafety.OBSIDIAN)
                        safety = HoleSafety.MIXED;
                    else if (safety != HoleSafety.MIXED)
                        safety = HoleSafety.BEDROCK;
                }

                if (mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.OBSIDIAN)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.RESPAWN_ANCHOR)
                        || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.ENDER_CHEST)) {
                    if (safety == HoleSafety.BEDROCK)
                        safety = HoleSafety.MIXED;
                    else if (safety != HoleSafety.MIXED)
                        safety = HoleSafety.OBSIDIAN;
                }
            }

            if (valid)
                box = new Box(position.getX(), position.getY(), position.getZ(), position.getX() + 1,
                        position.getY() + height, position.getZ() + 2);
        }

        if (box == null)
            return null;
        if (safety == null)
            safety = HoleSafety.OBSIDIAN;

        return new Hole(box, HoleType.DOUBLE, safety);
    }

    public static Hole getQuadHole(BlockPos position, double height) {
        if (!mc.world.getBlockState(position).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.up()).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.add(new Vec3i(1, 0, 0))).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.add(new Vec3i(0, 0, 1))).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.add(new Vec3i(1, 0, 1))).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.up().up()).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.add(new Vec3i(1, 0, 0)).up()).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.add(new Vec3i(1, 0, 0)).up().up()).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.add(new Vec3i(0, 0, 1)).up()).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.add(new Vec3i(0, 0, 1)).up().up()).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.add(new Vec3i(1, 0, 1)).up()).getBlock().equals(Blocks.AIR))
            return null;
        if (!mc.world.getBlockState(position.add(new Vec3i(1, 0, 1)).up().up()).getBlock().equals(Blocks.AIR))
            return null;

        HoleSafety safety = null;
        for (Vec3i offset : quadOffsets) {
            if (!(mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.BEDROCK)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.OBSIDIAN)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.RESPAWN_ANCHOR)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.ENDER_CHEST))) {
                return null;
            }

            if (mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.BEDROCK)) {
                if (safety == HoleSafety.OBSIDIAN)
                    safety = HoleSafety.MIXED;
                else if (safety != HoleSafety.MIXED)
                    safety = HoleSafety.BEDROCK;
            }

            if (mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.OBSIDIAN)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.RESPAWN_ANCHOR)
                    || mc.world.getBlockState(position.add(offset)).getBlock().equals(Blocks.ENDER_CHEST)) {
                if (safety == HoleSafety.BEDROCK)
                    safety = HoleSafety.MIXED;
                else if (safety != HoleSafety.MIXED)
                    safety = HoleSafety.OBSIDIAN;
            }
        }

        if (safety == null)
            safety = HoleSafety.OBSIDIAN;

        return new Hole(new Box(position.getX(), position.getY(), position.getZ(), position.getX() + 2,
                position.getY() + height, position.getZ() + 2), HoleType.QUAD, safety);
    }

    public record Hole(Box box, HoleType type, HoleSafety safety) {
    }

    public enum HoleType {
        SINGLE, DOUBLE, QUAD
    }

    public enum HoleSafety {
        OBSIDIAN, MIXED, BEDROCK
    }
}
