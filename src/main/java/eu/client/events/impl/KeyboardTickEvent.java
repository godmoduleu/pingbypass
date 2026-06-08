package eu.client.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import eu.client.events.Event;

@AllArgsConstructor @Getter @Setter
public class KeyboardTickEvent extends Event {
    private float movementForward;
    private float movementSideways;
}
