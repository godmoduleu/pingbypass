package eu.client.modules.impl.combat;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.EntityUtils;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.WorldUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RegisterModule(name = "CombatPlace", description = "Automatically places blocks in front of moving targets to block them.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class CombatPlace extends Module {
    public BooleanSetting flatten = new BooleanSetting("Flatten", "Place blocks under the target.", true);
    public ModeSetting mode = new ModeSetting("Mode", "Prediction mode.", "Two", new String[]{"None", "One", "Two", "Three"});
    public ModeSetting targetPriority = new ModeSetting("Priority", "Target selection priority.", "Closest", new String[]{"Closest", "Health"});
    public NumberSetting predictTicks = new NumberSetting("PredictTicks", "Prediction scale.", 2, 1, 8);
    public NumberSetting minKmh = new NumberSetting("MinKMH", "Min speed to trigger blocker.", 20, 1, 40);
    public NumberSetting limit = new NumberSetting("Limit", "The maximum number of blocks that can be placed per tick.", 4, 1, 20);
    public NumberSetting delay = new NumberSetting("Delay", "The delay in ticks between placements.", 0, 0, 20);
    public BooleanSetting strict = new BooleanSetting("Strict", "Prevent placement if entities intersect.", true);
    public BooleanSetting safety = new BooleanSetting("Safety", "Prevent trapping yourself.", true);
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which blocks will be placed.", 5.0, 0.0, 12.0);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Sends a packet rotation whenever placing a block.", true);
    public BooleanSetting crystalDestruction = new BooleanSetting("CrystalDestruction", "Destroys any crystals that interfere with block placement.", true);

    private int delayTicks = 0;

    @Override
    public void onEnable() {
        delayTicks = 0;
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (shouldRunOnProxy()) return;
        if (getNull()) return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        // Handle delay
        if (delayTicks < delay.getValue().intValue()) {
            delayTicks++;
            return;
        }

        List<BlockPos> blocksToPlace = getPlacePositions(target);
        if (blocksToPlace.isEmpty()) return;

        double rangeSq = Math.pow(range.getValue().doubleValue(), 2);
        blocksToPlace.removeIf(pos -> mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSq);

        if (blocksToPlace.isEmpty()) return;

        placeBlocks(blocksToPlace);
        delayTicks = 0;
    }

    private void placeBlocks(List<BlockPos> positions) {
        if (mc.player == null) return;

        int blockSlot = InventoryUtils.findHardestBlock(0, 8);
        if (blockSlot == -1) return;

        int previousSlot = mc.player.getInventory().selectedSlot;
        InventoryUtils.switchSlot("Silent", blockSlot, previousSlot);

        int placed = 0;
        for (BlockPos position : positions) {
            if (placed >= limit.getValue().intValue()) break;

            Direction direction = WorldUtils.getDirection(position, false);
            if (direction == null) continue;

            WorldUtils.placeBlock(position, direction, Hand.MAIN_HAND, rotate.getValue(), crystalDestruction.getValue(), false);
            placed++;
        }

        InventoryUtils.switchBack("Silent", blockSlot, previousSlot);
    }

    private List<BlockPos> getPlacePositions(PlayerEntity target) {
        List<BlockPos> positions = new ArrayList<>();

        if (flatten.getValue()) {
            BlockPos under = BlockPos.ofFloored(target.getPos()).down();
            if (isValidSpot(under)) positions.add(under);
        }

        String currentMode = mode.getValue();
        if (!currentMode.equals("None")) {
            double speedKmh = EntityUtils.getSpeed(target, EntityUtils.SpeedUnit.KILOMETERS);

            if (speedKmh >= minKmh.getValue().doubleValue()) {
                // Predict from the server's known position (lerp target), not the
                // rendered position which lags behind by several ticks.
                double targetX = target.getLerpTargetX();
                double targetZ = target.getLerpTargetZ();

                // Movement direction from rendered pos toward server pos
                double dx = targetX - target.getX();
                double dz = targetZ - target.getZ();

                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.001) {
                    double dirX = dx / len;
                    double dirZ = dz / len;

                    // Per-tick movement speed
                    double tickSpeed = Math.sqrt(
                            Math.pow(target.getX() - target.lastRenderX, 2)
                            + Math.pow(target.getZ() - target.lastRenderZ, 2));
                    double scale = predictTicks.getValue().doubleValue();

                    // Start from the lerp target (where the server knows they are)
                    // and project forward by tickSpeed * scale
                    BlockPos feetPos = BlockPos.ofFloored(
                            targetX + dirX * tickSpeed * scale,
                            target.getY(),
                            targetZ + dirZ * tickSpeed * scale);

                    if (isValidSpot(feetPos)) {
                        positions.add(feetPos);

                        if (currentMode.equals("Two") || currentMode.equals("Three")) {
                            BlockPos headPos = feetPos.up();
                            if (isValidSpot(headPos)) positions.add(headPos);
                        }

                        if (currentMode.equals("Three")) {
                            BlockPos topPos = feetPos.up(2);
                            if (isValidSpot(topPos)) positions.add(topPos);
                        }
                    }
                }
            }
        }
        return positions;
    }

    private boolean isValidSpot(BlockPos pos) {
        if (mc.world == null) return false;

        if (mc.world.isOutOfHeightLimit(pos)) return false;
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        if (strict.getValue()) {
            Box box = new Box(pos);
            if (mc.world.getOtherEntities(null, box).stream().anyMatch(e -> e instanceof PlayerEntity)) return false;
        }

        if (safety.getValue() && mc.player != null && mc.player.getBoundingBox().intersects(new Box(pos))) return false;

        return true;
    }

    private PlayerEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && !p.isDead() && !EUClient.FRIEND_MANAGER.contains(p.getName().getString()))
                .filter(p -> mc.player.distanceTo(p) <= range.getValue().doubleValue() + 4.0)
                .min(getComparator())
                .orElse(null);
    }

    private Comparator<PlayerEntity> getComparator() {
        if (targetPriority.getValue().equals("Health")) {
            return Comparator.comparingDouble(p -> p.getHealth() + p.getAbsorptionAmount());
        }
        return Comparator.comparingDouble(p -> mc.player != null ? mc.player.distanceTo(p) : 0);
    }
}
