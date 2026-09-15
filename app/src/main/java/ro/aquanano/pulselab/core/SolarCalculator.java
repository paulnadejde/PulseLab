package ro.aquanano.pulselab.core;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Offline sunrise/sunset calculation using the conventional -0.833 degree horizon. */
public final class SolarCalculator {
    private static final double SUNRISE_ALTITUDE_DEGREES = -0.833;

    private SolarCalculator() { }

    public static final class Events {
        public final LocalDate date;
        public final ZoneId zone;
        public final double latitude;
        public final double longitude;
        public final Instant sunrise;
        public final Instant sunset;
        public final boolean polarDay;

        Events(LocalDate date, ZoneId zone, double latitude, double longitude,
               Instant sunrise, Instant sunset, boolean polarDay) {
            this.date = date;
            this.zone = zone;
            this.latitude = latitude;
            this.longitude = longitude;
            this.sunrise = sunrise;
            this.sunset = sunset;
            this.polarDay = polarDay;
        }

        public boolean hasRiseAndSet() { return sunrise != null && sunset != null; }
    }

    public static Events calculate(LocalDate date, ZoneId zone,
                                   double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        long span = Duration.between(start, end).getSeconds();
        long step = 300;
        Instant rise = null;
        Instant set = null;
        Instant previousTime = start;
        double previous = elevationDegrees(start, zone, latitude, longitude)
            - SUNRISE_ALTITUDE_DEGREES;

        for (long seconds = step; seconds <= span; seconds += step) {
            Instant currentTime = start.plusSeconds(Math.min(seconds, span));
            double current = elevationDegrees(currentTime, zone, latitude, longitude)
                - SUNRISE_ALTITUDE_DEGREES;
            if (previous <= 0 && current > 0 && rise == null)
                rise = crossing(previousTime, currentTime, zone, latitude, longitude);
            if (previous >= 0 && current < 0 && set == null)
                set = crossing(previousTime, currentTime, zone, latitude, longitude);
            previousTime = currentTime;
            previous = current;
        }

        double noonElevation = elevationDegrees(date.atTime(12, 0).atZone(zone).toInstant(),
            zone, latitude, longitude);
        return new Events(date, zone, latitude, longitude, rise, set,
            rise == null && set == null && noonElevation > SUNRISE_ALTITUDE_DEGREES);
    }

    private static Instant crossing(Instant low, Instant high, ZoneId zone,
                                    double latitude, double longitude) {
        double lowValue = elevationDegrees(low, zone, latitude, longitude)
            - SUNRISE_ALTITUDE_DEGREES;
        while (Duration.between(low, high).getSeconds() > 1) {
            Instant middle = low.plusSeconds(Duration.between(low, high).getSeconds() / 2);
            double middleValue = elevationDegrees(middle, zone, latitude, longitude)
                - SUNRISE_ALTITUDE_DEGREES;
            if ((lowValue <= 0 && middleValue <= 0) || (lowValue >= 0 && middleValue >= 0)) {
                low = middle;
                lowValue = middleValue;
            } else {
                high = middle;
            }
        }
        return high;
    }

    public static double elevationDegrees(Instant instant, ZoneId zone,
                                          double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        double jd = instant.getEpochSecond() / 86400.0 + 2440587.5;
        double century = (jd - 2451545.0) / 36525.0;
        double geomLong = normalize(280.46646 + century * (36000.76983 + century * 0.0003032));
        double anomaly = 357.52911 + century * (35999.05029 - 0.0001537 * century);
        double eccentricity = 0.016708634 - century * (0.000042037 + 0.0000001267 * century);
        double meanObliquity = 23.0 + (26.0 + (21.448 - century
            * (46.815 + century * (0.00059 - century * 0.001813))) / 60.0) / 60.0;
        double obliquity = meanObliquity + 0.00256 * Math.cos(rad(125.04 - 1934.136 * century));
        double y = Math.tan(rad(obliquity / 2.0));
        y *= y;
        double equationMinutes = 4.0 * deg(
            y * Math.sin(2.0 * rad(geomLong))
                - 2.0 * eccentricity * Math.sin(rad(anomaly))
                + 4.0 * eccentricity * y * Math.sin(rad(anomaly))
                    * Math.cos(2.0 * rad(geomLong))
                - 0.5 * y * y * Math.sin(4.0 * rad(geomLong))
                - 1.25 * eccentricity * eccentricity * Math.sin(2.0 * rad(anomaly)));

        double center = Math.sin(rad(anomaly))
            * (1.914602 - century * (0.004817 + 0.000014 * century))
            + Math.sin(rad(2.0 * anomaly)) * (0.019993 - 0.000101 * century)
            + Math.sin(rad(3.0 * anomaly)) * 0.000289;
        double trueLong = geomLong + center;
        double apparentLong = trueLong - 0.00569
            - 0.00478 * Math.sin(rad(125.04 - 1934.136 * century));
        double declination = deg(Math.asin(
            Math.sin(rad(obliquity)) * Math.sin(rad(apparentLong))));

        ZonedDateTime local = instant.atZone(zone);
        double localMinutes = local.getHour() * 60.0 + local.getMinute()
            + local.getSecond() / 60.0 + local.getNano() / 60_000_000_000.0;
        double offsetMinutes = local.getOffset().getTotalSeconds() / 60.0;
        double trueSolarMinutes = normalizeMinutes(
            localMinutes + equationMinutes + 4.0 * longitude - offsetMinutes);
        double hourAngle = trueSolarMinutes / 4.0 - 180.0;
        double cosZenith = Math.sin(rad(latitude)) * Math.sin(rad(declination))
            + Math.cos(rad(latitude)) * Math.cos(rad(declination)) * Math.cos(rad(hourAngle));
        cosZenith = Math.max(-1.0, Math.min(1.0, cosZenith));
        return 90.0 - deg(Math.acos(cosZenith));
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Invalid coordinates");
    }

    private static double rad(double degrees) { return Math.toRadians(degrees); }
    private static double deg(double radians) { return Math.toDegrees(radians); }
    private static double normalize(double value) {
        value %= 360.0;
        return value < 0 ? value + 360.0 : value;
    }
    private static double normalizeMinutes(double value) {
        value %= 1440.0;
        return value < 0 ? value + 1440.0 : value;
    }
}
