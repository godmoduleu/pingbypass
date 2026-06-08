package eu.client.gui.impl;

import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.modules.Module;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.settings.Setting;
import eu.client.settings.impl.*;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;

@Getter @Setter
public class ModuleButton extends Button {
    private final Module module;
    private boolean open = false;
    private final ArrayList<Button> buttons = new ArrayList<>();
    private String searchQuery = "";

    public ModuleButton(Module module, Frame parent, int height) {
        super(parent, height, module.getDescription());
        this.module = module;

        for(Setting setting : module.getSettings()) {
            if(setting instanceof BooleanSetting s) {
                buttons.add(new BooleanButton(s, parent, height));
            } else if(setting instanceof NumberSetting s) {
                buttons.add(new NumberButton(s, parent, height));
            } else if(setting instanceof CategorySetting s) {
                buttons.add(new CategoryButton(s, parent, height));
            } else if(setting instanceof BindSetting s) {
                buttons.add(new BindButton(s, parent, height));
            } else if(setting instanceof ModeSetting s) {
                buttons.add(new ModeButton(s, parent, height));
            } else if(setting instanceof WhitelistSetting s) {
                buttons.add(new WhitelistButton(s, parent, height));
            } else if(setting instanceof StringSetting s) {
                buttons.add(new StringButton(s, parent, height));
            } else if(setting instanceof ColorSetting s) {
                buttons.add(new ColorButton(s, parent, height));
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(this.isHovering(mouseX, mouseY) && EUClient.CLICK_GUI.getDescriptionFrame().getDescription().isEmpty()) EUClient.CLICK_GUI.getDescriptionFrame().setDescription(this.getDescription());

        // Background — subtle hover highlight
        Color bgColor = isHovering(mouseX, mouseY) ? new Color(255, 255, 255, 15) : new Color(0, 0, 0, 0);
        if (bgColor.getAlpha() > 0) {
            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding(), getY(), getX() + getWidth() - getPadding(), getY() + getHeight() - 1, bgColor);
        }

        // Left-side toggle indicator bar
        if (module.isToggled()) {
            Color accentColor = ClickGuiScreen.getButtonColor(getY(), 255);
            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding(), getY() + 1, getX() + getPadding() + 2, getY() + getHeight() - 2, accentColor);
        }
        
        // Render module name with search highlighting
        String moduleName = module.getName();
        int textX = getX() + getTextPadding() + (module.isToggled() ? 1 : 0);
        int textY = getY() + 2;
        
        if (searchQuery != null && !searchQuery.isEmpty()) {
            // Find the match position (case-insensitive)
            String lowerName = moduleName.toLowerCase();
            String lowerQuery = searchQuery.toLowerCase();
            int matchIndex = lowerName.indexOf(lowerQuery);
            
            if (matchIndex != -1) {
                // Split into: before match, match, after match
                String before = moduleName.substring(0, matchIndex);
                String match = moduleName.substring(matchIndex, matchIndex + searchQuery.length());
                String after = moduleName.substring(matchIndex + searchQuery.length());
                
                // Render before match
                if (!before.isEmpty()) {
                    String prefix = module.isToggled() ? "" : Formatting.GRAY.toString();
                    EUClient.FONT_MANAGER.drawTextWithShadow(context, prefix + before, textX, textY, Color.WHITE);
                    textX += EUClient.FONT_MANAGER.getWidth(before);
                }
                
                // Render match with highlight background
                Color highlightColor = ClickGuiScreen.getButtonColor(getY(), 200);
                int matchWidth = EUClient.FONT_MANAGER.getWidth(match);
                Renderer2D.renderQuad(context.getMatrices(), textX - 1, textY - 1, textX + matchWidth + 1, textY + EUClient.FONT_MANAGER.getHeight(), highlightColor);
                EUClient.FONT_MANAGER.drawTextWithShadow(context, match, textX, textY, Color.WHITE);
                textX += matchWidth;
                
                // Render after match
                if (!after.isEmpty()) {
                    String prefix = module.isToggled() ? "" : Formatting.GRAY.toString();
                    EUClient.FONT_MANAGER.drawTextWithShadow(context, prefix + after, textX, textY, Color.WHITE);
                }
            } else {
                // No match found (shouldn't happen if filtering is correct)
                EUClient.FONT_MANAGER.drawTextWithShadow(context, (module.isToggled() ? "" : Formatting.GRAY) + moduleName, textX, textY, Color.WHITE);
            }
        } else {
            // No search query, render normally
            EUClient.FONT_MANAGER.drawTextWithShadow(context, (module.isToggled() ? "" : Formatting.GRAY) + moduleName, textX, textY, Color.WHITE);
        }

        if(open) {
            for(Button button : buttons) {
                if(!button.isVisible()) continue;
                button.render(context, mouseX, mouseY, delta);
                if(button.isHovering(mouseX, mouseY) && EUClient.CLICK_GUI.getDescriptionFrame().getDescription().isEmpty()) EUClient.CLICK_GUI.getDescriptionFrame().setDescription(button.getDescription());
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                module.setToggled(!module.isToggled());
                playClickSound();
            } else if(button == 1) {
                open = !open;
                playClickSound();
            }
        }

        if(open) {
            for(Button b : buttons) {
                if(!b.isVisible()) continue;
                b.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        for(Button b : buttons) {
            b.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (Button b : buttons) {
            b.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if(open) {
            for(Button b : buttons) {
                if(!b.isVisible()) continue;
                b.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if(open) {
            for(Button b : buttons) {
                if(!b.isVisible()) continue;
                b.charTyped(chr, modifiers);
            }
        }
    }
}
