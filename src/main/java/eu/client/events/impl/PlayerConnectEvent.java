package eu.client.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import eu.client.events.Event;

import java.util.UUID;

@AllArgsConstructor @Getter
public class PlayerConnectEvent extends Event {
    private final UUID id;
}
