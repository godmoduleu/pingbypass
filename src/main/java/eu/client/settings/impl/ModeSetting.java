package eu.client.settings.impl;

import lombok.Getter;
import lombok.Setter;
import eu.client.EUClient;
import eu.client.events.impl.SettingChangeEvent;
import eu.client.settings.Setting;

import java.util.Arrays;
import java.util.List;

@Getter
public class ModeSetting extends Setting {
    private String value;
    private final String defaultValue;
    private final List<String> modes;

    public ModeSetting(String name, String description, String value, String[] modes) {
        super(name, name, description, new Setting.Visibility());
        this.value = value;
        this.defaultValue = value;
        this.modes = Arrays.asList(modes);
    }

    public ModeSetting(String name, String tag, String description, String value, String[] modes) {
        super(name, tag, description, new Setting.Visibility());
        this.value = value;
        this.defaultValue = value;
        this.modes = Arrays.asList(modes);
    }

    public ModeSetting(String name, String description, Setting.Visibility visibility, String value, String[] modes) {
        super(name, name, description, visibility);
        this.value = value;
        this.defaultValue = value;
        this.modes = Arrays.asList(modes);
    }

    public ModeSetting(String name, String tag, String description, Setting.Visibility visibility, String value, String[] modes) {
        super(name, tag, description, visibility);
        this.value = value;
        this.defaultValue = value;
        this.modes = Arrays.asList(modes);
    }

    public void setValue(String value) {
        if (!modes.contains(value)) return;
        this.value = value;
        EUClient.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }


    public void resetValue() {
        value = defaultValue;
    }

    public static class Visibility extends Setting.Visibility {
        private final ModeSetting value;
        private final List<String> targetValues;

        public Visibility(ModeSetting value, String... targetValues) {
            super(value);
            this.value = value;
            this.targetValues = Arrays.asList(targetValues);
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

            boolean visible = false;
            for (String value : targetValues) {
                if (this.value.getValue().equals(value)) {
                    visible = true;
                    break;
                }
            }

            setVisible(visible);
        }
    }
}
