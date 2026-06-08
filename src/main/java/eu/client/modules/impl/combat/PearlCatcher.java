package eu.client.modules.impl.combat;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.EntitySpawnEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.minecraft.HoleUtils;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.WorldUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

@RegisterModule(name = "PearlCatcher", description = "Places obsidian in the air to stop players pearls from reaching their destination.", category = Module.Category.COMBAT)
public class PearlCatcher extends Module {
    public ModeSetting autoSwitch = new ModeSetting("Switch", "The mode that will be used for automatically switching to necessary items.", "Silent", InventoryUtils.SWITCH_MODES);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Sends a packet rotation whenever placing a block.", true);

    public NumberSetting range = new NumberSetting("Range", "The maximum range at which the blocks will be placed at.", 5.0, 0.0, 12.0);
    public NumberSetting enemyRange = new NumberSetting("EnemyRange", "The maximum distance at which the target should be at.", 8.0f, 0.0f, 16.0f);
    public NumberSetting distance = new NumberSetting("Distance", "The distance at which the obisidan block will be placed from the player.", 4.0f, 3.0f, 6.0f);

    public BooleanSetting holeCheck = new BooleanSetting("HoleCheck", "Only self traps whenever you are in a hole.", false);
    public BooleanSetting whileEating = new BooleanSetting("WhileEating", "Places blocks normally while eating.", true);
    public BooleanSetting itemDisable = new BooleanSetting("ItemDisable", "Toggles off the module whenever you run out of items to place with.", true);

    public BooleanSetting render = new BooleanSetting("Render", "Whether or not to render the place position.", true);

    @SubscribeEvent
    public void onEntitySpawn(EntitySpawnEvent event) {
        if(!(event.getEntity() instanceof EnderPearlEntity pearl) || (!whileEating.getValue() && mc.player.isUsingItem())) return;

        if(!(pearl.getOwner() instanceof PlayerEntity owner) || !validTarget(owner)) return;

        catchPearl(owner);
    }

    private void catchPearl(PlayerEntity player) {
        if (autoSwitch.getValue().equalsIgnoreCase("None") && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            if (itemDisable.getValue()) {
                EUClient.CHAT_MANAGER.tagged("You are currently not holding any blocks.", getName());
                setToggled(false);
            }
            return;
        }

        int slot = InventoryUtils.findHardestBlock(0, autoSwitch.getValue().equalsIgnoreCase("AltSwap") || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
        int previousSlot = mc.player.getInventory().selectedSlot;

        if (slot == -1) {
            if (itemDisable.getValue()) {
                EUClient.CHAT_MANAGER.tagged("No blocks could be found in your hotbar.", getName());
                setToggled(false);
            }
            return;
        }

        HitResult hitResult = player.raycast(distance.getValue().floatValue(), 0, false);

        if (!(hitResult instanceof BlockHitResult blockHitResult)) return;

        BlockPos pos = blockHitResult.getBlockPos();

        if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > MathHelper.square(range.getValue().doubleValue())) return;

        if (mc.world.getBlockState(pos).getBlock().equals(Blocks.AIR)) {
            InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);
            WorldUtils.placeBlock(pos, WorldUtils.getDirection(pos, false), Hand.MAIN_HAND, rotate.getValue(), false, render.getValue());
            InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);
        }
    }

    private boolean validTarget(PlayerEntity player) {
        if(player == mc.player) return false;
        if (mc.player.squaredDistanceTo(player) > MathHelper.square(enemyRange.getValue().doubleValue())) return false;
        if (EUClient.FRIEND_MANAGER.contains(player.getName().getString())) return false;
        if (holeCheck.getValue() && !HoleUtils.isPlayerInHole(player)) return false;
        return true;
    }
}
