package eu.client.gui.special;

import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.gui.PingBypassScreen;
import eu.client.modules.impl.core.ClickGuiModule;
import eu.client.utils.color.ColorUtils;
import eu.client.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The client's custom main menu. Replaces vanilla's {@link net.minecraft.client.gui.screen.TitleScreen}
 * when the Menu module's "MainMenu" setting is enabled, giving the title screen the same dark accent
 * theme used across the rest of the GUI.
 */
public class MainMenuScreen extends Screen {
    private final List<Entry> entries = new ArrayList<>();

    public MainMenuScreen() {
        super(Text.literal(EUClient.MOD_NAME));
    }

    @Override
    protected void init() {
        entries.clear();

        int buttonWidth = 230;
        int buttonHeight = 22;
        int spacing = 6;

        // Stack the buttons in the lower-centre of the screen, leaving room above for the logo.
        int x = width / 2 - buttonWidth / 2;
        int y = height / 2 - 18;

        entries.add(new Entry("Singleplayer", x, y, buttonWidth, buttonHeight,
                () -> client.setScreen(new SelectWorldScreen(this))));
        y += buttonHeight + spacing;

        entries.add(new Entry("Multiplayer", x, y, buttonWidth, buttonHeight,
                () -> client.setScreen(new MultiplayerScreen(this))));
        y += buttonHeight + spacing;

        entries.add(new Entry("PingBypass", x, y, buttonWidth, buttonHeight,
                () -> client.setScreen(new PingBypassScreen(this))));
        y += buttonHeight + spacing;

        // Two half-width buttons sharing one row.
        int half = (buttonWidth - spacing) / 2;
        entries.add(new Entry("Options", x, y, half, buttonHeight,
                () -> client.setScreen(new OptionsScreen(this, client.options))));
        entries.add(new Entry("Quit", x + half + spacing, y, buttonWidth - half - spacing, buttonHeight,
                () -> client.scheduleStop()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        renderLogo(context);

        for (Entry entry : entries) {
            entry.render(context, mouseX, mouseY, delta);
        }

        renderFooter(context);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Vertical gradient — slightly lighter at the top, fading into near-black at the bottom.
        Renderer2D.renderGradient(context.getMatrices(), 0, 0, width, height,
                new Color(18, 14, 30), new Color(8, 6, 14));
    }

    private void renderLogo(DrawContext context) {
        String title = EUClient.MOD_NAME;
        int titleWidth = EUClient.FONT_MANAGER.getWidth(title);
        int titleX = width / 2 - titleWidth / 2;
        int titleY = height / 2 - 70;

        // A faint accent bar behind the logo to anchor it.
        Color accent = accentColor(255);
        Renderer2D.renderQuad(context.getMatrices(), width / 2f - titleWidth / 2f - 6, titleY + EUClient.FONT_MANAGER.getHeight() + 3,
                width / 2f + titleWidth / 2f + 6, titleY + EUClient.FONT_MANAGER.getHeight() + 4, accent);

        if (isRainbow()) {
            EUClient.FONT_MANAGER.drawRainbowString(context, title, titleX, titleY, 6L);
        } else {
            EUClient.FONT_MANAGER.drawTextWithShadow(context, title, titleX, titleY, accent);
        }

        String subtitle = "v" + EUClient.MOD_VERSION + " • MC " + EUClient.MINECRAFT_VERSION;
        int subWidth = EUClient.FONT_MANAGER.getWidth(subtitle);
        EUClient.FONT_MANAGER.drawTextWithShadow(context, subtitle, width / 2 - subWidth / 2,
                titleY + EUClient.FONT_MANAGER.getHeight() + 8, new Color(150, 150, 165));
    }

    private void renderFooter(DrawContext context) {
        String left = EUClient.MOD_NAME + " " + EUClient.MOD_VERSION;
        EUClient.FONT_MANAGER.drawTextWithShadow(context, left, 4, height - EUClient.FONT_MANAGER.getHeight() - 4,
                new Color(120, 120, 135));

        String right = "git-" + EUClient.GIT_HASH;
        int rightWidth = EUClient.FONT_MANAGER.getWidth(right);
        EUClient.FONT_MANAGER.drawTextWithShadow(context, right, width - rightWidth - 4,
                height - EUClient.FONT_MANAGER.getHeight() - 4, new Color(120, 120, 135));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Entry entry : entries) {
                if (entry.isHovered(mouseX, mouseY)) {
                    playClick();
                    entry.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void playClick() {
        if (EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).sounds.getValue()) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private static boolean isRainbow() {
        return EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).isRainbow();
    }

    private static Color accentColor(int alpha) {
        return ClickGuiScreen.getButtonColor(0, alpha);
    }

    /**
     * A single clickable menu item. Keeps its own hover progress so the highlight can ease in and out
     * instead of snapping on/off.
     */
    private static class Entry {
        private final String label;
        private final int x, y, width, height;
        private final Runnable action;
        private float hover;

        private Entry(String label, int x, int y, int width, int height, Runnable action) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.action = action;
        }

        private boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        private void render(DrawContext context, int mouseX, int mouseY, float delta) {
            float target = isHovered(mouseX, mouseY) ? 1f : 0f;
            hover += (target - hover) * 0.2f;

            Color accent = accentColor(255);

            // Background darkens a touch towards the accent as you hover over it.
            Color base = ColorUtils.getColor(new Color(
                    (int) (24 + (accent.getRed() - 24) * 0.18f * hover),
                    (int) (20 + (accent.getGreen() - 20) * 0.18f * hover),
                    (int) (32 + (accent.getBlue() - 32) * 0.18f * hover)), 220);
            Renderer2D.renderQuad(context.getMatrices(), x, y, x + width, y + height, base);

            // Left accent bar grows in from nothing as the item lights up.
            float bar = 3f * hover;
            if (bar > 0.1f) {
                Renderer2D.renderQuad(context.getMatrices(), x, y, x + bar, y + height, accent);
            }

            // Thin underline that expands from the centre on hover.
            float underline = (width / 2f) * hover;
            Renderer2D.renderQuad(context.getMatrices(), x + width / 2f - underline, y + height - 1,
                    x + width / 2f + underline, y + height, accent);

            Color textColor = lerp(new Color(200, 200, 210), Color.WHITE, hover);
            int labelWidth = EUClient.FONT_MANAGER.getWidth(label);
            int textX = x + width / 2 - labelWidth / 2;
            int textY = y + height / 2 - EUClient.FONT_MANAGER.getHeight() / 2;
            EUClient.FONT_MANAGER.drawTextWithShadow(context, label, textX, textY, textColor);
        }

        private static Color lerp(Color from, Color to, float t) {
            t = Math.max(0f, Math.min(1f, t));
            return new Color(
                    (int) (from.getRed() + (to.getRed() - from.getRed()) * t),
                    (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                    (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t));
        }
    }
}
