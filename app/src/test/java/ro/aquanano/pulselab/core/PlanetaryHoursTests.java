package ro.aquanano.pulselab.core;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class PlanetaryHoursTests {
    public static void main(String[] args) {
        LocalDate friday = LocalDate.of(2026, 9, 25);
        Instant rise = Instant.parse("2026-09-25T04:00:00Z");
        Instant set = Instant.parse("2026-09-25T16:00:00Z");
        Instant nextRise = Instant.parse("2026-09-26T04:00:00Z");
        PlanetaryHours.Day day = PlanetaryHours.fromEvents(friday, rise, set, nextRise);
        assert day.hours.length == 24;
        assert day.hours[0].planet == PlanetaryHours.Planet.VENUS;
        assert day.hours[11].end.equals(set);
        assert day.hours[12].planet == PlanetaryHours.Planet.MARS;
        assert day.hours[23].end.equals(nextRise);
        assert day.at(set).number == 13;
        assert day.at(nextRise) == null;
        for (int i = 0; i < 23; i++)
            assert day.hours[i].end.equals(day.hours[i+1].start);

        // On a DST transition the absolute night duration, not wall-clock hours, is divided.
        ZoneId zone = ZoneId.of("Europe/Bucharest");
        LocalDate autumn = LocalDate.of(2026, 10, 24);
        Instant dusk = autumn.atTime(18, 0).atZone(zone).toInstant();
        Instant dawn = autumn.plusDays(1).atTime(7, 0).atZone(zone).toInstant();
        assert Duration.between(dusk, dawn).toHours() == 14;
        PlanetaryHours.Day dst = PlanetaryHours.fromEvents(autumn,
            autumn.atTime(7, 0).atZone(zone).toInstant(), dusk, dawn);
        assert Duration.between(dst.hours[12].start, dst.hours[23].end).toHours() == 14;
        assert dst.hours[0].planet == PlanetaryHours.Planet.SATURN;

        // Before sunrise the current planetary day started on the previous date.
        PlanetaryHours.Day beforeRise = PlanetaryHours.forMoment(
            Instant.parse("2026-09-25T02:00:00Z"), zone, 44.4268, 26.1025);
        assert beforeRise != null;
        assert beforeRise.date.equals(friday.minusDays(1));
        assert beforeRise.at(Instant.parse("2026-09-25T02:00:00Z")) != null;
        System.out.println("Planetary hours tests passed");
    }
}
