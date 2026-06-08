
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
import eu.client.utils.minecraft.PositionUtils;
import eu.client.utils.minecraft.WorldUtils;
import eu.client.utils.rotations.RotationUtils;
import eu.client.utils.system.ThreadExecutor;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.List;

@RegisterModule(name = "AutoWeb", description = "Automatically places webs on other people's feet.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class AutoWebModule extends Module {
    public ModeSetting autoSwitch = new ModeSetting("Switch", "The mode that will be used for automatically switching to necessary items.", "Silent", InventoryUtils.SWITCH_MODES);
    public BooleanSetting asynchronous = new BooleanSetting("Asynchronous", "Performs calculations on separate threads.", true);
    public NumberSetting delay = new NumberSetting("Delay", "The amount of ticks that have to be waited for between placements.", 0, 0, 20);
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which the blocks will be placed at.", 5.0, 0.0, 12.0);
    public NumberSetting enemyRange = new NumberSetting("EnemyRange", "The maximum distance at which the target should be at.", 8.0f, 0.0f, 16.0f);
    public NumberSetting extrapolation = new NumberSetting("Extrapolation", "Extrapolates the target's position to calculate positions ahead of time.", 0, 0, 20);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Sends a packet rotation whenever placing a block.", true);
    public BooleanSetting airPlace = new BooleanSetting("AirPlace", "Lets you place webs on air.", false);
    public BooleanSetting strictDirection = new BooleanSetting("StrictDirection", "Only places using directions that face you.", false);
    public BooleanSetting holeCheck = new BooleanSetting("HoleCheck", "Checks if the target is in a hole or not before placing.", true);
    public BooleanSetting whileEating = new BooleanSetting("WhileEating", "Places blocks normally while eating.", true);

    public BooleanSetting selfDisable = new BooleanSetting("SelfDisable", "Toggles off the module once it is finished with placing.", false);
    public BooleanSetting itemDisable = new BooleanSetting("ItemDisable", "Toggles off the module whenever you run out of items to place with.", true);

    public BooleanSetting render = new BooleanSetting("Render", "Whether or not to render the place position.", true);

    private PlayerEntity target = null;

    private int ticks = 0;

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (shouldRunOnProxy()) return;
        if (!whileEating.getValue() && mc.player.isUsingItem()) return;

        List<AbstractClientPlayerEntity> players = mc.world.getPlayers();

        Runnable runnable = () -> {
            if (ticks < delay.getValue().intValue()) {
                ticks++;
                return;
            }

            if (autoSwitch.getValue().equalsIgnoreCase("None") && mc.player.getMainHandStack().getItem() != Items.COBWEB) {
                if (itemDisable.getValue()) {
                    EUClient.CHAT_MANAGER.tagged("You are currently not holding any cobwebs.", getName());
                    setToggled(false);
                }

                target = null;
                return;
            }

            int slot = InventoryUtils.find(Items.COBWEB, 0, autoSwitch.getValue().equalsIgnoreCase("AltSwap") || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
            int previousSlot = mc.player.getInventory().selectedSlot;

            if (slot == -1) {
                if (itemDisable.getValue()) {
                    EUClient.CHAT_MANAGER.tagged("No cobwebs could be found in your hotbar.", getName());
                    setToggled(false);
                }

                target = null;
                return;
            }

            target = getTarget(players);
            if (target == null) {
                if (selfDisable.getValue()) setToggled(false);
                return;
            }

            Vec3d vec3d = PositionUtils.extrapolate(target, extrapolation.getValue().intValue()).getCenter();
            BlockPos position = new BlockPos((int) Math.floor(vec3d.x), (int) vec3d.y, (int) Math.floor(vec3d.z));

            if(target.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) && mc.world.getBlockState(position).getBlock().equals(Blocks.COBWEB)) {
                position = position.up();
            }

            if (!mc.world.getBlockState(position).isReplaceable()) return;
            if (mc.player.squaredDistanceTo(Vec3d.ofCenter(position)) > MathHelper.square(range.getValue().doubleValue())) return;
            if(PositionUtils.getFlooredPosition(mc.player).equals(position) && HoleUtils.isPlayerInHole(mc.player)) return;

            Direction direction = WorldUtils.getDirection(position, strictDirection.getValue());
            if (direction == null && !airPlace.getValue()) return;

            if(rotate.getValue() && airPlace.getValue()) EUClient.ROTATION_MANAGER.rotate(RotationUtils.getRotations(position.toCenterPos()), this);

            InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);
            WorldUtils.placeBlock(position, direction, Hand.MAIN_HAND, rotate.getValue(), false, render.getValue());
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
    public String getMetaData() {
        if (target == null) return "None";
        return target.getName().getString();
    }

    private PlayerEntity getTarget(List<AbstractClientPlayerEntity> players) {
        PlayerEntity optimalPlayer = null;
        for (PlayerEntity player : players) {
            if (player == mc.player) continue;
            if (!player.isAlive() || player.getHealth() <= 0.0f) continue;
            if (mc.player.squaredDistanceTo(player) > MathHelper.square(enemyRange.getValue().doubleValue())) continue;
            if (EUClient.FRIEND_MANAGER.contains(player.getName().getString())) continue;
            if (holeCheck.getValue() && !HoleUtils.isPlayerInHole(player)) continue;

            if (optimalPlayer == null) {
                optimalPlayer = player;
                continue;
            }

            if (mc.player.squaredDistanceTo(player) < mc.player.squaredDistanceTo(optimalPlayer)) {
                optimalPlayer = player;
            }
        }

        return optimalPlayer;
    }
}
