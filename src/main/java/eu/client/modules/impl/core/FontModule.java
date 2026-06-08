package eu.client.modules.impl.core;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.SettingChangeEvent;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.*;
import eu.client.utils.font.FontRenderer;

import java.awt.*;

@RegisterModule(name = "Font", description = "Manages the client and the game's font rendering.", category = Module.Category.CORE, drawn = false)
public class FontModule extends Module {
    public CategorySetting customFontCategory = new CategorySetting("CustomFont", "The category for settings relating to custom fonts.");
    public BooleanSetting customFont = new BooleanSetting("CustomFont", "Enabled", "Enables custom font rendering.", new CategorySetting.Visibility(customFontCategory), false);
    public StringSetting name = new StringSetting("Name", "The name of the font that will be rendered.", new CategorySetting.Visibility(customFontCategory), "Verdana");
    public NumberSetting size = new NumberSetting("Size", "The size of the custom font that will be rendered.", new CategorySetting.Visibility(customFontCategory), 18, 8, 48);
    public ModeSetting style = new ModeSetting("Style", "The style that will be used in the font's rendering.", new CategorySetting.Visibility(customFontCategory), "Plain", new String[]{"Plain", "Bold", "Italic", "BoldItalic"});
    public BooleanSetting global = new BooleanSetting("Global", "Applies the custom font rendering on every part of the game.", new CategorySetting.Visibility(customFontCategory), false);

    public CategorySetting offsetsCategory = new CategorySetting("Offsets", "Allows you to offset the custom font rendering to make it render perfectly.");
    public NumberSetting xOffset = new NumberSetting("XOffset", "The offset that will be applied to the font on the X axis.", new CategorySetting.Visibility(offsetsCategory), 0, -10, 10);
    public NumberSetting yOffset = new NumberSetting("YOffset", "The offset that will be applied to the font on the Y axis.", new CategorySetting.Visibility(offsetsCategory), 0, -10, 10);
    public NumberSetting widthOffset = new NumberSetting("WidthOffset", "The offset that will be applied to the font on the X axis.", new CategorySetting.Visibility(offsetsCategory), 0, -10, 10);
    public NumberSetting heightOffset = new NumberSetting("HeightOffset", "The font's offset on the Y axis.", new CategorySetting.Visibility(offsetsCategory), 0, -10, 10);

    public CategorySetting shadowsCategory = new CategorySetting("Shadows", "The category for settings related to font shadows.");
    public ModeSetting shadowMode = new ModeSetting("ShadowMode", "Mode", "The way that the shadow will be rendered.", new CategorySetting.Visibility(shadowsCategory), "Default", new String[]{"None", "Default", "Custom"});
    public NumberSetting shadowOffset = new NumberSetting("ShadowOffset", "Offset", "The distance of the shadow from the text being rendered.", new ModeSetting.Visibility(shadowMode, "Custom"), 0.5f, -2.0f, 2.0f);

    @SubscribeEvent
    public void onSettingChange(SettingChangeEvent event) {
        if (event.getSetting() == name || event.getSetting() == size || event.getSetting() == style) {
            updateFontRenderer();
        }
    }

    @Override
    public void onEnable() {
        updateFontRenderer();
    }

    private void updateFontRenderer() {
        EUClient.FONT_MANAGER.setFontRenderer(new FontRenderer(new Font[]{new Font(name.getValue(), style.getValue().equalsIgnoreCase("BoldItalic") ? Font.BOLD | Font.ITALIC : style.getValue().equalsIgnoreCase("Bold") ? Font.BOLD : style.getValue().equalsIgnoreCase("Italic") ? Font.ITALIC : Font.PLAIN, size.getValue().intValue())}, size.getValue().floatValue() / 2.0f));
    }
}
