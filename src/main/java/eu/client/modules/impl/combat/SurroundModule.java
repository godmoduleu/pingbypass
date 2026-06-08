package eu.client.modules.impl.combat;

import eu.client.Pingbypass;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PacketReceiveEvent;
import eu.client.events.impl.PlayerJumpEvent;
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
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RegisterModule(name = "Surround", description = "Automatically places blocks at your feet to prevent crystal damage.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class SurroundModule extends Module {
    public ModeSetting autoSwitch = new ModeSetting("Switch",
            "The mode that will be used for automatically switching to necessary items.", "Silent",
            InventoryUtils.SWITCH_MODES);
    public ModeSetting timing = new ModeSetting("Timing",
            "The timing that will be used in replacing broken surround blocks.", "Sequential",
            new String[] { "Vanilla", "Sequential" });
    public NumberSetting limit = new NumberSetting("Limit",
            "The maximum number of blocks that can be placed each group.", 4, 1, 20);
    public NumberSetting delay = new NumberSetting("Delay", "The delay in ticks between each group of placements.", 0,
            0, 20);
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which the blocks will be placed at.",
            5.0, 0.0, 12.0);
    public BooleanSetting await = new BooleanSetting("Await",
            "Waits for blocks to be registered by the client before placing on them.", false);
    public BooleanSetting rotate = new BooleanSetting("Rotate",
            "Whether or not you should rotate when you place blocks.", true);
    public BooleanSetting strictDirection = new BooleanSetting("StrictDirection",
            "Only places using directions that face you.", false);
    public BooleanSetting crystalDestruction = new BooleanSetting("CrystalDestruction",
            "Destroys any crystals that interfere with block placement.", true);
    public BooleanSetting center = new BooleanSetting("Center",
            "Puts you in the center of the block when you surround.", false);
    public BooleanSetting floor = new BooleanSetting("Floor", "Places blocks under your feet as well.", true);
    public BooleanSetting extension = new BooleanSetting("Extension",
            "Extends the surround if there are entities obstructing block placement.", true);
    public BooleanSetting whileEating = new BooleanSetting("WhileEating", "Places blocks normally while eating.", true);

    public BooleanSetting selfDisable = new BooleanSetting("SelfDisable",
            "Toggles off the module once it is finished with placing.", false);
    public BooleanSetting jumpDisable = new BooleanSetting("JumpDisable",
            "Toggles off the module whenever your Y level changes.", true);
    public BooleanSetting itemDisable = new BooleanSetting("ItemDisable",
            "Toggles off the module whenever you run out of items to place with.", true);

    public BooleanSetting stepToggle = new BooleanSetting("StepToggle", "Toggles the step module when you surround.",
            false);
    public BooleanSetting speedToggle = new BooleanSetting("SpeedToggle", "Toggles the speed module when you surround.",
            false);

    public BooleanSetting render = new BooleanSetting("Render", "Whether or not to render the place position.", true);

    private Set<BlockPos> targetPositions = new HashSet<>();
    private BlockPos lastPosition = null;

    private int ticks = 0;
    private int blocksPlaced = 0;

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null)
            return;
        lastPosition = PositionUtils.getFlooredPosition(mc.player);

        if (center.getValue())
            mc.player.setPosition(lastPosition.getX() + 0.5, lastPosition.getY(), lastPosition.getZ() + 0.5);
    }

    @SubscribeEvent
    public void onPlayerJump(PlayerJumpEvent event) {
        if (shouldRunOnProxy())
            return;
        if (jumpDisable.getValue()) {
            setToggled(false);
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;
        if (jumpDisable.getValue() && (mc.player.fallDistance > 2.0f)) {
            setToggled(false);
            return;
        }

        if (!whileEating.getValue() && mc.player.isUsingItem())
            return;
        blocksPlaced = 0;

        if (ticks < delay.getValue().intValue()) {
            ticks++;
            return;
        }

        if (autoSwitch.getValue().equalsIgnoreCase("None")
                && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            if (itemDisable.getValue()) {
                Pingbypass.CHAT_MANAGER.tagged("You are currently not holding any blocks.", getName());
                setToggled(false);
            }

            targetPositions.clear();
            return;
        }

        int slot = InventoryUtils.findHardestBlock(0, 8);
        int previousSlot = mc.player.getInventory().selectedSlot;

        if (slot == -1) {
            if (itemDisable.getValue()) {
                Pingbypass.CHAT_MANAGER.tagged("No blocks could be found in your hotbar.", getName());
                setToggled(false);
            }

            targetPositions.clear();
            return;
        }

        targetPositions = HoleUtils.getFeetPositions(mc.player, extension.getValue(), floor.getValue(), false);

        List<BlockPos> positions = targetPositions.stream()
                .filter(position -> mc.player.squaredDistanceTo(Vec3d.ofCenter(position)) <= MathHelper
                        .square(range.getValue().doubleValue()))
                .filter(position -> WorldUtils.isPlaceable(position, false))
                .toList();

        if (positions.isEmpty()) {
            if (selfDisable.getValue())
                setToggled(false);
            return;
        }

        InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);

        List<BlockPos> placedPositions = new ArrayList<>();
        for (BlockPos position : positions) {
            if (blocksPlaced >= limit.getValue().intValue())
                break;

            Direction direction = WorldUtils.getDirection(position, placedPositions, strictDirection.getValue());
            if (direction == null) {
                BlockPos supportPosition = position.add(0, -1, 0);
                if (!WorldUtils.isPlaceable(supportPosition))
                    continue;

                Direction supportDirection = WorldUtils.getDirection(supportPosition, placedPositions,
                        strictDirection.getValue());
                if (supportDirection == null)
                    continue;

                WorldUtils.placeBlock(supportPosition, supportDirection, Hand.MAIN_HAND, rotate.getValue(),
                        crystalDestruction.getValue(), render.getValue());
                placedPositions.add(supportPosition);
                blocksPlaced++;

                if (blocksPlaced >= limit.getValue().intValue())
                    break;
                if (await.getValue())
                    continue;

                direction = WorldUtils.getDirection(position, placedPositions, strictDirection.getValue());
                if (direction == null)
                    continue;
            }

            WorldUtils.placeBlock(position, direction, Hand.MAIN_HAND, rotate.getValue(), crystalDestruction.getValue(),
                    render.getValue());
            placedPositions.add(position);
            blocksPlaced++;
        }

        InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);

        ticks = 0;
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (shouldRunOnProxy())
            return;
        if (mc.player == null || mc.world == null)
            return;
        if (!timing.getValue().equalsIgnoreCase("Sequential"))
            return;

        if (event.getPacket() instanceof EntitySpawnS2CPacket packet
                && packet.getEntityType().equals(EntityType.END_CRYSTAL)) {
            EndCrystalEntity crystal = new EndCrystalEntity(mc.world, packet.getX(), packet.getY(), packet.getZ());

            for (BlockPos position : targetPositions) {
                if (new Box(position).intersects(crystal.getBoundingBox()) && targetPositions.contains(position)) {

                    if (blocksPlaced > limit.getValue().intValue())
                        return;
                    if (!whileEating.getValue() && mc.player.isUsingItem())
                        return;

                    int slot = InventoryUtils.findHardestBlock(0, autoSwitch.getValue().equalsIgnoreCase("AltSwap")
                            || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
                    int previousSlot = mc.player.getInventory().selectedSlot;

                    if (slot == -1)
                        return;

                    Direction direction = WorldUtils.getDirection(position, strictDirection.getValue());
                    if (direction == null)
                        return;

                    InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);

                    WorldUtils.placeBlock(position, direction, Hand.MAIN_HAND, () -> {
                        mc.player.networkHandler
                                .sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    }, rotate.getValue(), false, render.getValue());
                    blocksPlaced++;

                    InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);

                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onDisable() {
        lastPosition = null;
        targetPositions.clear();

        ticks = 0;
        blocksPlaced = 0;
    }

    @Override
    public String getMetaData() {
        return String.valueOf(targetPositions.size());
    }
}
