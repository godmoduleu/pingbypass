package eu.client.modules.impl.visuals;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.color.ColorUtils;

@RegisterModule(name = "Atmosphere", description = "Modifies the world's atmosphere, such as time and color.", category = Module.Category.VISUALS)
public class AtmosphereModule extends Module {
    public BooleanSetting modifyTime = new BooleanSetting("ModifyTime", "Modifies the world's time.", true);
    public NumberSetting time = new NumberSetting("Time", "The time that the world will be set to.", new BooleanSetting.Visibility(modifyTime, true), 200, -200, 200);
    public BooleanSetting modifyFog = new BooleanSetting("ModifyFog", "Modifies certain things about the world's fog.", false);
    public NumberSetting fogStart = new NumberSetting("FogStart", "The start value of the world's fog.", new BooleanSetting.Visibility(modifyFog, true), 50, 0, 300);
    public NumberSetting fogEnd = new NumberSetting("FogEnd", "The end value of the world's fog.", new BooleanSetting.Visibility(modifyFog, true), 150, 0, 300);
    public ColorSetting fogColor = new ColorSetting("FogColor", "Modifies the color of the world's fog.", new BooleanSetting.Visibility(modifyFog, true), ColorUtils.getDefaultOutlineColor());
}
