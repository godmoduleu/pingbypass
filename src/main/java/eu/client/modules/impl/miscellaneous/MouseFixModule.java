package eu.client.modules.impl.miscellaneous;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;

@RegisterModule(name = "MouseFix", description = "Fixes multiple mouse issues.", category = eu.client.modules.Module.Category.MISCELLANEOUS)
public class MouseFixModule extends Module {
    public BooleanSetting customDebounce = new BooleanSetting("CustomDebounce", "Implements a custom debounce timer on mouse inputs.", true);
}
