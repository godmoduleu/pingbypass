package eu.client.gui.api;

import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.modules.impl.core.ClickGuiModule;
import eu.client.settings.Setting;
import eu.client.utils.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

@Getter @Setter
public class Button implements IMinecraft {
    private final Setting setting;
    private final Frame parent;
    private int x, y, height, padding = 2, textPadding = 5;
    private final String description;
    private boolean visible = true;

    public Button(Frame parent, int height, String description) {
        this.setting = null;
        this.parent = parent;
        this.height = height;
        this.description = description;
    }

    public Button(Setting setting, Frame parent, int height, String description) {
        this.setting = setting;
        this.parent = parent;
        this.height = height;
        this.description = description;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {}
    public void mouseClicked(double mouseX, double mouseY, int button) {}
    public void mouseReleased(double mouseX, double mouseY, int button) {}
    public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {}
    public void charTyped(char chr, int modifiers) {}

    public int getWidth() {
        return parent.getWidth();
    }

    public boolean isHovering(double mouseX, double mouseY) {
        return x + padding <= mouseX && y <= mouseY && x + getWidth() - padding > mouseX && y + getHeight() > mouseY;
    }

    public void playClickSound() {
        if(EUClient.MODULE_MANAGER.getModule(ClickGuiModule.class).sounds.getValue()) mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
