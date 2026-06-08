package eu.client.gui.impl;

import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.settings.impl.CategorySetting;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class CategoryButton extends Button {
    private final CategorySetting setting;

    public CategoryButton(CategorySetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Setting background
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getWidth() - getPadding() - 1, getY() + getHeight() - 1, new Color(0, 0, 0, 40));
        // Subtle colored line on the left edge to mark this as a page/section header
        Color accentColor = ClickGuiScreen.getButtonColor(getY(), 180);
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY() + 2, getX() + getPadding() + 2, getY() + getHeight() - 3, accentColor);

        EUClient.FONT_MANAGER.drawTextWithShadow(context, setting.getTag(), getX() + getTextPadding() + 4, getY() + 2, accentColor);
        EUClient.FONT_MANAGER.drawTextWithShadow(context, setting.isOpen() ? "-" : "+", getX() + getWidth() - getTextPadding() - 1 - EUClient.FONT_MANAGER.getWidth(setting.isOpen() ? "-" : "+"), getY() + 2, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY) && (button == 0 || button == 1)) {
            setting.setOpen(!setting.isOpen());
            playClickSound();
        }
    }
}
