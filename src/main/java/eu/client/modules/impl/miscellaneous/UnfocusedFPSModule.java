package eu.client.modules.impl.miscellaneous;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.NumberSetting;

@RegisterModule(name = "UnfocusedFPS", description = "Limits your FPS when the game window is out of focus.", category = Module.Category.MISCELLANEOUS)
public class UnfocusedFPSModule extends Module {
    public NumberSetting limit = new NumberSetting("Limit", "The FPS limit that will be enforced once the game is out of focus.", 30, 10, 60);

    @Override
    public String getMetaData() {
        return String.valueOf(limit.getValue().intValue());
    }
}
