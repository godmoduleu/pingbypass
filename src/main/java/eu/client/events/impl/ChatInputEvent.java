package eu.client.events.impl;

import lombok.*;
import eu.client.events.Event;

@Getter @Setter @AllArgsConstructor
public class ChatInputEvent extends Event {
    private String message;
}
