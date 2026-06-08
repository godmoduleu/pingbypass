package eu.client.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import eu.client.events.Event;
import eu.client.settings.Setting;

@Getter @AllArgsConstructor
public class SettingChangeEvent extends Event {
    private final Setting setting;
}
