package eu.client.settings.impl;

import lombok.Setter;
import eu.client.Pingbypass;
import eu.client.events.impl.SettingChangeEvent;
import eu.client.settings.Setting;

@Setter
public class BooleanSetting extends Setting {
    private boolean value;
    private final boolean defaultValue;

    public BooleanSetting(String name, String description, boolean value) {
        super(name, name, description, new Setting.Visibility());
        this.value = value;
        this.defaultValue = value;
    }

    public BooleanSetting(String name, String tag, String description, boolean value) {
        super(name, tag, description, new Setting.Visibility());
        this.value = value;
        this.defaultValue = value;
    }

    public BooleanSetting(String name, String description, Setting.Visibility visibility, boolean value) {
        super(name, name, description, visibility);
        this.value = value;
        this.defaultValue = value;
    }

    public BooleanSetting(String name, String tag, String description, Setting.Visibility visibility, boolean value) {
        super(name, tag, description, visibility);
        this.value = value;
        this.defaultValue = value;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
        Pingbypass.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }


    public boolean getDefaultValue() {
        return defaultValue;
    }

    public void resetValue() {
        value = defaultValue;
    }

    public static class Visibility extends Setting.Visibility {
        private final BooleanSetting value;
        private final boolean targetValue;

        public Visibility(BooleanSetting value, boolean targetValue) {
            super(value);
            this.value = value;
            this.targetValue = targetValue;
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

            setVisible(value.getValue() == targetValue);
        }
    }
}
