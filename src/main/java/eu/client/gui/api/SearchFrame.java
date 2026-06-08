package eu.client.gui.api;

import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.modules.impl.core.ClickGuiModule;
import eu.client.utils.graphics.Renderer2D;
import eu.client.utils.system.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@Getter @Setter
public class SearchFrame {
    private String query = "";
    private boolean visible = false;
    private boolean focused = false;
    private int cursorIndex = 0;
    private final Timer cursorTimer = new Timer();
    private boolean showCursor = true;

    private int width = 160;
    private int height = 14;

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int x = (screenWidth - width) / 2;
        int y = screenHeight - height - 6;

        // Blink cursor
        if (cursorTimer.hasTimeElapsed(400L)) {
            showCursor = !showCursor;
            cursorTimer.reset();
        }

        // Background
        Color bgColor = EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).isRainbow()
                ? new Color(0, 0, 0, 100)
                : ClickGuiScreen.getButtonColor(y, 60);
        Renderer2D.renderQuad(context.getMatrices(), x, y, x + width, y + height, bgColor);

        // Outline when focused
        if (focused) {
            Renderer2D.renderOutline(context.getMatrices(), x, y, x + width, y + height, ClickGuiScreen.getButtonColor(y, 150));
        }

        // Text
        String displayText;
        if (query.isEmpty() && !focused) {
            displayText = "Search...";
            EUClient.FONT_MANAGER.drawTextWithShadow(context, displayText, x + 4, y + 3, new Color(150, 150, 150));
        } else {
            String cursor = (focused && showCursor) ? "|" : "";
            String before = query.substring(0, cursorIndex);
            String after = query.substring(cursorIndex);
            displayText = before + cursor + after;
            EUClient.FONT_MANAGER.drawTextWithShadow(context, displayText, x + 4, y + 3, Color.WHITE);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int x = (screenWidth - width) / 2;
        int y = screenHeight - height - 6;

        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            focused = true;
            return true;
        } else {
            focused = false;
            return false;
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !focused) return false;

        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        boolean ctrl = InputUtil.isKeyPressed(handle, MinecraftClient.IS_SYSTEM_MAC ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!query.isEmpty()) {
                query = "";
                cursorIndex = 0;
                return true;
            }
            visible = false;
            focused = false;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!query.isEmpty() && cursorIndex > 0) {
                query = query.substring(0, cursorIndex - 1) + query.substring(cursorIndex);
                cursorIndex--;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorIndex < query.length()) {
                query = query.substring(0, cursorIndex) + query.substring(cursorIndex + 1);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorIndex > 0) cursorIndex--;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorIndex < query.length()) cursorIndex++;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorIndex = 0;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_END) {
            cursorIndex = query.length();
            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            try {
                String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
                query = query.substring(0, cursorIndex) + clipboard + query.substring(cursorIndex);
                cursorIndex += clipboard.length();
            } catch (Exception ignored) {}
            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            cursorIndex = query.length();
            return true;
        }

        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!visible || !focused) return false;
        if (Character.isISOControl(chr)) return false;

        query = query.substring(0, cursorIndex) + chr + query.substring(cursorIndex);
        cursorIndex++;
        return true;
    }

    public void toggle() {
        visible = !visible;
        if (visible) {
            focused = true;
            query = "";
            cursorIndex = 0;
        } else {
            focused = false;
        }
    }

    public boolean isSearching() {
        return !query.isEmpty();
    }

    public boolean matches(String name) {
        if (query.isEmpty()) return true;
        return name.toLowerCase().contains(query.toLowerCase());
    }
}
