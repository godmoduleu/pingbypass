package eu.client.modules.impl.core;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.CategorySetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.color.ColorUtils;

import java.awt.*;

@RegisterModule(name = "Color", description = "Manages the client's global color system.", category = Module.Category.CORE, persistent = true, drawn = false)
public class ColorModule extends Module {
    public ColorSetting color = new ColorSetting("Color", "The global color that is used in sync and in most of the elements.", new ColorSetting.Color(new Color(130, 202, 255), false, false));

    public CategorySetting rainbowCategory = new CategorySetting("Rainbow", "The category containing all settings related to rainbow coloring.");
    public NumberSetting rainbowSpeed = new NumberSetting("RainbowSpeed", "Speed", "The speed that rainbow colors will be cycling at.", new CategorySetting.Visibility(rainbowCategory), 6L, 1L, 20L);
    public NumberSetting rainbowSaturation = new NumberSetting("RainbowSaturation", "Saturation", "The saturation value of the rainbow color.", new CategorySetting.Visibility(rainbowCategory), 100.0f, 0.0f, 100.0f);
    public NumberSetting rainbowBrightness = new NumberSetting("RainbowBrightness", "Brightness", "The brightness value of the rainbow color.", new CategorySetting.Visibility(rainbowCategory), 100.0f, 0.0f, 100.0f);
}
