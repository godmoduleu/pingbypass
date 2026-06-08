package eu.client.managers;

import eu.client.Pingbypass;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.TickEvent;
import eu.client.utils.IMinecraft;

import java.util.ArrayList;

public class TaskManager implements IMinecraft {
    private final ArrayList<Runnable> tasks = new ArrayList<>();

    public TaskManager() {
        Pingbypass.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!tasks.isEmpty()) {
            tasks.getFirst().run();
            tasks.removeFirst();
        }
    }

    public void submit(Runnable runnable) {
        tasks.add(runnable);
    }
}