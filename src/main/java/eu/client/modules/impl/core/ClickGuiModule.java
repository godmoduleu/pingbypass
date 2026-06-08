package eu.client.modules.impl.core;

import eu.client.EUClient;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ColorSetting;
import eu.client.settings.impl.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@RegisterModule(name = "ClickGui", description = "Allows you to change and interact with the client's modules and settings through a GUI.", category = Module.Category.CORE, drawn = false, bind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGuiModule extends Module {
    public BooleanSetting sounds = new BooleanSetting("Sounds", "Plays Minecraft UI sounds when interacting with the client's GUI.", true);
    public BooleanSetting blur = new BooleanSetting("Blur", "Whether or not to blur the background behind the GUI.", true);
    public NumberSetting scrollSpeed = new NumberSetting("ScrollSpeed", "The speed at which the scrolling of the frames will be at.", 15, 1, 50);
    public ColorSetting color = new ColorSetting("Color", "The color that will be used in the GUI.", new ColorSetting.Color(new Color(160, 120, 255), true, false));

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setToggled(false);
            return;
        }

        mc.setScreen(EUClient.CLICK_GUI);
    }

    @Override
    public void onDisable() {
        mc.setScreen(null);
    }

    public boolean isRainbow() {
        if(color.isSync()) return EUClient.MODULE_MANAGER.getModule(ColorModule.class).color.isRainbow();
        return color.isRainbow();
    }
}
