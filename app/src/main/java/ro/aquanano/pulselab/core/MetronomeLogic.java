package ro.aquanano.pulselab.core;

import java.util.Arrays;

/** Thread-safe state machine for the four sequential timers. */
public final class MetronomeLogic {
    private final double[] durations = {1, 1, 1, 1};
    private final boolean[] enabled = {true, false, false, false};
    private int activeIndex;
    private double elapsedInSequence;
    private double beatIntervalSeconds = 1.0;
    private boolean multiplicativeMode;
    private boolean modeLocked;

    public synchronized double[] durations() { return durations.clone(); }
    public synchronized boolean[] enabled() { return enabled.clone(); }
    public synchronized int activeIndex() { return activeIndex; }
    public synchronized double elapsedInSequence() { return elapsedInSequence; }
    public synchronized double beatIntervalSeconds() { return beatIntervalSeconds; }
    public synchronized boolean isMultiplicativeMode() { return multiplicativeMode; }
    public synchronized boolean isModeLocked() { return modeLocked; }

    public synchronized void setDuration(int index, double seconds) {
        durations[index] = Math.max(0.1, seconds);
    }

    public synchronized void setEnabled(int index, boolean value) {
        enabled[index] = value;
        if (!anyEnabled()) enabled[0] = true;
    }

    public synchronized void setMultiplicativeMode(boolean enabled) {
        if (modeLocked || multiplicativeMode == enabled) return;
        multiplicativeMode = enabled;
        beatIntervalSeconds = 1.0;
        resetPosition();
    }

    public synchronized void lockMode() { modeLocked = true; }
    public synchronized void unlockMode() { modeLocked = false; }

    public synchronized void setBeatIntervalSeconds(double seconds) {
        if (!modeLocked && multiplicativeMode)
            beatIntervalSeconds = Math.max(0.1, seconds);
    }

    public synchronized void adjustAdditive(int incrementSeconds) {
        if (multiplicativeMode) return;
        for (int i = 0; i < durations.length; i++) {
            if (enabled[i]) durations[i] = Math.max(0.1, durations[i] + incrementSeconds);
        }
    }

    public synchronized void adjustMultiplicative(double signedIncrement) {
        if (multiplicativeMode)
            beatIntervalSeconds = Math.max(0.1, beatIntervalSeconds + signedIncrement);
    }

    public synchronized void resetValues() {
        Arrays.fill(durations, 1);
        beatIntervalSeconds = 1.0;
        modeLocked = false;
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
        double activeDuration = durations[activeIndex] * beatIntervalSeconds;
        while (elapsedInSequence >= activeDuration) {
            elapsedInSequence -= activeDuration;
            activeIndex = nextEnabled(activeIndex);
            activeDuration = durations[activeIndex] * beatIntervalSeconds;
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
