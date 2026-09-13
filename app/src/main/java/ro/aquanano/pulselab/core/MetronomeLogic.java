package ro.aquanano.pulselab.core;

import java.util.Arrays;

/** Thread-safe state machine for the four sequential timers. */
public final class MetronomeLogic {
    private final int[] durations = {1, 1, 1, 1};
    private final int[] baseDurations = {1, 1, 1, 1};
    private final boolean[] enabled = {true, false, false, false};
    private int activeIndex;
    private int elapsedInSequence;

    public synchronized int[] durations() { return durations.clone(); }
    public synchronized boolean[] enabled() { return enabled.clone(); }
    public synchronized int activeIndex() { return activeIndex; }
    public synchronized int elapsedInSequence() { return elapsedInSequence; }

    public synchronized void setDuration(int index, int seconds) {
        durations[index] = Math.max(1, seconds);
    }

    public synchronized void setEnabled(int index, boolean value) {
        enabled[index] = value;
        if (!anyEnabled()) enabled[0] = true;
    }

    public synchronized void captureMultiplicativeBase() {
        System.arraycopy(durations, 0, baseDurations, 0, durations.length);
    }

    public synchronized void adjustAdditive(int incrementSeconds) {
        for (int i = 0; i < durations.length; i++) {
            if (enabled[i]) durations[i] = Math.max(1, durations[i] + incrementSeconds);
        }
    }

    public synchronized void adjustMultiplicative(double signedIncrement) {
        for (int i = 0; i < durations.length; i++) {
            if (enabled[i]) {
                durations[i] = Math.max(1,
                    (int) Math.round(durations[i] + baseDurations[i] * signedIncrement));
            }
        }
    }

    public synchronized void resetValues() {
        Arrays.fill(durations, 1);
        Arrays.fill(baseDurations, 1);
        resetPosition();
    }

    public synchronized void resetPosition() {
        activeIndex = firstEnabled();
        elapsedInSequence = 0;
    }

    /** Called once per second. Returns true when a sequence has just ended. */
    public synchronized boolean tick() {
        elapsedInSequence++;
        if (elapsedInSequence >= durations[activeIndex]) {
            elapsedInSequence = 0;
            activeIndex = nextEnabled(activeIndex);
            return true;
        }
        return false;
    }

    private boolean anyEnabled() {
        for (boolean e : enabled) if (e) return true;
        return false;
    }

    private int firstEnabled() {
        for (int i = 0; i < enabled.length; i++) if (enabled[i]) return i;
        return 0;
    }

    private int nextEnabled(int current) {
        for (int offset = 1; offset <= enabled.length; offset++) {
            int i = (current + offset) % enabled.length;
            if (enabled[i]) return i;
        }
        return 0;
    }
}
