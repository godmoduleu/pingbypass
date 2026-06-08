package eu.client.modules.impl.visuals;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.*;
import eu.client.utils.animations.Easing;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.HoleUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RegisterModule(name = "HoleESP", description = "Highlights all holes in a specified radius.", category = Module.Category.VISUALS)
public class HoleESPModule extends Module {
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which holes will be rendered.", 10, 1, 50);
    public BooleanSetting asynchronous = new BooleanSetting("Asynchronous", "Performs calculations on separate threads.", true);
    public BooleanSetting doubleHoles = new BooleanSetting("DoubleHoles", "Whether or not to render the ESP on double holes.", true);
    public BooleanSetting quadHoles = new BooleanSetting("QuadHoles", "Whether or not to render the ESP on quad holes.", true);
    public BooleanSetting fade = new BooleanSetting("Fade", "Fades the holes in and out based on your distance to them.", false);

    public ModeSetting fill = new ModeSetting("Fill", "The mode for the fill rendering on the hole boxes.", "Normal", new String[]{"None", "Normal", "Gradient"});
    public NumberSetting fillHeight = new NumberSetting("FillHeight", "The height of the fill rendering on the holes.", new ModeSetting.Visibility(fill, "Normal", "Gradient"), 1.0, -2.0, 2.0);
    public ModeSetting outline = new ModeSetting("Outline", "The mode for the outline rendering on the hole boxes.", "Normal", new String[]{"None", "Normal", "Gradient"});
    public NumberSetting outlineHeight = new NumberSetting("OutlineHeight", "The height of the outline rendering on the holes.", new ModeSetting.Visibility(outline, "Normal", "Gradient"), 1.0, -2.0, 2.0);

    public CategorySetting bedrockColorsCategory = new CategorySetting("Bedrock", "The category that contains the settings for coloring of bedrock holes.");
    public ColorSetting bedrockFillColor = new ColorSetting("BedrockFillColor", "Fill", "The color for the fill rendering on bedrock holes.", new CategorySetting.Visibility(bedrockColorsCategory), new ColorSetting.Color(new Color(0, 255, 0, ColorUtils.getDefaultFillColor().getColor().getAlpha()), false, false));
    public ColorSetting bedrockOutlineColor = new ColorSetting("BedrockOutlineColor", "Outline", "The color for the outline rendering on bedrock holes.", new CategorySetting.Visibility(bedrockColorsCategory), new ColorSetting.Color(new Color(0, 255, 0, ColorUtils.getDefaultOutlineColor().getColor().getAlpha()), false, false));

    public CategorySetting mixedColorsCategory = new CategorySetting("Mixed", "The category that contains the settings for coloring of mixed holes.");
    public ColorSetting mixedFillColor = new ColorSetting("MixedFillColor", "Fill", "The color for the fill rendering on mixed holes.", new CategorySetting.Visibility(mixedColorsCategory), new ColorSetting.Color(new Color(255, 255, 0, ColorUtils.getDefaultFillColor().getColor().getAlpha()), false, false));
    public ColorSetting mixedOutlineColor = new ColorSetting("MixedOutlineColor", "Outline", "The color for the outline rendering on mixed holes.", new CategorySetting.Visibility(mixedColorsCategory), new ColorSetting.Color(new Color(255, 255, 0, ColorUtils.getDefaultOutlineColor().getColor().getAlpha()), false, false));

    public CategorySetting obsidianColorsCategory = new CategorySetting("Obsidian", "The category that contains the settings for coloring of obsidian holes.");
    public ColorSetting obsidianFillColor = new ColorSetting("ObsidianFillColor", "Fill", "The color for the fill rendering on obsidian holes.", new CategorySetting.Visibility(obsidianColorsCategory), new ColorSetting.Color(new Color(255, 0, 0, ColorUtils.getDefaultFillColor().getColor().getAlpha()), false, false));
    public ColorSetting obsidianOutlineColor = new ColorSetting("ObsidianOutlineColor", "Outline", "The color for the outline rendering on obsidian holes.", new CategorySetting.Visibility(obsidianColorsCategory), new ColorSetting.Color(new Color(255, 0, 0, ColorUtils.getDefaultOutlineColor().getColor().getAlpha()), false, false));

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final List<HoleUtils.Hole> holes = Collections.synchronizedList(new ArrayList<>());

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        List<BlockPos> sphere = new ArrayList<>();
        for (int i = 0; i < EUClient.WORLD_MANAGER.getRadius(range.getValue().doubleValue()); i++) {
            sphere.add(mc.player.getBlockPos().add(EUClient.WORLD_MANAGER.getOffset(i)));
        }

        Runnable runnable = () -> {
            List<HoleUtils.Hole> holes = new ArrayList<>();
            for (BlockPos position : sphere) {
                HoleUtils.Hole singleHole = HoleUtils.getSingleHole(position, 0);
                if (singleHole != null) {
                    holes.add(singleHole);
                    continue;
                }

                if (doubleHoles.getValue()) {
                    HoleUtils.Hole doubleHole = HoleUtils.getDoubleHole(position, 0);
                    if (doubleHole != null) {
                        holes.add(doubleHole);
                        continue;
                    }
                }

                if (quadHoles.getValue()) {
                    HoleUtils.Hole quadHole = HoleUtils.getQuadHole(position, 0);
                    if (quadHole != null) {
                        holes.add(quadHole);
                    }
                }
            }

            synchronized (this.holes) {
                this.holes.clear();
                this.holes.addAll(holes);
            }
        };

        if (asynchronous.getValue()) executor.submit(runnable);
        else runnable.run();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.world == null) return;

        synchronized (holes) {
            if (holes.isEmpty()) return;

            for (HoleUtils.Hole hole : holes) {
                Box filledBox = new Box(hole.box().minX, hole.box().minY, hole.box().minZ, hole.box().maxX, hole.box().minY + fillHeight.getValue().doubleValue(), hole.box().maxZ);
                Box outlinedBox = new Box(hole.box().minX, hole.box().minY, hole.box().minZ, hole.box().maxX, hole.box().minY + outlineHeight.getValue().doubleValue(), hole.box().maxZ);

                if (fill.getValue().equalsIgnoreCase("Normal")) Renderer3D.renderBox(event.getMatrices(), filledBox, getFillColor(hole));
                if (fill.getValue().equalsIgnoreCase("Gradient")) Renderer3D.renderGradientBox(event.getMatrices(), filledBox, fillHeight.getValue().floatValue() < 0.0f ? getFillColor(hole) : new Color(0, 0, 0, 0), fillHeight.getValue().floatValue() < 0.0f ? new Color(0, 0, 0, 0) : getFillColor(hole));

                if (outline.getValue().equalsIgnoreCase("Normal")) Renderer3D.renderBoxOutline(event.getMatrices(), outlinedBox, getOutlineColor(hole));
                if (outline.getValue().equalsIgnoreCase("Gradient")) Renderer3D.renderGradientBoxOutline(event.getMatrices(), outlinedBox, outlineHeight.getValue().floatValue() < 0.0f ? getOutlineColor(hole) : new Color(0, 0, 0, 0), outlineHeight.getValue().floatValue() < 0.0f ? new Color(0, 0, 0, 0) : getOutlineColor(hole));
            }
        }
    }

    @Override
    public String getMetaData() {
        return String.valueOf(holes.size());
    }

    private Color getFillColor(HoleUtils.Hole hole) {
        Color color = switch (hole.safety()) {
            case BEDROCK -> bedrockFillColor.getColor();
            case MIXED -> mixedFillColor.getColor();
            default -> obsidianFillColor.getColor();
        };
        if(!fade.getValue()) return color;

        return ColorUtils.getColor(color, (int) (color.getAlpha() * getEasing(hole)));
    }

    private Color getOutlineColor(HoleUtils.Hole hole) {
        Color color = switch (hole.safety()) {
            case BEDROCK -> bedrockOutlineColor.getColor();
            case MIXED -> mixedOutlineColor.getColor();
            default -> obsidianOutlineColor.getColor();
        };
        if(!fade.getValue()) return color;

        return ColorUtils.getColor(color, (int) (color.getAlpha() * getEasing(hole)));
    }

    private float getEasing(HoleUtils.Hole hole) {
        float scale = (float) (1.0f - MathHelper.clamp(Math.sqrt(mc.player.squaredDistanceTo(hole.box().getCenter())) / range.getValue().doubleValue(), 0.0f, 1.0f));
        return Easing.ease(scale, Easing.Method.EASE_OUT_CUBIC);
    }
}
