package eu.client.modules.impl.visuals;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.events.impl.TickEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.*;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer3D;
import eu.client.utils.minecraft.PositionUtils;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.awt.*;
import java.util.ArrayList;

@RegisterModule(name = "PhaseESP", description = "Renders ESP on blocks which have a safe base.", category = Module.Category.VISUALS)
public class PhaseESPModule extends Module {
    public ModeSetting fill = new ModeSetting("Fill", "The mode for the fill rendering on the phase blocks.", "Normal", new String[]{"None", "Normal"});
    public NumberSetting fillHeight = new NumberSetting("FillHeight", "The height of the fill rendering on the phase blocks.", new ModeSetting.Visibility(fill, "Normal"), 1.0, -2.0, 2.0);
    public ModeSetting outline = new ModeSetting("Outline", "The mode for the outline rendering on the phase blocks.", "Normal", new String[]{"None", "Normal"});
    public NumberSetting outlineHeight = new NumberSetting("OutlineHeight", "The height of the outline rendering on the phase blocks.", new ModeSetting.Visibility(outline, "Normal"), 1.0, -2.0, 2.0);

    public CategorySetting bedrockCategory = new CategorySetting("Bedrock", "The bedrock base category.");
    public BooleanSetting bedrock = new BooleanSetting("Bedrock", "Enabled", "Renders blocks which have a bedrock base.", new CategorySetting.Visibility(bedrockCategory), true);
    public ColorSetting bedrockFillColor = new ColorSetting("BedrockFillColor", "Fill", "The color for the fill rendering of bedrock base blocks.", new CategorySetting.Visibility(bedrockCategory), new ColorSetting.Color(new Color(0, 255, 0, ColorUtils.getDefaultFillColor().getColor().getAlpha()), false, false));
    public ColorSetting bedrockOutlineColor = new ColorSetting("BedrockOutlineColor", "Outline", "The color for the outline rendering of bedrock base blocks.", new CategorySetting.Visibility(bedrockCategory), new ColorSetting.Color(new Color(0, 255, 0, ColorUtils.getDefaultOutlineColor().getColor().getAlpha()), false, false));

    public CategorySetting obsidianCategory = new CategorySetting("Obsidian", "The obsidian base category.");
    public BooleanSetting obsidian = new BooleanSetting("Obsidian", "Enabled", "Renders blocks which have a obsidian base.", new CategorySetting.Visibility(obsidianCategory), true);
    public ColorSetting obsidianFillColor = new ColorSetting("ObsidianFillColor", "Fill", "The color for the fill rendering of obsidian base blocks.", new CategorySetting.Visibility(obsidianCategory), new ColorSetting.Color(new Color(255, 255, 0, ColorUtils.getDefaultFillColor().getColor().getAlpha()), false, false));
    public ColorSetting obsidianOutlineColor = new ColorSetting("ObsidianOutlineColor", "Outline", "The color for the outline rendering of obsidian base blocks.", new CategorySetting.Visibility(obsidianCategory), new ColorSetting.Color(new Color(255, 255, 0, ColorUtils.getDefaultOutlineColor().getColor().getAlpha()), false, false));

    public CategorySetting airCategory = new CategorySetting("Air", "The air base category.");
    public BooleanSetting air = new BooleanSetting("Air", "Enabled", "Renders blocks which have a air base.", new CategorySetting.Visibility(airCategory), true);
    public ColorSetting airFillColor = new ColorSetting("AirFillColor", "Fill", "The color for the fill rendering of air base blocks.", new CategorySetting.Visibility(airCategory), new ColorSetting.Color(new Color(255, 0, 0, ColorUtils.getDefaultFillColor().getColor().getAlpha()), false, false));
    public ColorSetting airOutlineColor = new ColorSetting("AirOutlineColor", "Outline", "The color for the outline rendering of air base blocks.", new CategorySetting.Visibility(airCategory), new ColorSetting.Color(new Color(255, 0, 0, ColorUtils.getDefaultOutlineColor().getColor().getAlpha()), false, false));

    private enum PhaseType {
        BEDROCK, OBSIDIAN, AIR
    }

    private ArrayList<PhaseBlock> phaseBlocks = new ArrayList<>();

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if(getNull()) return;

        ArrayList<PhaseBlock> phaseBlocks = new ArrayList<>();
        BlockPos playerPos = PositionUtils.getFlooredPosition(mc.player);

        for(Direction direction : Direction.values()) {
            if(!direction.getAxis().isHorizontal()) continue;

            BlockPos offsetPos = playerPos.offset(direction);
            if(mc.world.getBlockState(offsetPos).getBlock() != Blocks.BEDROCK && mc.world.getBlockState(offsetPos).getBlock() != Blocks.OBSIDIAN) continue;

            PhaseType type = getPhaseType(offsetPos);
            if(type != null) {
                phaseBlocks.add(new PhaseBlock(new Box(offsetPos), type));
            }
        }

        this.phaseBlocks.clear();
        this.phaseBlocks.addAll(phaseBlocks);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if(getNull() || phaseBlocks.isEmpty()) return;

        for(PhaseBlock phaseBlock : phaseBlocks) {
            Box filledBox = new Box(phaseBlock.box().minX, phaseBlock.box().minY, phaseBlock.box().minZ, phaseBlock.box().maxX, phaseBlock.box().minY + fillHeight.getValue().doubleValue(), phaseBlock.box().maxZ);
            Box outlinedBox = new Box(phaseBlock.box().minX, phaseBlock.box().minY, phaseBlock.box().minZ, phaseBlock.box().maxX, phaseBlock.box().minY + outlineHeight.getValue().doubleValue(), phaseBlock.box().maxZ);

            if (fill.getValue().equalsIgnoreCase("Normal")) Renderer3D.renderBox(event.getMatrices(), filledBox, getFillColor(phaseBlock.type()));

            if (outline.getValue().equalsIgnoreCase("Normal")) Renderer3D.renderBoxOutline(event.getMatrices(), outlinedBox, getOutlineColor(phaseBlock.type()));
        }
    }

    private Color getFillColor(PhaseType type) {
        return switch (type) {
            case BEDROCK -> bedrockFillColor.getColor();
            case OBSIDIAN -> obsidianFillColor.getColor();
            default -> airFillColor.getColor();
        };
    }

    private Color getOutlineColor(PhaseType type) {
        return switch (type) {
            case BEDROCK -> bedrockOutlineColor.getColor();
            case OBSIDIAN -> obsidianOutlineColor.getColor();
            default -> airOutlineColor.getColor();
        };
    }

    private PhaseType getPhaseType(BlockPos pos) {
        if(mc.world.getBlockState(pos.down()).getBlock() == Blocks.BEDROCK && bedrock.getValue()) return PhaseType.BEDROCK;
        if(mc.world.getBlockState(pos.down()).getBlock() == Blocks.OBSIDIAN && obsidian.getValue()) return PhaseType.OBSIDIAN;
        if(mc.world.getBlockState(pos.down()).getBlock() == Blocks.AIR && air.getValue()) return PhaseType.AIR;
        return null;
    }

    public record PhaseBlock(Box box, PhaseType type) { }
}
