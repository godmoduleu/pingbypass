package eu.client.modules.impl.combat;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.modules.impl.movement.HitboxDesyncModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.PositionUtils;
import eu.client.utils.minecraft.WorldUtils;
import eu.client.utils.system.ThreadExecutor;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@RegisterModule(name = "OpponentScaffold", description = "Automatically places blocks under non-friended players to prevent them from falling.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class OpponentScaffold extends Module {
    public ModeSetting autoSwitch = new ModeSetting("Switch", "The mode that will be used for automatically switching to necessary items.", "Silent", InventoryUtils.SWITCH_MODES);
    public BooleanSetting asynchronous = new BooleanSetting("Asynchronous", "Performs calculations on separate threads.", true);
    public NumberSetting limit = new NumberSetting("Limit", "The number of blocks that can be placed per tick.", 4, 1, 20);
    public NumberSetting delay = new NumberSetting("Delay", "The amount of ticks that have to be waited for between placements.", 0, 0, 20);
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which blocks will be placed at.", 5.0, 0.0, 12.0);
    public NumberSetting enemyRange = new NumberSetting("EnemyRange", "The maximum distance at which the target should be at.", 10.0f, 0.0f, 20.0f);
    public NumberSetting extrapolation = new NumberSetting("Extrapolation", "Extrapolates the target's position to predict where they will move.", 0, 0, 20);
    public BooleanSetting await = new BooleanSetting("Await", "Waits for blocks to be registered by the client before placing on them.", false);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Whether or not you should rotate when you place blocks.", true);
    public BooleanSetting strictDirection = new BooleanSetting("StrictDirection", "Only places using directions that face you.", false);
    public BooleanSetting crystalDestruction = new BooleanSetting("CrystalDestruction", "Destroys any crystals that interfere with block placement.", true);
    public BooleanSetting whileEating = new BooleanSetting("WhileEating", "Places blocks normally while eating.", true);

    public BooleanSetting selfDisable = new BooleanSetting("SelfDisable", "Toggles off the module once it is finished with placing.", false);
    public BooleanSetting itemDisable = new BooleanSetting("ItemDisable", "Toggles off the module whenever you run out of items to place with.", true);

    public BooleanSetting render = new BooleanSetting("Render", "Whether or not to render the place position.", true);

    private List<BlockPos> positions = new ArrayList<>();

    private int ticks = 0;
    private int blocksPlaced = 0;

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (shouldRunOnProxy()) return;
        if (mc.player == null || mc.world == null) return;
        if (!whileEating.getValue() && mc.player.isUsingItem()) return;

        List<AbstractClientPlayerEntity> players = mc.world.getPlayers();
        if (players == null || players.isEmpty()) return;

        Runnable runnable = () -> {
            if (mc.player == null || mc.world == null) return;

            blocksPlaced = 0;
            if (ticks < delay.getValue().intValue()) {
                ticks++;
                return;
            }

            if (autoSwitch.getValue().equalsIgnoreCase("None") && !(mc.player.getMainHandStack() != null && mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
                if (itemDisable.getValue()) {
                    EUClient.CHAT_MANAGER.tagged("You are currently not holding any blocks.", getName());
                    setToggled(false);
                }

                positions = new ArrayList<>();
                return;
            }

            int slot = InventoryUtils.findHardestBlock(0, autoSwitch.getValue().equalsIgnoreCase("AltSwap") || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
            int previousSlot = mc.player.getInventory() != null ? mc.player.getInventory().selectedSlot : 0;

            if (slot == -1) {
                if (itemDisable.getValue()) {
                    EUClient.CHAT_MANAGER.tagged("No blocks could be found in your hotbar.", getName());
                    setToggled(false);
                }

                positions = new ArrayList<>();
                return;
            }

            Target target = getTarget(players);
            if (target == null) {
                if (selfDisable.getValue()) setToggled(false);

                positions = new ArrayList<>();
                return;
            }

            positions = target.positions();

            InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);

            List<BlockPos> placedPositions = new ArrayList<>();
            for (BlockPos position : positions) {
                if (blocksPlaced >= limit.getValue().intValue()) break;

                Direction direction = WorldUtils.getDirection(position, placedPositions, strictDirection.getValue());
                if (direction == null) {
                    BlockPos supportPosition = position.add(0, -1, 0);
                    if (!WorldUtils.isPlaceable(supportPosition)) continue;

                    Direction supportDirection = WorldUtils.getDirection(supportPosition, placedPositions, strictDirection.getValue());
                    if (supportDirection == null) continue;

                    WorldUtils.placeBlock(supportPosition, supportDirection, Hand.MAIN_HAND, rotate.getValue(), crystalDestruction.getValue(), render.getValue());
                    placedPositions.add(supportPosition);
                    blocksPlaced++;

                    if (blocksPlaced >= limit.getValue().intValue()) break;
                    if (await.getValue()) continue;

                    direction = WorldUtils.getDirection(position, placedPositions, strictDirection.getValue());
                    if (direction == null) continue;
                }

                WorldUtils.placeBlock(position, direction, Hand.MAIN_HAND, rotate.getValue(), crystalDestruction.getValue(), render.getValue());
                placedPositions.add(position);
                blocksPlaced++;
            }

            InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);

            ticks = 0;
        };

        if (asynchronous.getValue()) ThreadExecutor.execute(runnable);
        else runnable.run();
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) setToggled(false);
    }

    @Override
    public void onDisable() {
        positions = new ArrayList<>();
    }

    @Override
    public String getMetaData() {
        return String.valueOf(positions.size());
    }

    private Target getTarget(List<AbstractClientPlayerEntity> players) {
        if (mc.player == null) return null;

        PlayerEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (AbstractClientPlayerEntity player : players) {
            if (player == mc.player) continue;
            if (player.isInvisible()) continue;
            if (EUClient.FRIEND_MANAGER.contains(player.getName().getString())) continue;
            if (player.getHealth() <= 0) continue;

            double distance = mc.player.squaredDistanceTo(player);
            if (distance > MathHelper.square(enemyRange.getValue().floatValue())) continue;

            if (distance < closestDistance) {
                closestDistance = distance;
                closest = player;
            }
        }

        if (closest == null) return null;

        // Calculate target positions
        BlockPos basePos = getTargetPosition(closest);
        if (basePos == null) return null;

        HitboxDesyncModule module = EUClient.MODULE_MANAGER.getModule(HitboxDesyncModule.class);
        List<BlockPos> targetPositions = new ArrayList<>();

        // Add the block directly under the player
        BlockPos underPlayer = basePos.down();
        // Guard against null module and null-close value; treat as disabled when module missing
        boolean hitboxEnabled = module != null && module.isToggled() && !Boolean.TRUE.equals(module.close.getValue());
        if (WorldUtils.isPlaceable(underPlayer, hitboxEnabled)) {
            double distToPlayer = mc.player.squaredDistanceTo(Vec3d.ofCenter(underPlayer));
            if (distToPlayer <= MathHelper.square(range.getValue().doubleValue())) {
                targetPositions.add(underPlayer);
            }
        }


        if (targetPositions.isEmpty()) return null;

        return new Target(closest, targetPositions);
    }

    private BlockPos getTargetPosition(PlayerEntity player) {
        if (player == null) return null;

        // Get the predicted position of the player based on extrapolation ticks
        int extrapolationTicks = extrapolation.getValue().intValue();

        if (extrapolationTicks > 0) {
            // Calculate the player's motion
            double deltaX = player.getX() - player.prevX;
            double deltaZ = player.getZ() - player.prevZ;

            // Predict where the player will be
            double predictedX = player.getX() + (deltaX * extrapolationTicks);
            double predictedZ = player.getZ() + (deltaZ * extrapolationTicks);

            return new BlockPos((int) Math.floor(predictedX), (int) Math.floor(player.getY()), (int) Math.floor(predictedZ));
        } else {
            // Use current position if no extrapolation
            return PositionUtils.getFlooredPosition(player);
        }
    }

    private record Target(PlayerEntity player, List<BlockPos> positions) {
    }
}
