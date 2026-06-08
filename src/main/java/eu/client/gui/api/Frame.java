package eu.client.gui.api;

import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.gui.impl.WhitelistButton;
import eu.client.modules.Module;
import eu.client.gui.impl.ModuleButton;
import eu.client.modules.impl.core.ClickGuiModule;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Frame {
    private final Module.Category category;
    private int x, y, width, height, totalHeight, dragX = 0, dragY = 0, textPadding = 3;
    public boolean open = true, dragging = false;
    private final ArrayList<Button> buttons = new ArrayList<>();

    public Frame(Module.Category category, int x, int y, int width, int height) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        for(Module module : EUClient.MODULE_MANAGER.getModules(category)) buttons.add(new ModuleButton(module, this, height));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(context, mouseX, mouseY, delta, "");
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta, String searchQuery) {
        if(dragging) {
            setX(mouseX - dragX);
            setY(mouseY - dragY);
        }

        boolean searching = searchQuery != null && !searchQuery.isEmpty();

        this.totalHeight = height;

        if(open) {
            totalHeight += 1;
            for(Button button : buttons) {
                // Filter by search query
                if(searching && button instanceof ModuleButton moduleButton) {
                    if(!moduleButton.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                        button.setVisible(false);
                        continue;
                    }
                    button.setVisible(true);
                    moduleButton.setSearchQuery(searchQuery); // Pass search query for highlighting
                } else if(!searching && button instanceof ModuleButton moduleButton) {
                    button.setVisible(true);
                    moduleButton.setSearchQuery(""); // Clear search query
                }

                if(!button.isVisible()) continue;

                button.setX(x);
                button.setY(y + totalHeight);
                totalHeight += button.getHeight();

                if(button instanceof ModuleButton moduleButton && moduleButton.isOpen()) {
                    for(Button b : moduleButton.getButtons()) {
                        b.getSetting().getVisibility().update();
                        b.setVisible(b.getSetting().getVisibility().isVisible());
                        if(!b.isVisible()) continue;

                        b.setX(x);
                        b.setY(y + totalHeight);
                        totalHeight += b.getHeight();
                    }
                }
            }
        }

        // Category header — dark background with accent underline
        Renderer2D.renderQuad(context.getMatrices(), x, y, x + width, y + height, new Color(20, 20, 25, 200));
        Color accentColor = ClickGuiScreen.getButtonColor(y, 200);
        Renderer2D.renderQuad(context.getMatrices(), x, y + height - 1, x + width, y + height, accentColor);
        EUClient.FONT_MANAGER.drawTextWithShadow(context, category.getName(), x + textPadding, y + 2, Color.WHITE);

        if(open) {
            Renderer2D.renderQuad(context.getMatrices(), x, y + height, x + width, y + totalHeight + 1, new Color(15, 15, 20, 180));
            for(Button button : buttons) {
                if(!button.isVisible()) continue;
                button.render(context, mouseX, mouseY, delta);
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                dragging = true;
                dragX = (int) (mouseX - getX());
                dragY = (int) (mouseY - getY());
            } else if(button == 1) {
                open = !open;
            }
        }

        if(open) {
            for(Button b : buttons) {
                if(!b.isVisible()) continue;
                b.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }

        for(Button b : buttons) {
            if(!b.isVisible()) continue;
            b.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (x <= mouseX && x + width > mouseX) {
            boolean whitelistHandling = false;
            for (Button b : buttons) {
                if (b instanceof ModuleButton moduleButton && moduleButton.isOpen()) {
                    List<Button> wbButtons = moduleButton.getButtons().stream().filter(button -> button instanceof WhitelistButton).toList();
                    for (Button whitelistButton : wbButtons) {
                        if (whitelistButton instanceof WhitelistButton wb) {
                            if (wb.isHandlingScroll(mouseX, mouseY)) {
                                wb.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                                whitelistHandling = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!whitelistHandling) {
                if (verticalAmount < 0) {
                    setY(getY() - EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).scrollSpeed.getValue().intValue());
                } else if (verticalAmount > 0) {
                    setY(getY() + EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).scrollSpeed.getValue().intValue());
                }
            }
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (Button b : buttons) {

            b.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (open) {
            for (Button button : buttons) {
                if(!button.isVisible()) continue;
                button.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (open) {
            for (Button button : buttons) {
                if(!button.isVisible()) continue;
                button.charTyped(chr, modifiers);
            }
        }
    }

    public boolean isHovering(double mouseX, double mouseY) {
        return x <= mouseX && y <= mouseY && x + width > mouseX && y + height > mouseY;
    }
}
