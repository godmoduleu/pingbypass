package eu.client.modules.impl.movement;

import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.NumberSetting;

@RegisterModule(name = "KeepSprint", description = "Makes it so that you're sprinting even after attacking.", category = Module.Category.MOVEMENT)
public class KeepSprintModule extends Module {
    public NumberSetting motion = new NumberSetting("Motion", "The velocity that will be applied to your movement when attacking.", 1.0f, 0.0f, 1.0f);

    @Override
    public String getMetaData() {
        return String.valueOf(motion.getValue().doubleValue());
    }
}
