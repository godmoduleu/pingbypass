package eu.client.modules.impl.movement;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerMoveEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.HoleUtils;
import eu.client.utils.minecraft.PositionUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "Anchor", description = "Pulls you toward your nearest hole when looking down.", category = Module.Category.MOVEMENT)
public class Anchor extends Module {
    public NumberSetting pitch = new NumberSetting("Pitch", "Minimum pitch angle to activate anchoring.", 60, 0, 90);
    public BooleanSetting doubles = new BooleanSetting("Doubles", "Include 2x1 holes when anchoring.", true);
    public BooleanSetting stopMotion = new BooleanSetting("StopMotion", "Stop horizontal movement while anchoring.", true);
    public BooleanSetting fastFall = new BooleanSetting("FastFall", "Accelerate fall speed while anchoring.", true);
    public NumberSetting speed = new NumberSetting("FallSpeed", "Speed multiplier for falling.", 1.0f, 1.0f, 5.0f);
    public BooleanSetting checkAlreadyInHole = new BooleanSetting("AlreadyInHole", "Don't anchor if already in a hole.", true);
    public BooleanSetting checkTerrainHole = new BooleanSetting("CheckTerrainHole", "Don't anchor toward terrain holes.", false);
    public BooleanSetting checkBurrowed = new BooleanSetting("CheckBurrowed", "Don't anchor if burrowed.", true);


    @SubscribeEvent
    public void onPlayerMove(PlayerMoveEvent event) {
        if (getNull() || mc.player == null || mc.world == null) return;

        // Check if already in hole
        if (checkAlreadyInHole.getValue() && isAlreadyInHole()) {
            return;
        }

        // Check if burrowed
        if (checkBurrowed.getValue() && isBurrowed()) {
            return;
        }

        // ...existing code...
        float playerPitch = mc.player.getPitch();
        if (playerPitch < pitch.getValue().floatValue()) {
            return;
        }

        BlockPos playerPos = PositionUtils.getFlooredPosition(mc.player);

        // Don't anchor if falling or if not a valid position
        if (mc.player.isGliding()) {
            return;
        }

        // Search for holes below player
        int searchHeight = 5;

        for (int i = 0; i <= searchHeight; ++i) {
            BlockPos checkPos = playerPos.down(i + 1);

            // Check if there's solid ground below
            if (mc.world.isAir(checkPos)) {
                return;
            }

            // Try to find a single hole
            HoleUtils.Hole hole = HoleUtils.getSingleHole(checkPos, 1);

            if (hole != null) {
                if (checkTerrainHole.getValue() && isTerrainHole(hole)) {
                    continue;
                }
                applyAnchorLogic(event, hole);
                return;
            }

            // Try double holes if enabled
            if (doubles.getValue()) {
                hole = HoleUtils.getDoubleHole(checkPos, 1);
                if (hole != null) {
                    if (checkTerrainHole.getValue() && isTerrainHole(hole)) {
                        continue;
                    }
                    applyAnchorLogic(event, hole);
                    return;
                }
            }

            // Try quad holes if available
            hole = HoleUtils.getQuadHole(checkPos, 1);
            if (hole != null) {
                if (checkTerrainHole.getValue() && isTerrainHole(hole)) {
                    continue;
                }
                applyAnchorLogic(event, hole);
                return;
            }
        }
    }

    private void applyAnchorLogic(PlayerMoveEvent event, HoleUtils.Hole hole) {
        if (mc.player == null) return;

        Vec3d holeCenter = hole.box().getCenter();

        // Apply stop motion if enabled
        if (stopMotion.getValue()) {
            Vec3d velocity = mc.player.getVelocity();
            if (velocity != null) {
                mc.player.setVelocity(0.0D, velocity.y, 0.0D);
            }
            mc.player.input.movementForward = 0.0F;
            mc.player.input.movementSideways = 0.0F;
        }

        // Apply fast fall if enabled
        if (fastFall.getValue()) {
            Vec3d velocity = mc.player.getVelocity();
            if (velocity != null && velocity.y > -0.1D) {
                double fallSpeed = speed.getValue().doubleValue() / 10.0D;
                mc.player.setVelocity(velocity.x, -fallSpeed, velocity.z);
            }
        }

        // Calculate movement towards hole center
        double xSpeed = holeCenter.x - mc.player.getX();
        double zSpeed = holeCenter.z - mc.player.getZ();

        event.setX(xSpeed / 2.0D);
        event.setZ(zSpeed / 2.0D);
    }

    /**
     * Check if player is already in a hole
     */
    private boolean isAlreadyInHole() {
        return HoleUtils.isPlayerInHole(mc.player);
    }

    /**
     * Check if player is burrowed (surrounded by solid blocks)
     */
    private boolean isBurrowed() {
        BlockPos playerPos = PositionUtils.getFlooredPosition(mc.player);
        net.minecraft.util.math.Direction[] horizontalDirections = {
            net.minecraft.util.math.Direction.NORTH,
            net.minecraft.util.math.Direction.SOUTH,
            net.minecraft.util.math.Direction.EAST,
            net.minecraft.util.math.Direction.WEST
        };

        // Check if all 4 horizontal sides are solid
        int solidSides = 0;
        for (net.minecraft.util.math.Direction direction : horizontalDirections) {
            BlockPos checkPos = playerPos.offset(direction);
            if (!mc.world.isAir(checkPos) && !mc.world.getBlockState(checkPos).isReplaceable()) {
                solidSides++;
            }
        }

        // If 4 sides are solid, player is burrowed
        return solidSides == 4;
    }

    /**
     * Check if a hole is a terrain hole (not made of hardened blocks)
     */
    private boolean isTerrainHole(HoleUtils.Hole hole) {
        if (hole == null) return false;

        // Terrain holes have OBSIDIAN or BEDROCK safety level
        // If it's not hardened (made of terrain blocks), return true
        return hole.safety() == HoleUtils.HoleSafety.OBSIDIAN ||
               hole.safety() == HoleUtils.HoleSafety.BEDROCK;
    }
}
