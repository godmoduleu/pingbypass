package eu.client.utils.system;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Counter {
    private final ConcurrentLinkedQueue<Long> count = new ConcurrentLinkedQueue<>();

    public void increment() {
        count.add(System.currentTimeMillis() + 1000L);
    }

    public int getCount() {
        count.removeIf(c -> c < System.currentTimeMillis());
        return count.size();
    }

    public void reset() {
        count.clear();
    }
}
