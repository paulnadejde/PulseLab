package ro.aquanano.pulselab.core;

import java.time.Duration;
import java.time.Instant;

/** The nested five-state cycles displayed by SolaRitm. */
public final class SolarCycle {
    public static final long LARGE_STEP_SECONDS = 24 * 60;
    public static final long SMALL_STEP_SECONDS = LARGE_STEP_SECONDS / 5;

    private SolarCycle() { }

    public static final class State {
        public final int large;
        public final int small;

        State(int large, int small) {
            this.large = large;
            this.small = small;
        }
    }

    public static State at(Instant reset, Instant now) {
        if (reset == null || now == null) throw new IllegalArgumentException("Missing time");
        long elapsed = Math.max(0, Duration.between(reset, now).getSeconds());
        int large = 5 - (int) ((elapsed / LARGE_STEP_SECONDS) % 5);
        int small = 5 - (int) ((elapsed / SMALL_STEP_SECONDS) % 5);
        return new State(large, small);
    }
}
