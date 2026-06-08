package eu.client.events.impl;

import lombok.*;
import eu.client.events.Event;

@EqualsAndHashCode(callSuper = true) @Data
public class KeyInputEvent extends Event {
    private final int key, modifiers;
}
