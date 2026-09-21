package ro.aquanano.pulselab.core;

import java.util.Arrays;

/** Thread-safe state machine for the four sequential timers. */
public final class MetronomeLogic {
    private final double[] durations = {1, 1, 1, 1};
    private final double[] baseDurations = {1, 1, 1, 1};
    private final boolean[] enabled = {true, false, false, false};
    private int activeIndex;
    private double elapsedInSequence;
    private double multiplicativeOffset;

    public synchronized double[] durations() { return durations.clone(); }
    public synchronized boolean[] enabled() { return enabled.clone(); }
    public synchronized int activeIndex() { return activeIndex; }
    public synchronized double elapsedInSequence() { return elapsedInSequence; }

    public synchronized void setDuration(int index, double seconds) {
        durations[index] = Math.max(0.1, seconds);
    }

    public synchronized void setEnabled(int index, boolean value) {
        enabled[index] = value;
        if (!anyEnabled()) enabled[0] = true;
    }

    public synchronized void captureMultiplicativeBase() {
        System.arraycopy(durations, 0, baseDurations, 0, durations.length);
        multiplicativeOffset = 0.0;
    }

    public synchronized void adjustAdditive(int incrementSeconds) {
        for (int i = 0; i < durations.length; i++) {
            if (enabled[i]) durations[i] = Math.max(0.1, durations[i] + incrementSeconds);
        }
    }

    public synchronized void adjustMultiplicative(double signedIncrement) {
        multiplicativeOffset += signedIncrement;
        for (int i = 0; i < durations.length; i++) {
            if (enabled[i]) {
                durations[i] = Math.max(0.1,
                    baseDurations[i] * (1.0 + multiplicativeOffset));
            }
        }
    }

    public synchronized void resetValues() {
        Arrays.fill(durations, 1);
        Arrays.fill(baseDurations, 1);
        multiplicativeOffset = 0.0;
        resetPosition();
    }

    public synchronized void resetPosition() {
        activeIndex = firstEnabled();
        elapsedInSequence = 0;
    }

    /** Advances by real elapsed time and preserves any remainder across boundaries. */
    public synchronized boolean advance(double seconds) {
        elapsedInSequence += Math.max(0.0, seconds);
        boolean ended = false;
        while (elapsedInSequence >= durations[activeIndex]) {
            elapsedInSequence -= durations[activeIndex];
            activeIndex = nextEnabled(activeIndex);
            ended = true;
        }
        return ended;
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
