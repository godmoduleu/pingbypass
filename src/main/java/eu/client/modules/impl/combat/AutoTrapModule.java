package eu.client.modules.impl.combat;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.HoleUtils;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.WorldUtils;
import eu.client.utils.system.ThreadExecutor;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;

@RegisterModule(name = "AutoTrap", description = "Automatically places blocks around you to prevent other people from getting inside your hole.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class AutoTrapModule extends Module {
    public ModeSetting autoSwitch = new ModeSetting("Switch", "The mode that will be used for automatically switching to necessary items.", "Silent", InventoryUtils.SWITCH_MODES);
    public ModeSetting mode = new ModeSetting("Mode", "The offsets that will be used when trapping.", "Full", new String[]{"Partial", "Full"});
    public BooleanSetting head = new BooleanSetting("Head", "Whether or not to cover the block on the players head.", new ModeSetting.Visibility(mode, "Full"), true);
    public BooleanSetting asynchronous = new BooleanSetting("Asynchronous", "Performs calculations on separate threads.", true);
    public NumberSetting limit = new NumberSetting("Limit", "The number of blocks that can be placed per tick.", 4, 1, 20);
    public NumberSetting delay = new NumberSetting("Delay", "The amount of ticks that have to be waited for between placements.", 0, 0, 20);
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which the blocks will be placed at.", 5.0, 0.0, 12.0);
    public NumberSetting enemyRange = new NumberSetting("EnemyRange", "The maximum distance at which the target should be at.", 8.0f, 0.0f, 16.0f);
    public BooleanSetting await = new BooleanSetting("Await", "Waits for blocks to be registered by the client before placing on them.", false);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Sends a packet rotation whenever placing a block.", true);
    public BooleanSetting strictDirection = new BooleanSetting("StrictDirection", "Only places using directions that face you.", false);
    public BooleanSetting crystalDestruction = new BooleanSetting("CrystalDestruction", "Destroys any crystals that interfere with block placement.", true);
    public BooleanSetting holeCheck = new BooleanSetting("HoleCheck", "Checks if the target is in a hole or not before placing.", true);
    public BooleanSetting antiStep = new BooleanSetting("AntiStep", "Adds additional blocks that prevent the player from stepping out of the hole.", false);
    public BooleanSetting whileEating = new BooleanSetting("WhileEating", "Places blocks normally while eating.", true);

    public BooleanSetting selfDisable = new BooleanSetting("SelfDisable", "Toggles off the module once it is finished with placing.", false);
    public BooleanSetting itemDisable = new BooleanSetting("ItemDisable", "Toggles off the module whenever you run out of items to place with.", true);

    public BooleanSetting render = new BooleanSetting("Render", "Whether or not to render the place position.", true);

    private List<BlockPos> positions = new ArrayList<>();

    private int ticks = 0;
    private int blocksPlaced = 0;

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (shouldRunOnProxy()) return;
        if (!whileEating.getValue() && mc.player.isUsingItem()) return;

        List<AbstractClientPlayerEntity> players = mc.world.getPlayers();

        Runnable runnable = () -> {
            blocksPlaced = 0;
            if (ticks < delay.getValue().intValue()) {
                ticks++;
                return;
            }

            if (autoSwitch.getValue().equalsIgnoreCase("None") && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
                if (itemDisable.getValue()) {
                    EUClient.CHAT_MANAGER.tagged("You are currently not holding any blocks.", getName());
                    setToggled(false);
                }

                positions = new ArrayList<>();
                return;
            }

            int slot = InventoryUtils.findHardestBlock(0, autoSwitch.getValue().equalsIgnoreCase("AltSwap") || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
            int previousSlot = mc.player.getInventory().selectedSlot;

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
                    BlockPos supportPosition = position.add(0, position.getY() - target.player().getBlockY() == 1 ? 1 : -1, 0);
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
        Target optimalTarget = null;
        for (PlayerEntity player : players) {
            if (player == mc.player) continue;
            if (!player.isAlive() || player.getHealth() <= 0.0f) continue;
            if (mc.player.squaredDistanceTo(player) > MathHelper.square(enemyRange.getValue().doubleValue())) continue;
            if (EUClient.FRIEND_MANAGER.contains(player.getName().getString())) continue;
            if (holeCheck.getValue() && !HoleUtils.isPlayerInHole(player)) continue;

            List<BlockPos> positions = HoleUtils.getTrapPositions(player, mode.getValue().equalsIgnoreCase("Partial"), head.getValue(), antiStep.getValue(), false, strictDirection.getValue()).stream()
                    .filter(position -> mc.player.squaredDistanceTo(Vec3d.ofCenter(position)) <= MathHelper.square(range.getValue().doubleValue()))
                    .filter(WorldUtils::isPlaceable)
                    .toList();

            if (positions.isEmpty()) continue;

            if (optimalTarget == null) {
                optimalTarget = new Target(player, positions);
                continue;
            }

            if (mc.player.squaredDistanceTo(player) < mc.player.squaredDistanceTo(optimalTarget.player())) {
                optimalTarget = new Target(player, positions);
            }
        }

        return optimalTarget;
    }

    private record Target(PlayerEntity player, List<BlockPos> positions) { }
}
