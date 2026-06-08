package eu.client.modules.impl.visuals;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.NumberSetting;

import java.awt.*;

@RegisterModule(name = "EntityModifier", description = "Lets you customize the rendering of entities.", category = Module.Category.VISUALS)
public class EntityModifierModule extends Module {
    public CategorySetting playersCategory = new CategorySetting("Players", "The category for the settings related to player models.");
    public BooleanSetting players = new BooleanSetting("Players", "Enabled", "Whether or not to modify player rendering.", new CategorySetting.Visibility(playersCategory), true);
    public ColorSetting playerColor = new ColorSetting("PlayerColor", "Color", "The color that will be applied to the player models.", new CategorySetting.Visibility(playersCategory), new ColorSetting.Color(new Color(255, 255, 255, 255), false, false));
    public NumberSetting playerScale = new NumberSetting("PlayerScale", "Scale", "The scale that will be applied to the player models.", new CategorySetting.Visibility(playersCategory), 1.0, 0.0, 10.0);

    public CategorySetting crystalsCategory = new CategorySetting("Crystals", "The category for the settings related to crystal models.");
    public BooleanSetting crystals = new BooleanSetting("Crystals", "Enabled", "Whether or not to modify crystal rendering.", new CategorySetting.Visibility(crystalsCategory), true);
    public ColorSetting crystalColor = new ColorSetting("CrystalColor", "Color", "The color that will be applied to the crystal models.", new CategorySetting.Visibility(crystalsCategory), new ColorSetting.Color(new Color(255, 255, 255, 255), false, false));
    public NumberSetting crystalScale = new NumberSetting("CrystalScale", "Scale", "The scale that will be applied to the crystal models.", new CategorySetting.Visibility(crystalsCategory), 1.0, 0.0, 10.0);
    public NumberSetting crystalSpeed = new NumberSetting("CrystalSpeed", "Speed", "The modifier that will be applied to the speed of crystal rotations.", new CategorySetting.Visibility(crystalsCategory), 1.0f, 0.0f, 10.0f);
    public NumberSetting crystalBounce = new NumberSetting("CrystalBounce", "Bounce", "The modifier that will be applied to the bouncing of crystals.", new CategorySetting.Visibility(crystalsCategory), 1.0f, 0.0f, 10.0f);

    public CategorySetting itemsCategory = new CategorySetting("Items", "The category for the settings related to item models.");
    public BooleanSetting items = new BooleanSetting("Items", "Enabled", "Whether or not to modify item rendering.", new CategorySetting.Visibility(itemsCategory), true);
    public ColorSetting itemColor = new ColorSetting("ItemColor", "Color", "The color that will be applied to the item models.", new CategorySetting.Visibility(itemsCategory), new ColorSetting.Color(new Color(255, 255, 255, 255), false, false));
    public BooleanSetting itemGlobal = new BooleanSetting("ItemGlobal", "Global", "Applies the modifications to every single item regardless of where it is.", new CategorySetting.Visibility(itemsCategory), false);
}
