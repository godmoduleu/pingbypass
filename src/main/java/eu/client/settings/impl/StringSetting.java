package eu.client.settings.impl;

import lombok.Getter;
import lombok.Setter;
import eu.client.Pingbypass;
import eu.client.events.impl.SettingChangeEvent;
import eu.client.settings.Setting;

@Getter @Setter
public class StringSetting extends Setting {
    private String value;
    private final String defaultValue;

    public StringSetting(String name, String description, String value) {
        super(name, name, description, new Setting.Visibility());
        this.value = value;
        this.defaultValue = value;
    }

    public StringSetting(String name, String tag, String description, String value) {
        super(name, tag, description, new Setting.Visibility());
        this.value = value;
        this.defaultValue = value;
    }

    public StringSetting(String name, String description, Setting.Visibility visibility, String value) {
        super(name, name, description, visibility);
        this.value = value;
        this.defaultValue = value;
    }

    public StringSetting(String name, String tag, String description, Setting.Visibility visibility, String value) {
        super(name, tag, description, visibility);
        this.value = value;
        this.defaultValue = value;
    }

    public void resetValue() {
        value = defaultValue;
    }

    public void setValue(String value) {
        this.value = value;
        Pingbypass.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    public static class Visibility extends Setting.Visibility {
        private final StringSetting value;
        private final String targetValue;

        public Visibility(StringSetting value, String targetValue) {
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

            setVisible(value.getValue().equals(targetValue));
        }
    }
}
