package eu.client.utils.system;

public class ZeroTimer {
    private long startTime;

    public ZeroTimer() {
        startTime = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long time) {
        if (startTime == 0L) return true;
        return System.currentTimeMillis() - startTime >= time;
    }

    public void zero() {
        startTime = 0L;
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }
}
