package eu.client.modules.impl.miscellaneous;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.StringSetting;

@RegisterModule(name = "NameProtect", description = "Hides your current in game name.", category = Module.Category.MISCELLANEOUS)
public class NameProtectModule extends Module {
    public StringSetting name = new StringSetting("Name", "The name to use as a replacement.", "EUClient");
}
