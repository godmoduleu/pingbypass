package eu.client.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import eu.client.events.Event;
import eu.client.modules.Module;

@AllArgsConstructor @Getter
public class ToggleModuleEvent extends Event {
    private final Module module;
    private final boolean state;
}
