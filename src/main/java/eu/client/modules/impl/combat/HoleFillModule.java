package eu.client.modules.impl.combat;

import eu.client.Pingbypass;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.HoleUtils;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.PositionUtils;
import eu.client.utils.minecraft.WorldUtils;
import eu.client.utils.system.ThreadExecutor;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;

@RegisterModule(name = "HoleFill", description = "Automatically places blocks inside of holes to prevent others from getting inside of them.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class HoleFillModule extends Module {
    public ModeSetting autoSwitch = new ModeSetting("Switch", "The mode that will be used for automatically switching to necessary items.", "Silent", InventoryUtils.SWITCH_MODES);
    public ModeSetting mode = new ModeSetting("Mode", "The offsets that will be used when trapping.", "Normal", new String[]{"Normal", "Smart"});
    public BooleanSetting webs = new BooleanSetting("Webs", "Use webs to holefill instead of blocks.", false);
    public BooleanSetting selfWeb = new BooleanSetting("SelfWeb", "Includes your own hole when holefilling with webs.", false);
    public BooleanSetting asynchronous = new BooleanSetting("Asynchronous", "Performs calculations on separate threads.", true);
    public NumberSetting limit = new NumberSetting("Limit", "The number of blocks that can be placed per tick.", 1, 1, 20);
    public NumberSetting delay = new NumberSetting("Delay", "The amount of ticks that have to be waited for between placements.", 0, 0, 20);
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which the blocks will be placed at.", 5.0, 0.0, 12.0);
    public NumberSetting enemyRange = new NumberSetting("EnemyRange", "The maximum distance at which the target should be at.", new ModeSetting.Visibility(mode, "Smart"), 8.0f, 0.0f, 16.0f);
    public NumberSetting smartRange = new NumberSetting("SmartRange", "The distance at which the holes will have to be away from the target.", new ModeSetting.Visibility(mode, "Smart"), 3.0f, 0.0f, 6.0f);
    public BooleanSetting safety = new BooleanSetting("Safety", "Prevents holes close to you from getting filled.", true);
    public NumberSetting safetyRange = new NumberSetting("SafetyRange", "The maximum distance the holes can be at to be prevented from getting filled.", new BooleanSetting.Visibility(safety, true), 2.0f, 0.0f, 6.0f);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Sends a packet rotation whenever placing a block.", true);
    public BooleanSetting strictDirection = new BooleanSetting("StrictDirection", "Only places using directions that face you.", false);
    public BooleanSetting crystalDestruction = new BooleanSetting("CrystalDestruction", "Destroys any crystals that interfere with block placement.", true);
    public BooleanSetting doubleHoles = new BooleanSetting("DoubleHoles", "Whether or not to fill double holes.", false);
    public BooleanSetting holeCheck = new BooleanSetting("HoleCheck", "Checks if the target isn't in a hole before placing.", true);
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

            boolean flag = webs.getValue() ? !mc.player.getMainHandStack().getItem().equals(Items.COBWEB) : !(mc.player.getMainHandStack().getItem() instanceof BlockItem);
            if (autoSwitch.getValue().equalsIgnoreCase("None") && flag) {
                if (itemDisable.getValue()) {
                    Pingbypass.CHAT_MANAGER.tagged("You are currently not holding any " + (webs.getValue() ? "cobwebs." : "blocks."), getName());
                    setToggled(false);
                }

                positions = new ArrayList<>();
                return;
            }
            int slot;
            if(webs.getValue()) {
                slot = InventoryUtils.find(Items.COBWEB, 0, autoSwitch.getValue().equalsIgnoreCase("AltSwap") || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
            } else {
                slot = InventoryUtils.findHardestBlock(0, autoSwitch.getValue().equalsIgnoreCase("AltSwap") || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
            }
            int previousSlot = mc.player.getInventory().selectedSlot;

            if (slot == -1) {
                if (itemDisable.getValue()) {
                    Pingbypass.CHAT_MANAGER.tagged("No " + (webs.getValue() ? "cobwebs" : "blocks") + " could be found in your hotbar.", getName());
                    setToggled(false);
                }

                positions = new ArrayList<>();
                return;
            }

            if (mode.getValue().equalsIgnoreCase("Smart")) {
                Target target = getTarget(players);

                if (target == null) positions = new ArrayList<>();
                else positions = target.positions();
            } else {
                positions = getPositions(null);
            }

            if (positions.isEmpty()) {
                if (selfDisable.getValue()) setToggled(false);
                return;
            }

            InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);

            for (BlockPos position : this.positions) {
                if (blocksPlaced >= limit.getValue().intValue()) break;

                Direction direction = WorldUtils.getDirection(position, strictDirection.getValue());
                if (direction == null) continue;

                WorldUtils.placeBlock(position, direction, Hand.MAIN_HAND, rotate.getValue(), crystalDestruction.getValue(), render.getValue());
                blocksPlaced++;
            }

            InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);

            ticks = 0;
        };

        if (asynchronous.getValue()) ThreadExecutor.execute(runnable);
        else runnable.run();
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
            if (Pingbypass.FRIEND_MANAGER.contains(player.getName().getString())) continue;
            if (holeCheck.getValue() && HoleUtils.isPlayerInHole(player)) continue;

            List<BlockPos> positions = getPositions(player);
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

    private List<BlockPos> getPositions(PlayerEntity player) {
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < Pingbypass.WORLD_MANAGER.getRadius(range.getValue().doubleValue()); i++) {
            BlockPos position = mc.player.getBlockPos().add(Pingbypass.WORLD_MANAGER.getOffset(i));
            Vec3d vec3d = Vec3d.ofCenter(position);

            if (!mc.world.getBlockState(position).isReplaceable()) continue;
            if (mc.player.squaredDistanceTo(vec3d) > MathHelper.square(range.getValue().doubleValue())) continue;
            if (mode.getValue().equalsIgnoreCase("Smart") && player != null && player.squaredDistanceTo(vec3d) > MathHelper.square(smartRange.getValue().doubleValue())) continue;
            if (safety.getValue() && !HoleUtils.isPlayerInHole(mc.player) && mc.player.squaredDistanceTo(vec3d) <= MathHelper.square(safetyRange.getValue().doubleValue())) continue;
            if (HoleUtils.getSingleHole(position, 1.0) == null && (!doubleHoles.getValue() || HoleUtils.getDoubleHole(position, 1.0) == null)) continue;
            if(webs.getValue() && selfWeb.getValue() && PositionUtils.getFlooredPosition(mc.player).equals(position) && HoleUtils.isPlayerInHole(mc.player)
                    && player.getY() > mc.player.getY() && mc.player.distanceTo(player) <= 2 && mc.player.isOnGround()) {
                positions.add(position);
                continue;
            }
            if (!WorldUtils.isPlaceable(position)) continue;

            positions.add(position);
        }

        return positions;
    }

    private record Target(PlayerEntity player, List<BlockPos> positions) { }
}
