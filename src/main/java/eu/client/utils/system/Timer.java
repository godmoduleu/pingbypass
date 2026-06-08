package eu.client.utils.system;

public class Timer {
    private long startTime;

    public Timer() {
        startTime = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(Number time) {
        return System.currentTimeMillis() - startTime >= time.longValue();
    }

    public long timeElapsed() {
        return System.currentTimeMillis() - startTime;
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }
}
