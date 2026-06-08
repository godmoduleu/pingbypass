package eu.client.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import eu.client.events.Event;

@Getter @AllArgsConstructor
public class ChangeYawEvent extends Event {
    private final float yaw;
}
