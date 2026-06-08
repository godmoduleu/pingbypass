package eu.client.modules.impl.miscellaneous;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.NumberSetting;

@RegisterModule(name = "ExtraTab", description = "Extends the size of the player list.", category = Module.Category.MISCELLANEOUS)
public class ExtraTabModule extends Module {
    public NumberSetting limit = new NumberSetting("Limit", "The maximum amount of players that will be listed.", 1000, 1, 1000);
    public BooleanSetting friends = new BooleanSetting("Friends", "Highlights your friends on the player list.", true);

    @Override
    public String getMetaData() {
        return String.valueOf(limit.getValue().intValue());
    }
}
