package eu.client.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import eu.client.events.Event;
import net.minecraft.util.math.BlockPos;

@AllArgsConstructor @Getter
public class DestroyBlockEvent extends Event {
    private final BlockPos position;
}
