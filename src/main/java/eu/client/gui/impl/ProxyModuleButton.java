package eu.client.gui.impl;

import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.modules.impl.core.PingBypassModule;
import eu.client.pingbypass.protocol.PbCustomPayload;
import eu.client.pingbypass.protocol.packets.C2SModuleTogglePacket;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Formatting;

import java.awt.*;

/**
 * A GUI button representing a proxy module in the PingBypass category.
 * Toggles send C2S_MODULE_TOGGLE packets to the proxy server.
 *
 * <p><b>Validates: Requirements 10.1, 10.2</b></p>
 */
public class ProxyModuleButton extends Button {
    private final String moduleName;

    public ProxyModuleButton(String moduleName, Frame parent, int height) {
        super(parent, height, "PingBypass proxy module: " + moduleName);
        this.moduleName = moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }

    private boolean isEnabled() {
        PingBypassModule pbModule = EUClient.MODULE_MANAGER.getModule(PingBypassModule.class);
        if (pbModule == null) return false;
        return pbModule.isProxyModuleEnabled(moduleName);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.isHovering(mouseX, mouseY) && EUClient.CLICK_GUI.getDescriptionFrame().getDescription().isEmpty()) {
            EUClient.CLICK_GUI.getDescriptionFrame().setDescription(this.getDescription());
        }

        // Background — subtle hover highlight
        Color bgColor = isHovering(mouseX, mouseY) ? new Color(255, 255, 255, 15) : new Color(0, 0, 0, 0);
        if (bgColor.getAlpha() > 0) {
            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding(), getY(), getX() + getWidth() - getPadding(), getY() + getHeight() - 1, bgColor);
        }

        // Left-side toggle indicator bar
        boolean enabled = isEnabled();
        if (enabled) {
            Color accentColor = ClickGuiScreen.getButtonColor(getY(), 255);
            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding(), getY() + 1, getX() + getPadding() + 2, getY() + getHeight() - 2, accentColor);
        }

        // Render module name
        String displayName = moduleName;
        int textX = getX() + getTextPadding() + (enabled ? 1 : 0);
        int textY = getY() + 2;
        EUClient.FONT_MANAGER.drawTextWithShadow(context, (enabled ? "" : Formatting.GRAY) + displayName, textX, textY, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(mouseX, mouseY) && button == 0) {
            boolean newState = !isEnabled();
            sendModuleToggle(newState);
            playClickSound();
        }
    }

    /**
     * Sends a C2S_MODULE_TOGGLE packet to the proxy server.
     * Validates: Requirement 10.2
     */
    private void sendModuleToggle(boolean enabled) {
        if (mc.getNetworkHandler() == null) return;

        C2SModuleTogglePacket packet = new C2SModuleTogglePacket(moduleName, enabled);
        mc.getNetworkHandler().getConnection().send(
                new CustomPayloadC2SPacket(PbCustomPayload.fromPacket(packet)));
    }
}
