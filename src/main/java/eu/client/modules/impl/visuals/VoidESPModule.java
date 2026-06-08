package eu.client.modules.impl.visuals;

import eu.client.events.SubscribeEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.*;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer3D;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RegisterModule(name = "VoidESP", description = "Highlights any non-bedrock blocks that can drop you into the void.", category = Module.Category.VISUALS)
public class VoidESPModule extends Module {
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which void blocks will be rendered.", 10, 1, 50);
    public BooleanSetting asynchronous = new BooleanSetting("Asynchronous", "Performs calculations on separate threads.", true);

    public CategorySetting fillCategory = new CategorySetting("Fill", "The category for settings related to fill rendering.");
    public ModeSetting fill = new ModeSetting("Fill", "Mode", "The mode for the fill rendering on the void blocks.", new CategorySetting.Visibility(fillCategory), "Normal", new String[]{"None", "Normal", "Gradient"});
    public NumberSetting fillHeight = new NumberSetting("FillHeight", "Height", "The height of the fill rendering on the void blocks.", new ModeSetting.Visibility(fill, "Normal", "Gradient"), 1.0, 0.0, 2.0);
    public ColorSetting fillColor = new ColorSetting("FillColor", "Color", "The color for the fill rendering on the void blocks.", new ModeSetting.Visibility(fill, "Normal", "Gradient"), new ColorSetting.Color(new Color(255, 0, 0, ColorUtils.getDefaultFillColor().getColor().getAlpha()), false, false));

    public CategorySetting outlineCategory = new CategorySetting("Outline", "The category for settings related to outline rendering.");
    public ModeSetting outline = new ModeSetting("Outline", "Mode", "The mode for the outline rendering on the void blocks.", new CategorySetting.Visibility(outlineCategory), "Normal", new String[]{"None", "Normal", "Gradient"});
    public NumberSetting outlineHeight = new NumberSetting("OutlineHeight", "Height", "The height of the outline rendering on the void blocks.", new ModeSetting.Visibility(outline, "Normal", "Gradient"), 1.0, 0.0, 2.0);
    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "Color", "The color for the outline rendering on the void blocks.", new ModeSetting.Visibility(outline, "Normal", "Gradient"), new ColorSetting.Color(new Color(255, 0, 0, ColorUtils.getDefaultOutlineColor().getColor().getAlpha()), false, false));

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final List<BlockPos> positions = Collections.synchronizedList(new ArrayList<>());
    
    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        Runnable runnable = () -> {
            List<BlockPos> positions = new ArrayList<>();
            for (int x = (int) (mc.player.getX() - range.getValue().intValue()); x < mc.player.getX() + range.getValue().intValue(); x++) {
                for (int z = (int) (mc.player.getZ() - range.getValue().intValue()); z < mc.player.getZ() + range.getValue().intValue(); z++) {
                    BlockPos position = BlockPos.ofFloored(x, mc.world.getBottomY(), z);
                    if (mc.world.getBlockState(position).getBlock() == Blocks.BEDROCK) continue;
                    if (!mc.world.getWorldBorder().contains(position)) continue;

                    positions.add(position);
                }
            }

            synchronized (this.positions) {
                this.positions.clear();
                this.positions.addAll(positions);
            }
        };

        if (asynchronous.getValue()) executor.submit(runnable);
        else runnable.run();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.world == null) return;

        synchronized (positions) {
            if (positions.isEmpty()) return;

            for (BlockPos position : positions) {
                Box box = new Box(position);

                Box filledBox = new Box(box.minX, box.minY, box.minZ, box.maxX, box.minY + fillHeight.getValue().doubleValue(), box.maxZ);
                Box outlinedBox = new Box(box.minX, box.minY, box.minZ, box.maxX, box.minY + outlineHeight.getValue().doubleValue(), box.maxZ);

                if (fill.getValue().equalsIgnoreCase("Normal")) Renderer3D.renderBox(event.getMatrices(), filledBox, fillColor.getColor());
                if (fill.getValue().equalsIgnoreCase("Gradient")) Renderer3D.renderGradientBox(event.getMatrices(), filledBox, fillHeight.getValue().floatValue() < 0.0f ? fillColor.getColor() : new Color(0, 0, 0, 0), fillHeight.getValue().floatValue() < 0.0f ? new Color(0, 0, 0, 0) : fillColor.getColor());

                if (outline.getValue().equalsIgnoreCase("Normal")) Renderer3D.renderBoxOutline(event.getMatrices(), outlinedBox, outlineColor.getColor());
                if (outline.getValue().equalsIgnoreCase("Gradient")) Renderer3D.renderGradientBoxOutline(event.getMatrices(), outlinedBox, outlineHeight.getValue().floatValue() < 0.0f ? outlineColor.getColor() : new Color(0, 0, 0, 0), outlineHeight.getValue().floatValue() < 0.0f ? new Color(0, 0, 0, 0) : outlineColor.getColor());
            }
        }
    }

    @Override
    public String getMetaData() {
        return String.valueOf(positions.size());
    }

}
