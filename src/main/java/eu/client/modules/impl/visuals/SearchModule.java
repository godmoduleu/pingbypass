package eu.client.modules.impl.visuals;

import lombok.AllArgsConstructor;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.settings.impl.WhitelistSetting;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.system.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RegisterModule(name = "Search", description = "Renders ESP on selected blocks.", category = Module.Category.VISUALS)
public class SearchModule extends Module {
    public NumberSetting range = new NumberSetting("Range", "The range to render blocks at.", 100, 0, 200);
    public WhitelistSetting whitelist = new WhitelistSetting("Whitelist", "The list of whitelisted blocks.", WhitelistSetting.Type.BLOCKS);
    public ModeSetting color = new ModeSetting("Color", "The color mode for the block render.", "Default", new String[]{"Default", "Custom"});
    public ModeSetting mode = new ModeSetting("Mode", "The rendering that will be applied to the blocks.", "Both", new String[]{"Fill", "Outline", "Both"});
    public ColorSetting fillColor = new ColorSetting("FillColor", "The color that will be used for the fill rendering.", new ModeSetting.Visibility(mode, "Fill", "Both"), ColorUtils.getDefaultFillColor());
    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "The color that will be used for the outline rendering.", new ModeSetting.Visibility(mode, "Outline", "Both"), ColorUtils.getDefaultOutlineColor());

    private List<SearchBlock> blocks = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Timer timer = new Timer();
    private boolean lock;

    @Override
    public void onEnable() {
        blocks.clear();
        lock = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if(getNull()) return;

        if(timer.hasTimeElapsed(1000) && !lock) {
            CompletableFuture.supplyAsync(this::search, executor).thenAcceptAsync(this::sync, Util.getMainWorkerExecutor());
            timer.reset();
            lock = true;
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if(getNull() || blocks.isEmpty()) return;

        for(SearchBlock block : blocks) {
            Color fill = color.getValue().equals("Default") ? ColorUtils.getColor(block.color, fillColor.getAlpha()) : fillColor.getColor();
            Color outline = color.getValue().equals("Default") ? ColorUtils.getColor(block.color, outlineColor.getAlpha()) : outlineColor.getColor();
            if (mode.getValue().equalsIgnoreCase("Fill") || mode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBox(event.getMatrices(), block.box, fill);
            if (mode.getValue().equalsIgnoreCase("Outline") || mode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBoxOutline(event.getMatrices(), block.box, outline);
        }
    }

    private List<SearchBlock> search() {
        List<SearchBlock> blocks = new ArrayList<>();
        for (int x = (int) (mc.player.getX() - range.getValue().floatValue()); x <= (int) (mc.player.getX() + range.getValue().floatValue()); x++) {
            for (int z = (int) (mc.player.getZ() - range.getValue().floatValue()); z <= (int) (mc.player.getZ() + range.getValue().floatValue()); z++) {
                for (int y = mc.world.getBottomY() + 1; y <= mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z); y++) {
                    BlockPos position = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(position);

                    if (whitelist.isWhitelistContains(state.getBlock())) {
                        Box box = mc.world.getBlockState(position).getOutlineShape(mc.world, position).getBoundingBox().offset(position);
                        blocks.add(new SearchBlock(box, getColor(state, position)));
                    }
                }
            }
        }
        return blocks;
    }

    private void sync(List<SearchBlock> blocks) {
        this.blocks = blocks;
        lock = false;
    }

    private Color getColor(BlockState state, BlockPos position) {
        if (state.getBlock() == Blocks.NETHER_PORTAL) return new Color(100, 50, 255);
        if (state.getBlock() == Blocks.DIAMOND_ORE) return new Color(70, 150, 255);
        if (state.getBlock() == Blocks.DIAMOND_BLOCK) return new Color(70, 150, 255);
        if (state.getBlock() == Blocks.GOLD_ORE) return new Color(255, 200, 70);
        if (state.getBlock() == Blocks.GOLD_BLOCK) return new Color(255, 200, 70);
        if (state.getBlock() == Blocks.EMERALD_ORE) return new Color(70, 255, 90);
        if (state.getBlock() == Blocks.EMERALD_BLOCK) return new Color(70, 255, 90);
        if (state.getBlock() == Blocks.REDSTONE_ORE) return new Color(250, 30, 30);
        if (state.getBlock() == Blocks.REDSTONE_BLOCK) return new Color(250, 30, 30);
        if (state.getBlock() == Blocks.LAPIS_ORE) return new Color(30, 50, 250);
        if (state.getBlock() == Blocks.LAPIS_BLOCK) return new Color(30, 50, 250);
        if (state.getBlock() == Blocks.IRON_ORE) return new Color(170, 150, 130);
        if (state.getBlock() == Blocks.IRON_BLOCK) return new Color(170, 150, 130);
        if (state.getBlock() == Blocks.COAL_ORE) return new Color(35, 35, 35);
        if (state.getBlock() == Blocks.COAL_BLOCK) return new Color(35, 35, 35);
        if (state.getBlock() == Blocks.NETHERITE_BLOCK) return new Color(140, 30, 15);

        return new Color(state.getMapColor(mc.world, position).color);
    }

    @Override
    public String getMetaData() {
        return whitelist.getWhitelist().size() + "";
    }

    @AllArgsConstructor
    public static class SearchBlock {
        private Box box;
        private Color color;
    }
}
