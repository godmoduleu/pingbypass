package eu.client.gui.impl;

import eu.client.EUClient;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.settings.impl.BindSetting;
import eu.client.utils.graphics.Renderer2D;
import eu.client.utils.input.KeyboardUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class BindButton extends Button {
    private final BindSetting setting;
    private boolean listening = false;

    public BindButton(BindSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Setting background
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getWidth() - getPadding() - 1, getY() + getHeight() - 1, new Color(0, 0, 0, 40));
        EUClient.FONT_MANAGER.drawTextWithShadow(context, setting.getTag(), getX() + getTextPadding() + 3, getY() + 2, Color.WHITE);

        String bind = listening ? "..." : KeyboardUtils.getKeyName(setting.getValue());
        EUClient.FONT_MANAGER.drawTextWithShadow(context, Formatting.GRAY + bind, getX() + getWidth() - getTextPadding() - 1 - EUClient.FONT_MANAGER.getWidth(bind), getY() + 2, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                listening = true;
                playClickSound();
            } else {
                setting.setValue(0);
            }

            if(listening) {
                if (button == 1 || button == 2 || button == 3 || button == 4) {
                    setting.setValue(-button - 1);
                    listening = false;
                }
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if(listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                setting.setValue(0);
            } else {
                setting.setValue(keyCode);
            }
            listening = false;
        }
    }
}
