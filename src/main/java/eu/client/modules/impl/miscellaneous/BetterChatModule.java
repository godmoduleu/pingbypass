package eu.client.modules.impl.miscellaneous;

import lombok.Getter;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.*;
import net.minecraft.client.gui.hud.ChatHudLine;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@RegisterModule(name = "BetterChat", description = "Improves the default minecraft chat.", category = Module.Category.MISCELLANEOUS)
public class BetterChatModule extends Module {
    public BooleanSetting noIndicators = new BooleanSetting("NoIndicators", "Removes indicators from the chat.", false);
    public NumberSetting offset = new NumberSetting("Offset", "The offset for the chat on the X axis.", 0, -100, 100);
    public BooleanSetting animation = new BooleanSetting("Animation", "Adds in an animation when adding new messages.", false);
    public NumberSetting delay = new NumberSetting("Delay", "The delay of the animation.", new BooleanSetting.Visibility(animation, true), 200, 0, 300);
    public NumberSetting textAlpha = new NumberSetting("TextAlpha", "The alpha of the chat text.", 255, 0, 255);
    public BooleanSetting timestamps = new BooleanSetting("Timestamps", "Adds timestamps to every chat message.", true);
    public StringSetting opening = new StringSetting("Opening", "The symbol that will be placed before the timestamp text.", new BooleanSetting.Visibility(timestamps, true), "<");
    public StringSetting closing = new StringSetting("Closing", "The symbol that will be placed after the timestamp text.", new BooleanSetting.Visibility(timestamps, true), ">");
    public ModeSetting background = new ModeSetting("Background", "The background mode for the chat.", "Default", new String[]{"Default", "Clear", "Custom"});
    public ColorSetting color = new ColorSetting("Color", "The color of the chat background.", new ModeSetting.Visibility(background, "Custom"), new ColorSetting.Color(new Color(0, 0, 0, 127), false, false));

    @Getter private final Map<ChatHudLine.Visible, Long> animationMap = new HashMap<>();
}
