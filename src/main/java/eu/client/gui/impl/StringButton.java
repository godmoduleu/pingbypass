package eu.client.gui.impl;

import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.settings.impl.StringSetting;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class StringButton extends Button {
    private final StringSetting setting;
    private String currentString = "";
    private boolean listening = false;
    private boolean selecting = false;
    private int cursorIndex = 0; // New field to track cursor position

    public StringButton(StringSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Setting background
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getWidth() - getPadding() - 1, getY() + getHeight() - 1, new Color(0, 0, 0, 40));
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getWidth() - getPadding() - 1, getY() + getHeight() - 1, ClickGuiScreen.getButtonColor(getY(), 100));

        String displayText = currentString;
        // Render selection or cursor
        if (listening) {
            if (selecting) {
                // If selecting all, just highlight the text by changing color (optional)
                // For simplicity, we draw normally but can change text color
                // Cursor won't show during full selection
                displayText = currentString;
            } else {
                // Insert cursor symbol at cursorIndex
                int clampedIndex = Math.max(0, Math.min(cursorIndex, currentString.length()));
                String before = currentString.substring(0, clampedIndex);
                String after = currentString.substring(clampedIndex);
                String cursorChar = EUClient.CLICK_GUI.isShowLine() ? "|" : " ";
                displayText = before + cursorChar + after;
            }
        } else {
            displayText = setting.getTag() + " " + Formatting.GRAY + setting.getValue();
        }

        EUClient.FONT_MANAGER.drawTextWithShadow(context, displayText, getX() + getTextPadding() + 1, getY() + 2, selecting ? ClickGuiScreen.getButtonColor(getY(), 255) : Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isHovering(mouseX, mouseY) && !listening) {
                listening = true;
                currentString = setting.getValue();
                cursorIndex = currentString.length();
                selecting = false;
                playClickSound();
            } else if (!isHovering(mouseX, mouseY)) {
                listening = false;
                selecting = false;
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening) return;

        long handle = mc.getWindow().getHandle();
        boolean ctrl = InputUtil.isKeyPressed(handle, MinecraftClient.IS_SYSTEM_MAC ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            selecting = false;
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            setting.setValue(currentString);
            selecting = false;
            listening = false;
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT && !currentString.isEmpty()) {
            if (selecting) {
                selecting = false;
                cursorIndex = 0;
            } else {
                if (cursorIndex > 0) cursorIndex--;
            }
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT && !currentString.isEmpty()) {
            if (selecting) {
                selecting = false;
                cursorIndex = currentString.length();
            } else {
                if (cursorIndex < currentString.length()) cursorIndex++;
            }
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (selecting) {
                currentString = "";
                cursorIndex = 0;
                selecting = false;
            } else {
                if (cursorIndex > 0) {
                    currentString = currentString.substring(0, cursorIndex - 1) + currentString.substring(cursorIndex);
                    cursorIndex--;
                }
            }
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (selecting) {
                currentString = "";
                cursorIndex = 0;
                selecting = false;
            } else {
                if (cursorIndex < currentString.length()) {
                    currentString = currentString.substring(0, cursorIndex) + currentString.substring(cursorIndex + 1);
                }
            }
            return;
        }

        if (ctrl) {
            if (keyCode == GLFW.GLFW_KEY_V) {
                try {
                    String clipboard = mc.keyboard.getClipboard();
                    if (selecting) {
                        currentString = clipboard;
                        cursorIndex = currentString.length();
                        selecting = false;
                    } else {
                        currentString = currentString.substring(0, cursorIndex) + clipboard + currentString.substring(cursorIndex);
                        cursorIndex += clipboard.length();
                    }
                } catch (Exception exception) {
                    EUClient.LOGGER.error("{}: Failed to process clipboard paste", exception.getClass().getName(), exception);
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_C && selecting) {
                try {
                    mc.keyboard.setClipboard(currentString);
                } catch (Exception exception) {
                    EUClient.LOGGER.error("{}: Failed to process clipboard change", exception.getClass().getName(), exception);
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_A) {
                if (!currentString.isEmpty()) {
                    selecting = true;
                    cursorIndex = currentString.length();
                }
            }
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (!listening) return;
        if (Character.isISOControl(chr)) return;

        if (selecting) {
            currentString = String.valueOf(chr);
            cursorIndex = 1;
            selecting = false;
        } else {
            currentString = currentString.substring(0, cursorIndex) + chr + currentString.substring(cursorIndex);
            cursorIndex++;
        }
    }
}