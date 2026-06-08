package eu.client.gui;

import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.gui.api.DescriptionFrame;
import eu.client.gui.api.PingBypassFrame;
import eu.client.gui.api.SearchFrame;
import eu.client.modules.Module;
import eu.client.modules.impl.core.ClickGuiModule;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer2D;
import eu.client.utils.system.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;

@Getter @Setter
public class ClickGuiScreen extends Screen {
    private final ArrayList<Frame> frames = new ArrayList<>();
    private final ArrayList<Button> buttons = new ArrayList<>();
    private final DescriptionFrame descriptionFrame;
    private final SearchFrame searchFrame;
    private final PingBypassFrame pingBypassFrame;

    private final Timer lineTimer = new Timer();
    private boolean showLine = false;
    private Color colorClipboard = null;

    public ClickGuiScreen() {
        super(Text.literal(EUClient.MOD_ID + "-click-gui"));

        int x = 6;
        for(Module.Category category : Module.Category.values()) {
            frames.add(new Frame(category, x, 3, 100, 13));
            x += 104;
        }

        this.pingBypassFrame = new PingBypassFrame(x, 3, 100, 13);
        this.descriptionFrame = new DescriptionFrame(x + 104, 3, 200, 13);
        this.searchFrame = new SearchFrame();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (lineTimer.hasTimeElapsed(400L)){
            showLine = !showLine;
            lineTimer.reset();
        }

        descriptionFrame.setDescription("");
        String query = searchFrame.getQuery();
        for(Frame frame : frames) frame.render(context, mouseX, mouseY, delta, query);
        pingBypassFrame.render(context, mouseX, mouseY, delta, query);

        descriptionFrame.render(context, mouseX, mouseY, delta);
        searchFrame.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for(Frame frame : frames) frame.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        pingBypassFrame.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchFrame.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (Frame frame : frames) {
            frame.mouseClicked(mouseX, mouseY, button);
        }
        pingBypassFrame.mouseClicked(mouseX, mouseY, button);

        descriptionFrame.mouseClicked(mouseX, mouseY, button);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Frame frame : frames) {
            frame.mouseReleased(mouseX, mouseY, button);
        }
        pingBypassFrame.mouseReleased(mouseX, mouseY, button);

        descriptionFrame.mouseReleased(mouseX, mouseY, button);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Frame frame : frames) {
            frame.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        pingBypassFrame.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        return this.hoveredElement(mouseX, mouseY).filter(element -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)).isPresent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Check for Ctrl+F to toggle search
        long handle = client.getWindow().getHandle();
        boolean ctrl = InputUtil.isKeyPressed(handle, MinecraftClient.IS_SYSTEM_MAC ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL);
        
        if (ctrl && keyCode == GLFW.GLFW_KEY_F) {
            searchFrame.toggle();
            return true;
        }

        if (searchFrame.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        for (Frame frame : frames) {
            frame.keyPressed(keyCode, scanCode, modifiers);
        }
        pingBypassFrame.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFrame.charTyped(chr, modifiers)) {
            return true;
        }
        for (Frame frame : frames) {
            frame.charTyped(chr, modifiers);
        }
        pingBypassFrame.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if(EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).blur.getValue()) applyBlur();
        Renderer2D.renderQuad(context.getMatrices(), 0, 0, this.width, this.height, new Color(10, 8, 18, 120));
    }

    @Override
    public void close() {
        searchFrame.setQuery("");
        searchFrame.setCursorIndex(0);
        searchFrame.setFocused(false);
        searchFrame.setVisible(false);
        super.close();
        EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).setToggled(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static Color getButtonColor(int index, int alpha) {
        Color color = EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).isRainbow() ? ColorUtils.getOffsetRainbow(index*10L) : EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).color.getColor();
        return ColorUtils.getColor(color, alpha);
    }
}
