package eu.client.gui.api;

import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.modules.impl.core.ClickGuiModule;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer2D;
import eu.client.utils.text.FormattingUtils;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.List;

@Getter @Setter
public class DescriptionFrame {
    private String description = "";
    private int x, y, width, height, dragX = 0, dragY = 0, textPadding = 3;
    private boolean dragging = false;

    public DescriptionFrame(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(dragging) {
            setX(mouseX - dragX);
            setY(mouseY - dragY);
        }

        Renderer2D.renderQuad(context.getMatrices(), x, y, x + width, y + height, ClickGuiScreen.getButtonColor(y, 100));
        EUClient.FONT_MANAGER.drawTextWithShadow(context, "Description", x + textPadding, y + 2, Color.WHITE);
        if(!description.isEmpty()) {
            List<String> wrappedText =FormattingUtils.wrapText(description, width - textPadding*2);
            Color color = EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).color.getColor();

            Renderer2D.renderQuad(context.getMatrices(), x, y + height, x + width, y + height + (wrappedText.size()*EUClient.FONT_MANAGER.getHeight()) + 4, EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).isRainbow() ? new Color(0, 0, 0, 100) : new Color((int) (color.getRed()*0.3), (int) (color.getGreen()*0.3), (int) (color.getBlue()*0.3), 100));
            int i = 0;
            for(String s : wrappedText) {
                EUClient.FONT_MANAGER.drawTextWithShadow(context, s, x + textPadding, y + height + 2 + (EUClient.FONT_MANAGER.getHeight()*i), Color.WHITE);
                i++;
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                dragging = true;
                dragX = (int) (mouseX - getX());
                dragY = (int) (mouseY - getY());
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    public boolean isHovering(double mouseX, double mouseY) {
        return x <= mouseX && y <= mouseY && x + width > mouseX && y + height > mouseY;
    }
}
