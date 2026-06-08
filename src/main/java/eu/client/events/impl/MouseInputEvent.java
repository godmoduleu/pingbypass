package eu.client.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import eu.client.events.Event;

@Getter
@AllArgsConstructor
public class MouseInputEvent extends Event {
    private final int button;
}