package ro.aquanano.pulselab.core;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** The traditional 24 unequal hours, anchored to three consecutive solar events. */
public final class PlanetaryHours {
    public enum Planet {
        SATURN("Saturn", "♄"), JUPITER("Jupiter", "♃"), MARS("Marte", "♂"),
        SUN("Soare", "☉"), VENUS("Venus", "♀"), MERCURY("Mercur", "☿"),
        MOON("Lună", "☽");
        public final String name;
        public final String symbol;
        Planet(String name, String symbol) { this.name = name; this.symbol = symbol; }
    }

    public static final class Hour {
        public final int number;
        public final Planet planet;
        public final Instant start;
        public final Instant end;
        Hour(int number, Planet planet, Instant start, Instant end) {
            this.number = number; this.planet = planet; this.start = start; this.end = end;
        }
    }

    public static final class Day {
        public final LocalDate date;
        public final Instant sunrise;
        public final Instant sunset;
        public final Instant nextSunrise;
        public final Hour[] hours;
        Day(LocalDate date, Instant sunrise, Instant sunset, Instant nextSunrise, Hour[] hours) {
            this.date = date;
            this.sunrise = sunrise;
            this.sunset = sunset;
            this.nextSunrise = nextSunrise;
            this.hours = hours;
        }
        public Hour at(Instant now) {
            for (Hour hour : hours)
                if (!now.isBefore(hour.start) && now.isBefore(hour.end)) return hour;
            return null;
        }
    }

    private static final Planet[] ORDER = Planet.values();
    // Java's DayOfWeek: Monday=1 ... Sunday=7.
    private static final int[] FIRST = {0, 6, 2, 5, 1, 4, 0, 3};
    private PlanetaryHours() { }

    public static Day forMoment(Instant now, ZoneId zone, double lat, double lon) {
        LocalDate today = now.atZone(zone).toLocalDate();
        SolarCalculator.Events events = SolarCalculator.calculate(today, zone, lat, lon);
        LocalDate startDate = events.sunrise != null && now.isBefore(events.sunrise)
            ? today.minusDays(1) : today;
        return forDate(startDate, zone, lat, lon);
    }

    public static Day forDate(LocalDate date, ZoneId zone, double lat, double lon) {
        SolarCalculator.Events today = SolarCalculator.calculate(date, zone, lat, lon);
        SolarCalculator.Events tomorrow = SolarCalculator.calculate(date.plusDays(1), zone, lat, lon);
        if (today.sunrise == null || today.sunset == null || tomorrow.sunrise == null
                || !today.sunrise.isBefore(today.sunset)
                || !today.sunset.isBefore(tomorrow.sunrise)) return null;
        return fromEvents(date, today.sunrise, today.sunset, tomorrow.sunrise);
    }

    /** Separately testable event division; all arithmetic uses absolute instants for DST days. */
    public static Day fromEvents(LocalDate date, Instant sunrise, Instant sunset, Instant nextSunrise) {
        if (date == null || sunrise == null || sunset == null || nextSunrise == null
                || !sunrise.isBefore(sunset) || !sunset.isBefore(nextSunrise))
            throw new IllegalArgumentException("Solar events out of order");
        Hour[] hours = new Hour[24];
        int first = FIRST[date.getDayOfWeek().getValue()];
        long dayMillis = Duration.between(sunrise, sunset).toMillis();
        long nightMillis = Duration.between(sunset, nextSunrise).toMillis();
        for (int i = 0; i < 24; i++) {
            Instant anchor = i < 12 ? sunrise : sunset;
            long span = i < 12 ? dayMillis : nightMillis;
            int j = i % 12;
            Instant start = anchor.plusMillis(span * j / 12);
            Instant end = anchor.plusMillis(span * (j + 1) / 12);
            hours[i] = new Hour(i + 1, ORDER[(first + i) % 7], start, end);
        }
        return new Day(date, sunrise, sunset, nextSunrise, hours);
    }
}
