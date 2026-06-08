package eu.client.modules.impl.combat;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.PlayerUpdateEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.InventoryUtils;
import eu.client.utils.minecraft.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@RegisterModule(name = "HoleBuilder", description = "Automatically places blocks to complete incomplete holes at your feet.", category = Module.Category.COMBAT, proxyEnhanced = true)
public class HoleBuilder extends Module {
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Sends a packet rotation whenever placing a block.", true);
    public BooleanSetting crystalDestruction = new BooleanSetting("CrystalDestruction", "Destroys any crystals that interfere with block placement.", true);
    public NumberSetting limit = new NumberSetting("Limit", "The number of blocks that can be placed per tick.", 1, 1, 20);
    public NumberSetting delay = new NumberSetting("Delay", "The amount of ticks to wait between placements.", 0, 0, 20);
    public BooleanSetting autoSurround = new BooleanSetting("AutoSurround", "Automatically activates Surround when the hole is completed.", true);
    public BooleanSetting render = new BooleanSetting("Render", "Renders the incomplete hole positions.", true);
    public ColorSetting fillColor = new ColorSetting("FillColor", "Fill", "The fill color for incomplete hole rendering.", new BooleanSetting.Visibility(render, true), new ColorSetting.Color(new Color(255, 170, 0, 40), false, false));
    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "Outline", "The outline color for incomplete hole rendering.", new BooleanSetting.Visibility(render, true), new ColorSetting.Color(new Color(255, 170, 0, 120), false, false));

    private final List<BlockPos> targetPositions = new ArrayList<>();
    private int delayTicks = 0;

    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    @Override
    public void onEnable() {
        targetPositions.clear();
        delayTicks = 0;
    }

    @Override
    public void onDisable() {
        targetPositions.clear();
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (shouldRunOnProxy()) return;
        if (getNull()) return;

        BlockPos playerPos = mc.player.getBlockPos();
        targetPositions.clear();

        // Find incomplete hole at the player's feet position
        List<BlockPos> airPositions = findIncompleteHole(playerPos);
        if (airPositions == null || airPositions.isEmpty()) {
            return;
        }

        targetPositions.addAll(airPositions);

        // Handle delay
        if (delayTicks < delay.getValue().intValue()) {
            delayTicks++;
            return;
        }

        // Find hardest block in hotbar
        int blockSlot = InventoryUtils.findHardestBlock(0, 8);
        if (blockSlot == -1) return;

        int previousSlot = mc.player.getInventory().selectedSlot;

        // Filter out positions blocked by non-crystal entities
        List<BlockPos> validPositions = targetPositions.stream()
                .filter(pos -> !isEntityBlocking(pos))
                .toList();

        if (validPositions.isEmpty()) {
            delayTicks = 0;
            return;
        }

        InventoryUtils.switchSlot("Silent", blockSlot, previousSlot);

        int placed = 0;
        for (BlockPos position : validPositions) {
            if (placed >= limit.getValue().intValue()) break;

            Direction direction = WorldUtils.getDirection(position, false);
            if (direction == null) continue;

            WorldUtils.placeBlock(position, direction, Hand.MAIN_HAND, rotate.getValue(), crystalDestruction.getValue(), false);
            placed++;
        }

        InventoryUtils.switchBack("Silent", blockSlot, previousSlot);
        delayTicks = 0;

        // If we placed all needed blocks and auto-surround is on, enable surround
        if (autoSurround.getValue() && placed >= validPositions.size()) {
            SurroundModule surroundModule = EUClient.MODULE_MANAGER.getModule(SurroundModule.class);
            if (surroundModule != null && !surroundModule.isToggled()) {
                surroundModule.setToggled(true, false);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull() || !render.getValue() || targetPositions.isEmpty()) return;

        for (BlockPos pos : targetPositions) {
            Box box = new Box(pos);
            Renderer3D.renderBox(event.getMatrices(), box, fillColor.getColor());
            Renderer3D.renderBoxOutline(event.getMatrices(), box, outlineColor.getColor());
        }
    }

    /**
     * Checks if the player is standing in an incomplete hole.
     * An incomplete hole has exactly 3 hard blocks (bedrock, obsidian, ender chest, respawn anchor)
     * and 1 air block in the 4 cardinal horizontal directions, with a solid floor beneath.
     *
     * @return the list of air positions that need to be filled, or null if not an incomplete hole
     */
    private List<BlockPos> findIncompleteHole(BlockPos playerPos) {
        if (mc.world == null) return null;

        // Floor must be solid (hard block)
        if (!isHardBlock(mc.world.getBlockState(playerPos.down()).getBlock())) return null;

        int hardCount = 0;
        List<BlockPos> airPositions = new ArrayList<>();

        for (Direction dir : HORIZONTAL) {
            BlockPos checkPos = playerPos.offset(dir);
            Block block = mc.world.getBlockState(checkPos).getBlock();

            if (isHardBlock(block)) {
                hardCount++;
            } else if (mc.world.getBlockState(checkPos).isReplaceable()) {
                airPositions.add(checkPos);
            } else {
                // Some other non-hard, non-replaceable block — not a valid incomplete hole
                return null;
            }
        }

        // Incomplete hole: exactly 3 hard blocks and 1 air gap
        if (hardCount == 3 && airPositions.size() == 1) {
            return airPositions;
        }

        return null;
    }

    /**
     * Checks if a block is a "hard" block used in holes (obsidian, bedrock, ender chest, respawn anchor).
     */
    private boolean isHardBlock(Block block) {
        return block.equals(Blocks.OBSIDIAN)
                || block.equals(Blocks.BEDROCK)
                || block.equals(Blocks.ENDER_CHEST)
                || block.equals(Blocks.RESPAWN_ANCHOR);
    }

    /**
     * Checks if a non-crystal entity is blocking placement at the given position.
     * Crystals are allowed because they can be destroyed by the crystal destruction setting.
     */
    private boolean isEntityBlocking(BlockPos pos) {
        if (mc.world == null) return false;
        return mc.world.getOtherEntities(mc.player, new Box(pos)).stream()
                .anyMatch(e -> !(e instanceof EndCrystalEntity));
    }

    @Override
    public String getMetaData() {
        return String.valueOf(targetPositions.size());
    }
}
