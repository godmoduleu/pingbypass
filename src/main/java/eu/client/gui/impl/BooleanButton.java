package eu.client.gui.impl;

import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.settings.impl.BooleanSetting;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;

public class BooleanButton extends Button {
    private final BooleanSetting setting;

    public BooleanButton(BooleanSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Setting background — darker to distinguish from module rows
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getWidth() - getPadding() - 1, getY() + getHeight() - 1, new Color(0, 0, 0, 40));
        if(setting.getValue()) {
            Color accentColor = ClickGuiScreen.getButtonColor(getY(), 220);
            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY() + 1, getX() + getPadding() + 3, getY() + getHeight() - 2, accentColor);
        }
        EUClient.FONT_MANAGER.drawTextWithShadow(context, (setting.getValue() ? "" : Formatting.GRAY) + setting.getTag(), getX() + getTextPadding() + 3, getY() + 2, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY) && button == 0) {
            setting.setValue(!setting.getValue());
            playClickSound();
        }
    }
}
