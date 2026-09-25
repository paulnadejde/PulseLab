package ro.aquanano.pulselab.core;

import java.time.Duration;
import java.time.Instant;

/** Approximate geocentric lunar phases from the Sun–Moon ecliptic elongation. */
public final class LunarPhases {
    public enum Phase {
        NEW("Lună nouă", 0), FIRST("Primul pătrar", 90),
        FULL("Lună plină", 180), LAST("Ultimul pătrar", 270);

        public final String name;
        public final int degrees;
        Phase(String name, int degrees) {
            this.name = name;
            this.degrees = degrees;
        }
    }

    public static final class Event {
        public final Phase phase;
        public final Instant instant;
        Event(Phase phase, Instant instant) {
            this.phase = phase;
            this.instant = instant;
        }
    }

    private static final Phase[] ORDER = Phase.values();
    private LunarPhases() { }

    public static double cycleDegrees(Instant instant) {
        return normalize(AstroCalculator.eclipticLongitude(AstroCalculator.Body.MOON, instant)
            - AstroCalculator.eclipticLongitude(AstroCalculator.Body.SUN, instant));
    }

    public static Phase currentPhase(Instant instant) {
        return ORDER[(int) (cycleDegrees(instant) / 90)];
    }

    public static double degreesIntoPhase(Instant instant) {
        return cycleDegrees(instant) % 90;
    }

    public static Event[] nextFour(Instant now) {
        Event[] result = new Event[4];
        Instant cursor = now;
        for (int i = 0; i < result.length; i++) {
            double start = cycleDegrees(cursor);
            int nextIndex = ((int) (start / 90) + 1) % 4;
            Phase phase = ORDER[nextIndex];
            double target = phase.degrees;
            Instant low = cursor;
            Instant high = low.plus(Duration.ofHours(6));
            // Compare forward angular distance from the initial position: this
            // stays continuous at the 360°/0° boundary.
            double needed = normalize(target - start);
            while (normalize(cycleDegrees(high) - start) < needed) {
                high = high.plus(Duration.ofHours(6));
                if (Duration.between(low, high).toDays() > 36)
                    throw new IllegalStateException("Lunar phase could not be located");
            }
            while (Duration.between(low, high).getSeconds() > 1) {
                Instant mid = low.plusSeconds(Duration.between(low, high).getSeconds() / 2);
                if (normalize(cycleDegrees(mid) - start) < needed) low = mid;
                else high = mid;
            }
            result[i] = new Event(phase, high);
            cursor = high.plusSeconds(2);
        }
        return result;
    }

    private static double normalize(double degrees) {
        return ((degrees % 360) + 360) % 360;
    }
}
