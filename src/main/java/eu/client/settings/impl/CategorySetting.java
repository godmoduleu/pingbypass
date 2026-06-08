package eu.client.settings.impl;

import lombok.Getter;
import lombok.Setter;
import eu.client.settings.Setting;

@Getter @Setter
public class CategorySetting extends Setting {
    private boolean open = false;

    public CategorySetting(String name, String description) {
        super(name, name, description, new Setting.Visibility());
    }

    public CategorySetting(String name, String tag, String description) {
        super(name, tag, description, new Setting.Visibility());
    }

    public CategorySetting(String name, String description, Setting.Visibility visibility) {
        super(name, name, description, visibility);
    }

    public CategorySetting(String name, String tag, String description, Setting.Visibility visibility) {
        super(name, tag, description, visibility);
    }

    public static class Visibility extends Setting.Visibility {
        private final CategorySetting value;

        public Visibility(CategorySetting value) {
            super(value);
            this.value = value;
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

            setVisible(value.isOpen());
        }
    }
}
