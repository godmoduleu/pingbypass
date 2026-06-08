package eu.client.modules.impl.visuals;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;

@RegisterModule(name = "HandProgress", description = "Modifies the progress of the items that you're holding.", category = Module.Category.VISUALS)
public class HandProgressModule extends Module {
    public BooleanSetting modifyMainhand = new BooleanSetting("ModifyMainhand", "Modifies the mainhand's progress.", true);
    public NumberSetting mainhandProgress = new NumberSetting("MainhandProgress", "Progress", "The progress for the mainhand.", new BooleanSetting.Visibility(modifyMainhand, true), 1.0f, -1.0f, 1.0f);
    public BooleanSetting modifyOffhand = new BooleanSetting("ModifyOffhand", "Modifies the offhand's progress.", true);
    public NumberSetting offhandProgress = new NumberSetting("OffhandProgress", "Progress", "The progress for the offhand.", new BooleanSetting.Visibility(modifyOffhand, true), 1.0f, -1.0f, 1.0f);

    public BooleanSetting staticEating = new BooleanSetting("StaticEating", "Cancel eating animation.", false);
}
