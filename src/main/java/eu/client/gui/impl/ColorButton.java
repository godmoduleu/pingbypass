package eu.client.gui.impl;

import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.modules.impl.core.ColorModule;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.settings.impl.ColorSetting;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class ColorButton extends Button {
    private final ColorSetting setting;
    private boolean open = false;
    private boolean hoveringHue = false, hoveringColor = false, hoveringAlpha = false, hoveringCopy = false, hoveringPaste = false, hoveringSync = false, hoveringRainbow = false;
    private boolean draggingHue = false, draggingColor = false, draggingAlpha = false;
    private int colorWidth = 84, totalHeight = 0;
    private float[] hsb;

    public ColorButton(ColorSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
        hsb = Color.RGBtoHSB(setting.getColor().getRed(), setting.getColor().getGreen(), setting.getColor().getBlue(), null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Color outlineColor = Color.BLACK, realColor = Color.getHSBColor(hsb[0], 1, 1);

        // Setting background
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getWidth() - getPadding() - 1, getY() + getParent().getHeight() - 1, new Color(0, 0, 0, 40));
        EUClient.FONT_MANAGER.drawTextWithShadow(context, setting.getTag(), getX() + getTextPadding() + 3, getY() + 2, Color.WHITE);

        Renderer2D.renderQuad(context.getMatrices(), getX() + getWidth() - getPadding() - 9, getY() + 2, getX() + getWidth() - getPadding() - 1, getY() + getParent().getHeight() - 3, ColorUtils.getColor(setting.getColor(), 255));
        Renderer2D.renderOutline(context.getMatrices(), getX() + getWidth() - getPadding() - 9, getY() + 2, getX() + getWidth() - getPadding() - 1, getY() + getParent().getHeight() - 3, outlineColor);

        if(open) {
            int offset = getParent().getHeight();

            int dragX = MathHelper.clamp(mouseX - getX() - getPadding() - 1, 0, colorWidth);
            int dragY = MathHelper.clamp(mouseY - getY() - offset, 0, colorWidth);
            float dragHue = colorWidth * hsb[0];
            float dragSaturation = colorWidth * hsb[1];
            float dragBrightness = colorWidth * (1.0f - hsb[2]);
            float dragAlpha = colorWidth * (setting.getAlpha()/255.0f);

            // hue slider
            for(float i = 0; i < colorWidth; i+= 0.5f) Renderer2D.renderQuad(context.getMatrices(), getX() + getWidth() - getPadding() - 9, getY() + offset + i, getX() + getWidth() - getPadding() - 1, getY() + offset + i + 0.5f, Color.getHSBColor(i / colorWidth, 1.0f, 1.0f));
            Renderer2D.renderOutline(context.getMatrices(), getX() + getWidth() - getPadding() - 9, getY() + offset, getX() + getWidth() - getPadding() - 1, getY() + offset + colorWidth, outlineColor);
            hoveringHue = isHoveringComponent(mouseX, mouseY, getX() + getWidth() - getPadding() - 9, getY() + offset, getX() + getWidth() - getPadding() - 1, getY() + offset + colorWidth);

            Renderer2D.renderQuad(context.getMatrices(), getX() + getWidth() - getPadding() - 10, getY() + offset + dragHue - 1.5f, getX() + getWidth() - getPadding(), getY() + offset + dragHue + 1.5f, outlineColor);
            Renderer2D.renderQuad(context.getMatrices(), getX() + getWidth() - getPadding() - 9, getY() + offset + dragHue - 0.5f, getX() + getWidth() - getPadding() - 1, getY() + offset + dragHue + 0.5f, Color.WHITE);

            if(draggingHue) {
                hsb[0] = (float) dragY / colorWidth;
                setColor(hsb);
            }

            // color slider
            Renderer2D.renderSidewaysGradient(context.getMatrices(), getX() + getPadding() + 1, getY() + offset, getX() + getPadding() + 1 + colorWidth, getY() + offset + colorWidth, Color.WHITE, realColor);
            Renderer2D.renderGradient(context.getMatrices(), getX() + getPadding() + 1, getY() + offset, getX() + getPadding() + 1 + colorWidth, getY() + offset + colorWidth, new Color(0, 0, 0, 0), Color.BLACK);
            Renderer2D.renderOutline(context.getMatrices(), getX() + getPadding() + 1, getY() + offset, getX() + getPadding() + 1 + colorWidth, getY() + offset + colorWidth, outlineColor);
            hoveringColor = isHoveringComponent(mouseX, mouseY, getX() + getPadding() + 1, getY() + offset, getX() + getPadding() + 1 + colorWidth, getY() + offset + colorWidth);

            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1 + dragSaturation - 1.5f, getY() + offset + dragBrightness - 1.5f, getX() + getPadding() + 1 + dragSaturation + 1.5f, getY() + offset + dragBrightness + 1.5f, outlineColor);
            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1 + dragSaturation - 0.5f, getY() + offset + dragBrightness - 0.5f, getX() + getPadding() + 1 + dragSaturation + 0.5f, getY() + offset + dragBrightness + 0.5f, Color.WHITE);

            if(draggingColor) {
                hsb[1] = (float) dragX / colorWidth;
                hsb[2] = 1.0f - (float) dragY / colorWidth;
                setColor(hsb);
            }

            offset += colorWidth + 2;

            // alpha slider
            Renderer2D.renderSidewaysGradient(context.getMatrices(), getX() + getPadding() + 1, getY() + offset, getX() + getPadding() + 1 + colorWidth, getY() + offset + 8, Color.BLACK, ColorUtils.getColor(setting.getColor(), 255));
            Renderer2D.renderOutline(context.getMatrices(), getX() + getPadding() + 1, getY() + offset, getX() + getPadding() + 1 + colorWidth, getY() + offset + 8, outlineColor);
            hoveringAlpha = isHoveringComponent(mouseX, mouseY, getX() + getPadding() + 1, getY() + offset, getX() + getPadding() + 1 + colorWidth, getY() + offset + 8);

            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1 + dragAlpha - 1.5f, getY() + offset - 1, getX() + getPadding() + 1 + dragAlpha + 1.5f, getY() + offset + 9, outlineColor);
            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1 + dragAlpha - 0.5f, getY() + offset, getX() + getPadding() + 1 + dragAlpha + 0.5f, getY() + offset + 8, Color.WHITE);

            if(draggingAlpha) {
                setColor(hsb,(int) (255 * (float) dragX / colorWidth));
            }

            offset += 10;

            // copy button
            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY() + offset, getX() + (getWidth()/2f) - 0.5f, getY() + offset + getParent().getHeight(), ClickGuiScreen.getButtonColor(getY(), 100));
            EUClient.FONT_MANAGER.drawTextWithShadow(context, "Copy", getX() + getPadding() + (getWidth()/4) - 1 - EUClient.FONT_MANAGER.getWidth("Copy")/2, getY() + offset + 2, Color.WHITE);
            hoveringCopy = isHoveringComponent(mouseX, mouseY, getX() + getPadding() + 1, getY() + offset, getX() + (getWidth()/2f) - 0.5f, getY() + offset + getParent().getHeight());

            // paste button
            Renderer2D.renderQuad(context.getMatrices(), getX() + (getWidth()/2f) + 0.5f, getY() + offset, getX() + getWidth() - getPadding() - 1, getY() + offset + getParent().getHeight(), ClickGuiScreen.getButtonColor(getY(), 100));
            EUClient.FONT_MANAGER.drawTextWithShadow(context, "Paste", getX() + (getWidth()/2) + (getWidth()/4) - 1 - EUClient.FONT_MANAGER.getWidth("Paste")/2, getY() + offset + 2, Color.WHITE);
            hoveringPaste = isHoveringComponent(mouseX, mouseY, getX() + (getWidth()/2f) + 0.5f, getY() + offset, getX() + getWidth() - getPadding() - 1, getY() + offset + getParent().getHeight());

            offset += this.getParent().getHeight() + 1;

            // sync button
            if(setting.isSync()) Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY() + offset, getX() + getWidth() - getPadding() - 1, getY() + offset + getParent().getHeight(), ClickGuiScreen.getButtonColor(getY(), 100));
            EUClient.FONT_MANAGER.drawTextWithShadow(context, "Sync", getX() + (getWidth()/2) - EUClient.FONT_MANAGER.getWidth("Sync")/2, getY() + offset + 2, Color.WHITE);
            hoveringSync = isHoveringComponent(mouseX, mouseY, getX() + getPadding() + 1, getY() + offset, getX() + getWidth() - getPadding() - 1, getY() + offset + getParent().getHeight());

            offset += this.getParent().getHeight() + 1;

            if(setting.isRainbow()) Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY() + offset, getX() + getWidth() - getPadding() - 1, getY() + offset + getParent().getHeight(), ClickGuiScreen.getButtonColor(getY(), 100));
            EUClient.FONT_MANAGER.drawTextWithShadow(context, "Rainbow", getX() + (getWidth()/2) - EUClient.FONT_MANAGER.getWidth("Rainbow")/2, getY() + offset + 2, Color.WHITE);
            hoveringRainbow = isHoveringComponent(mouseX, mouseY, getX() + getPadding() + 1, getY() + offset, getX() + getWidth() - getPadding() - 1, getY() + offset + getParent().getHeight());

            offset += this.getParent().getHeight() + 1;
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY) && button == 1) {
            open = !open;
            playClickSound();
        }

        if(button == 0) {
            if(hoveringHue) draggingHue = true;
            if(hoveringColor) draggingColor = true;
            if(hoveringAlpha) draggingAlpha = true;

            if(hoveringCopy) {
                EUClient.CLICK_GUI.setColorClipboard(setting.getValue().getColor());
                playClickSound();
            }
            if(hoveringPaste && EUClient.CLICK_GUI.getColorClipboard() != null) {
                setting.setColor(EUClient.CLICK_GUI.getColorClipboard());
                hsb = Color.RGBtoHSB(setting.getColor().getRed(), setting.getColor().getGreen(), setting.getColor().getBlue(), null);
                playClickSound();
            }
            if(setting != EUClient.MODULE_MANAGER.getModule(ColorModule.class).color && hoveringSync) {
                setting.setSync(!setting.isSync());
                playClickSound();
            }
            if(hoveringRainbow) {
                setting.setRainbow(!setting.isRainbow());
                playClickSound();
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingHue = false;
        draggingColor = false;
        draggingAlpha = false;
    }

    @Override
    public int getHeight() {
        return open ? 151 : getParent().getHeight();
    }

    @Override
    public boolean isHovering(double mouseX, double mouseY) {
        return getX() + getPadding() <= mouseX && getY() <= mouseY && getX() + getWidth() - getPadding() > mouseX && getY() + getParent().getHeight() > mouseY;
    }

    private boolean isHoveringComponent(double mouseX, double mouseY, double left, double top, double right, double bottom) {
        return left <= mouseX && top <= mouseY && right > mouseX && bottom > mouseY;
    }

    private void setColor(float[] hsb) {
        setColor(hsb, setting.getAlpha());
    }

    private void setColor(float[] hsb, int alpha) {
        Color color = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        setting.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
    }
}
