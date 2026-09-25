package ro.aquanano.pulselab.core;

import java.time.Duration;
import java.time.Instant;

public final class LunarPhasesTests {
    public static void main(String[] args) {
        // US Naval Observatory primary phases (UTC), September–October 2026.
        String[] reference = {"2026-09-26T16:49:00Z", "2026-10-03T13:25:00Z",
            "2026-10-10T15:50:00Z", "2026-10-18T16:12:00Z"};
        LunarPhases.Event[] september = LunarPhases.nextFour(
            Instant.parse("2026-09-25T12:00:00Z"));
        for (int i = 0; i < reference.length; i++)
            assert Math.abs(Duration.between(Instant.parse(reference[i]),
                september[i].instant).toMinutes()) < 10;
        for (Instant now : new Instant[]{Instant.parse("2026-09-25T12:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                Instant.parse("2026-10-10T12:00:00Z"),
                Instant.parse("2026-10-18T12:00:00Z")}) {
            double cycle = LunarPhases.cycleDegrees(now);
            assert cycle >= 0 && cycle < 360;
            assert Math.abs(cycle % 90 - LunarPhases.degreesIntoPhase(now)) < 1e-8;
            LunarPhases.Event[] events = LunarPhases.nextFour(now);
            Instant previous = now;
            for (int i = 0; i < 4; i++) {
                assert events[i].instant.isAfter(previous);
                assert Duration.between(previous, events[i].instant).toDays() < 10;
                assert Math.abs(((LunarPhases.cycleDegrees(events[i].instant)
                    - events[i].phase.degrees + 540) % 360) - 180) < .001;
                if (i > 0) assert events[i].phase.ordinal()
                    == (events[i-1].phase.ordinal() + 1) % 4;
                previous = events[i].instant;
            }
            // A fresh lookup just after an event must start at the following phase.
            LunarPhases.Event[] after = LunarPhases.nextFour(events[0].instant.plusSeconds(5));
            assert after[0].phase == events[1].phase;
            assert after[0].instant.isAfter(events[0].instant);
        }
        System.out.println("Lunar phases tests passed");
    }
}
