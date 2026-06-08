package eu.client.settings.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.modules.impl.core.ColorModule;
import eu.client.settings.Setting;
import eu.client.utils.color.ColorUtils;

@Getter @Setter
public class ColorSetting extends Setting {
    private Color value;
    private final Color defaultValue;

    public ColorSetting(String name, String description, Color value) {
        super(name, name, description, new Setting.Visibility());
        this.value = value;
        this.defaultValue = new Color(value.getColor(), value.isSync(), value.isRainbow());
    }

    public ColorSetting(String name, String tag, String description, Color value) {
        super(name, tag, description, new Setting.Visibility());
        this.value = value;
        this.defaultValue = new Color(value.getColor(), value.isSync(), value.isRainbow());
    }

    public ColorSetting(String name, String description, Setting.Visibility visibility, Color value) {
        super(name, name, description, visibility);
        this.value = value;
        this.defaultValue = new Color(value.getColor(), value.isSync(), value.isRainbow());
    }

    public ColorSetting(String name, String tag, String description, Setting.Visibility visibility, Color value) {
        super(name, tag, description, visibility);
        this.value = value;
        this.defaultValue = new Color(value.getColor(), value.isSync(), value.isRainbow());
    }

    public java.awt.Color getColor() {
        if (isSync()) {
            return ColorUtils.getGlobalColor(getAlpha());
        } else {
            if (isRainbow()) {
                if (this == EUClient.MODULE_MANAGER.getModule(ColorModule.class).color) return ColorUtils.getRainbow(255);
                return ColorUtils.getRainbow(getAlpha());
            } else {
                if (this == EUClient.MODULE_MANAGER.getModule(ColorModule.class).color) return ColorUtils.getColor(value.getColor(), 255);
                return value.getColor();
            }
        }
    }

    public void setColor(java.awt.Color color) {
        value.setColor(color);
    }

    public int getAlpha() {
        return getValue().getColor().getAlpha();
    }

    public boolean isSync() {
        return value.isSync();
    }

    public void setSync(boolean sync) {
        value.setSync(sync);
    }

    public boolean isRainbow() {
        return value.isRainbow();
    }

    public void setRainbow(boolean rainbow) {
        value.setRainbow(rainbow);
    }

    public void resetValue() {
        this.value = new Color(defaultValue.getColor(), defaultValue.isSync(), defaultValue.isRainbow());
    }

    public static class Visibility extends Setting.Visibility {
        private final ColorSetting value;
        private final boolean targetValue;
        private final Target target;

        public Visibility(ColorSetting value, boolean targetValue, Target target) {
            super(value);
            this.value = value;
            this.targetValue = targetValue;
            this.target = target;
        }

        @Override
        public void update() {
            if (value.getVisibility() != null) {
                value.getVisibility().update();
                if (!value.getVisibility().isVisible()) {
                    setVisible(false);
                    return;
                }
            }

            setVisible(target == Target.RAINBOW ? value.isRainbow() == targetValue : value.isSync() == targetValue);
        }

        public enum Target {
            RAINBOW, SYNC
        }
    }

    @Getter @Setter @AllArgsConstructor
    public static class Color {
        private java.awt.Color color;
        private boolean sync;
        private boolean rainbow;
    }
}
