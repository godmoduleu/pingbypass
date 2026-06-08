package eu.client.gui.impl;

import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.settings.impl.NumberSetting;
import eu.client.utils.chat.ChatUtils;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer2D;
import eu.client.utils.system.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class NumberButton extends Button {
    private final NumberSetting setting;
    private boolean dragging = false;
    private String currentString = "";
    private boolean listening = false, selecting = false;

    public NumberButton(NumberSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        double slider, sliderMax = getWidth() - 2 - getPadding()*2;
        double drag = Math.min(sliderMax, Math.max(0, mouseX - getX() - 1 - getPadding()));

        if(setting.getType() == NumberSetting.Type.INTEGER) {
            slider = sliderMax * (setting.getValue().intValue() - setting.getMinimum().intValue()) / (setting.getMaximum().intValue() - setting.getMinimum().intValue());
            if(dragging) {
                int value = (int) MathUtils.round(drag / sliderMax * (setting.getMaximum().intValue() - setting.getMinimum().intValue()) + setting.getMinimum().intValue(), 0);
                setting.setValue(value);
            }
        } else if(setting.getType() == NumberSetting.Type.FLOAT) {
            slider = sliderMax * (setting.getValue().floatValue() - setting.getMinimum().floatValue()) / (setting.getMaximum().floatValue() - setting.getMinimum().floatValue());
            if(dragging) {
                float value = (float) MathUtils.round(drag / sliderMax * (setting.getMaximum().floatValue() - setting.getMinimum().floatValue()) + setting.getMinimum().floatValue(), 2);
                setting.setValue(value);
            }
        } else if(setting.getType() == NumberSetting.Type.DOUBLE) {
            slider = sliderMax * (setting.getValue().doubleValue() - setting.getMinimum().doubleValue()) / (setting.getMaximum().doubleValue() - setting.getMinimum().doubleValue());
            if(dragging) {
                double value = MathUtils.round(drag / sliderMax * (setting.getMaximum().doubleValue() - setting.getMinimum().doubleValue()) + setting.getMinimum().doubleValue(), 2);
                setting.setValue(value);
            }
        } else {
            slider = sliderMax * (setting.getValue().longValue() - setting.getMinimum().longValue()) / (setting.getMaximum().longValue() - setting.getMinimum().longValue());
            if(dragging) {
                long value = (long) MathUtils.round(drag / sliderMax * (setting.getMaximum().longValue() - setting.getMinimum().longValue()) + setting.getMinimum().longValue(), 0);
                setting.setValue(value);
            }
        }

        // Setting background
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getWidth() - getPadding() - 1, getY() + getHeight() - 1, new Color(0, 0, 0, 40));
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getPadding() + 1 + (float) slider, getY() + getHeight() - 1, ClickGuiScreen.getButtonColor(getY(), 100));
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getPadding() + 2, getY() + getHeight() - 1, ClickGuiScreen.getButtonColor(getY(), 255));

        EUClient.FONT_MANAGER.drawTextWithShadow(context, listening ? (currentString + (selecting ? "" : EUClient.CLICK_GUI.isShowLine() ? "|" : "")) : setting.getTag(), getX() + getTextPadding() + 3, getY() + 2, Color.WHITE);
        if(!listening) EUClient.FONT_MANAGER.drawTextWithShadow(context, Formatting.GRAY + "" + setting.getValue(), getX()+ getWidth() - getTextPadding() - 1 - EUClient.FONT_MANAGER.getWidth(setting.getValue() + ""), getY() + 2, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY) && button == 0) {
            dragging = true;
        }

        if(button == 1) {
            if(isHovering(mouseX, mouseY) && !listening) {
                listening = true;
                currentString = "";
            } else {
                listening = false;
                selecting = false;
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if(button == 0) {
            if(dragging) playClickSound();
            dragging = false;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if(!listening) return;

        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE)) {
            selecting = false;
            return;
        }

        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_ENTER)) {
            try {
                switch (setting.getType()) {
                    case LONG -> setting.setValue(Long.parseLong(currentString));
                    case DOUBLE -> setting.setValue(Double.parseDouble(currentString));
                    case FLOAT -> setting.setValue(Float.parseFloat(currentString));
                    default -> setting.setValue(Integer.parseInt(currentString));
                }
            } catch (NumberFormatException exception) {
                EUClient.CHAT_MANAGER.warn("Please input a valid " + ChatUtils.getPrimary() + setting.getType().name().toLowerCase() + ChatUtils.getSecondary() + " number.");
            }
            selecting = false;
            listening = false;
            return;
        }

        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_BACKSPACE)) {
            currentString = selecting ? "" : (!currentString.isEmpty() ? currentString.substring(0, currentString.length() - 1) : currentString);
            selecting = false;
            return;
        }

        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), MinecraftClient.IS_SYSTEM_MAC ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL)) {
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_A)) {
                selecting = true;
            }
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (listening) {
            currentString = selecting ? String.valueOf(chr) : currentString + chr;
            if(selecting) selecting = false;
        }
    }
}
