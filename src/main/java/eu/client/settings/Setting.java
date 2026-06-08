package eu.client.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @AllArgsConstructor
public class Setting {
    private final String name, tag, description;
    private final Visibility visibility;

    @Getter @Setter @RequiredArgsConstructor
    public static class Visibility {
        private final Setting setting;
        private boolean visible = true;

        public Visibility() {
            this.setting = null;
        }

        public void update() {}
    }
}
