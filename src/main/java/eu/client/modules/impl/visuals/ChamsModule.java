package eu.client.modules.impl.visuals;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.RenderWorldEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.ModelRenderer;
import eu.client.utils.graphics.Renderer3D;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;

@RegisterModule(name = "Chams", description = "Adds a customizable render on top of the default Minecraft rendering.", category = Module.Category.VISUALS)
public class ChamsModule extends Module {
    public CategorySetting entitiesCategory = new CategorySetting("Entities", "The category for settings related to chams rendered on living entities.");
    public BooleanSetting players = new BooleanSetting("Players", "Renders the chams on player entities.", new CategorySetting.Visibility(entitiesCategory), true);
    public BooleanSetting hostiles = new BooleanSetting("Hostiles", "Renders the chams on hostile entities.", new CategorySetting.Visibility(entitiesCategory), false);
    public BooleanSetting passives = new BooleanSetting("Passives", "Renders the chams on passive entities.", new CategorySetting.Visibility(entitiesCategory), false);
    public BooleanSetting entityPulse = new BooleanSetting("EntityPulse", "Pulse", "Adds a pulsing effect to the chams opacity.", new CategorySetting.Visibility(entitiesCategory), false);
    public BooleanSetting entityShine = new BooleanSetting("EntityShine", "Shine", "Adds a shine effect to the chams.", new CategorySetting.Visibility(entitiesCategory), false);
    public ModeSetting entityMode = new ModeSetting("EntityMode", "Mode", "The rendering that will be applied to living entities.", new CategorySetting.Visibility(entitiesCategory), "Both", new String[]{"Fill", "Outline", "Both"});
    public ColorSetting entityFillColor = new ColorSetting("EntityFillColor", "FillColor", "The color that will be used for the fill rendering.", new ModeSetting.Visibility(entityMode, "Fill", "Both"), ColorUtils.getDefaultFillColor());
    public ColorSetting entityOutlineColor = new ColorSetting("EntityOutlineColor", "OutlineColor", "The color that will be used for the outline rendering.", new ModeSetting.Visibility(entityMode, "Outline", "Both"), ColorUtils.getDefaultOutlineColor());
    public ModeSetting friendMode = new ModeSetting("FriendMode",  "The mode for the friend color.", new ModeSetting.Visibility(entityMode, "Fill", "Both"), "Default", new String[]{"Default", "Custom", "Sync"});
    public ColorSetting friendFillColor = new ColorSetting("FriendFillColor", "The color that will be used for the fill rendering on friends.", new ModeSetting.Visibility(entityMode, "Fill", "Both"), new ColorSetting.Color(new Color(85, 255, 255, ColorUtils.getDefaultFillColor().getColor().getAlpha()), false, false));
    public ColorSetting friendOutlineColor = new ColorSetting("FriendOutlineColor", "The color that will be used for the outline rendering on friends.", new ModeSetting.Visibility(entityMode, "Outline", "Both"), new ColorSetting.Color(new Color(85, 255, 255, ColorUtils.getDefaultOutlineColor().getColor().getAlpha()), false, false));
    public BooleanSetting damageModify = new BooleanSetting("DamageModify", "Changes the color of the chams when a player takes damage.", new ModeSetting.Visibility(entityMode, "Fill", "Both"), false);
    public ColorSetting damageColor = new ColorSetting("DamageColor", "The color to apply on chams when a player takes damage.", new BooleanSetting.Visibility(damageModify, true), new ColorSetting.Color(new Color(255, 0, 0), false, false));

    public CategorySetting crystalsCategory = new CategorySetting("Crystals", "The category for settings related to crystal chams.");
    public BooleanSetting crystals = new BooleanSetting("Crystals", "Enabled", "Renders the chams on crystal entities.", new CategorySetting.Visibility(crystalsCategory), true);
    public BooleanSetting crystalPulse = new BooleanSetting("CrystalPulse", "Pulse", "Adds a pulsing effect to the chams opacity.", new CategorySetting.Visibility(crystalsCategory), false);
    public BooleanSetting crystalShine = new BooleanSetting("CrystalShine", "Shine", "Adds a shine effect on crystal chams.", new CategorySetting.Visibility(crystalsCategory), false);
    public ModeSetting crystalMode = new ModeSetting("CrystalMode", "Mode", "The rendering that will be applied to crystal entities.", new CategorySetting.Visibility(crystalsCategory), "Both", new String[]{"Fill", "Outline", "Both"});
    public ColorSetting crystalFillColor = new ColorSetting("CrystalFillColor", "FillColor", "The color that will be used for the fill rendering.", new ModeSetting.Visibility(crystalMode, "Fill", "Both"), ColorUtils.getDefaultFillColor());
    public ColorSetting crystalOutlineColor = new ColorSetting("CrystalOutlineColor", "OutlineColor", "The color that will be used for the outline rendering.", new ModeSetting.Visibility(crystalMode, "Outline", "Both"), ColorUtils.getDefaultOutlineColor());

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (!Renderer3D.isFrustumVisible(entity.getBoundingBox())) continue;

            if (entity instanceof LivingEntity livingEntity) {
                if (!isValidEntity(livingEntity)) continue;

                boolean flag = damageModify.getValue() && livingEntity.hurtTime > 0;
                Color friendFill = flag ? ColorUtils.getColor(damageColor.getColor(), friendFillColor.getColor().getAlpha()) : (friendMode.getValue().equals("Default") ? EUClient.FRIEND_MANAGER.getDefaultFriendColor(friendFillColor.getColor().getAlpha()) : friendFillColor.getColor());
                Color friendOutline = flag ? ColorUtils.getColor(damageColor.getColor(), friendOutlineColor.getColor().getAlpha()) : (friendMode.getValue().equals("Default") ? EUClient.FRIEND_MANAGER.getDefaultFriendColor(friendOutlineColor.getColor().getAlpha()) : friendOutlineColor.getColor());
                Color fillColor = (livingEntity instanceof PlayerEntity player && EUClient.FRIEND_MANAGER.contains(player.getName().getString()) && !friendMode.getValue().equals("Sync")) ? friendFill : flag ? ColorUtils.getColor(damageColor.getColor(), entityFillColor.getColor().getAlpha()) : entityFillColor.getColor();
                Color outlineColor = (livingEntity instanceof PlayerEntity player && EUClient.FRIEND_MANAGER.contains(player.getName().getString()) && !friendMode.getValue().equals("Sync")) ? friendOutline : flag ? ColorUtils.getColor(damageColor.getColor(), entityOutlineColor.getColor().getAlpha()) : entityOutlineColor.getColor();

                ModelRenderer.renderModel(livingEntity, 1.0f, event.getTickDelta(), new ModelRenderer.Render(entityMode.getValue().equals("Fill") || entityMode.getValue().equals("Both"), entityPulse.getValue() ? ColorUtils.getPulse(fillColor) : fillColor, entityMode.getValue().equals("Outline") || entityMode.getValue().equals("Both"), entityPulse.getValue() ? ColorUtils.getPulse(outlineColor) : outlineColor, entityShine.getValue()));
            }

            if (crystals.getValue() && entity instanceof EndCrystalEntity crystal) {
                ModelRenderer.renderModel(crystal, 1.0f, event.getTickDelta(), new ModelRenderer.Render(crystalMode.getValue().equals("Fill") || crystalMode.getValue().equals("Both"), crystalPulse.getValue() ? ColorUtils.getPulse(crystalFillColor.getColor()) : crystalFillColor.getColor(), crystalMode.getValue().equals("Outline") || crystalMode.getValue().equals("Both"), crystalPulse.getValue() ? ColorUtils.getPulse(crystalOutlineColor.getColor()) : crystalOutlineColor.getColor(), crystalShine.getValue()));
            }
        }
    }

    private boolean isValidEntity(Entity entity) {
        if (players.getValue() && entity.getType() == EntityType.PLAYER) return true;
        if (hostiles.getValue() && entity.getType().getSpawnGroup() == SpawnGroup.MONSTER) return true;
        return passives.getValue() && (entity.getType().getSpawnGroup() == SpawnGroup.CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.WATER_CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.WATER_AMBIENT || entity.getType().getSpawnGroup() == SpawnGroup.UNDERGROUND_WATER_CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.AXOLOTLS);
    }
}
