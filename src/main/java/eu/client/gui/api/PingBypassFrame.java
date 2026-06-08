package eu.client.gui.api;

import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.gui.impl.ProxyModuleButton;
import eu.client.modules.impl.core.PingBypassModule;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A GUI frame that displays proxy modules under a "PingBypass" category.
 * Only visible when the PingBypass module is enabled and connected (has proxy module states).
 * Dynamically rebuilds its button list when proxy module states change.
 *
 * <p><b>Validates: Requirements 10.1, 10.4</b></p>
 */
public class PingBypassFrame extends Frame {
    private static final String CATEGORY_NAME = "PingBypass";
    private Set<String> lastKnownModules = Collections.emptySet();

    public PingBypassFrame(int x, int y, int width, int height) {
        // Use CORE as a placeholder category — we override the name rendering
        super(null, x, y, width, height);
    }

    /**
     * Checks if the PingBypass module is active and has proxy module states.
     */
    private boolean isActive() {
        PingBypassModule pbModule = EUClient.MODULE_MANAGER.getModule(PingBypassModule.class);
        return pbModule != null && pbModule.isToggled() && !pbModule.getProxyModuleStates().isEmpty();
    }

    /**
     * Rebuilds the button list if the set of proxy modules has changed.
     */
    private void refreshButtons() {
        PingBypassModule pbModule = EUClient.MODULE_MANAGER.getModule(PingBypassModule.class);
        if (pbModule == null) return;

        Set<String> currentModules = new TreeSet<>(pbModule.getProxyModuleStates().keySet());
        if (currentModules.equals(lastKnownModules)) return;

        lastKnownModules = currentModules;
        getButtons().clear();
        for (String moduleName : currentModules) {
            getButtons().add(new ProxyModuleButton(moduleName, this, getHeight()));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta, String searchQuery) {
        if (!isActive()) return;
        refreshButtons();

        if (isDragging()) {
            setX(mouseX - getDragX());
            setY(mouseY - getDragY());
        }

        boolean searching = searchQuery != null && !searchQuery.isEmpty();
        int totalH = getHeight();

        if (isOpen()) {
            totalH += 1;
            for (Button button : getButtons()) {
                if (searching && button instanceof ProxyModuleButton pmb) {
                    if (!pmb.getModuleName().toLowerCase().contains(searchQuery.toLowerCase())) {
                        button.setVisible(false);
                        continue;
                    }
                    button.setVisible(true);
                } else {
                    button.setVisible(true);
                }

                if (!button.isVisible()) continue;

                button.setX(getX());
                button.setY(getY() + totalH);
                totalH += button.getHeight();
            }
        }

        setTotalHeight(totalH);

        // Category header
        Renderer2D.renderQuad(context.getMatrices(), getX(), getY(), getX() + getWidth(), getY() + getHeight(), new Color(20, 20, 25, 200));
        Color accentColor = ClickGuiScreen.getButtonColor(getY(), 200);
        Renderer2D.renderQuad(context.getMatrices(), getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), accentColor);
        EUClient.FONT_MANAGER.drawTextWithShadow(context, CATEGORY_NAME, getX() + getTextPadding(), getY() + 2, Color.WHITE);

        if (isOpen()) {
            Renderer2D.renderQuad(context.getMatrices(), getX(), getY() + getHeight(), getX() + getWidth(), getY() + totalH + 1, new Color(15, 15, 20, 180));
            for (Button button : getButtons()) {
                if (!button.isVisible()) continue;
                button.render(context, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(context, mouseX, mouseY, delta, "");
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isActive()) return;

        if (isHovering(mouseX, mouseY)) {
            if (button == 0) {
                setDragging(true);
                setDragX((int) (mouseX - getX()));
                setDragY((int) (mouseY - getY()));
            } else if (button == 1) {
                setOpen(!isOpen());
            }
        }

        if (isOpen()) {
            for (Button b : getButtons()) {
                if (!b.isVisible()) continue;
                b.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!isActive()) return;
        if (button == 0) {
            setDragging(false);
        }
        for (Button b : getButtons()) {
            if (!b.isVisible()) continue;
            b.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isActive()) return;
        super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isActive()) return;
        if (isOpen()) {
            for (Button button : getButtons()) {
                if (!button.isVisible()) continue;
                button.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (!isActive()) return;
        if (isOpen()) {
            for (Button button : getButtons()) {
                if (!button.isVisible()) continue;
                button.charTyped(chr, modifiers);
            }
        }
    }
}
