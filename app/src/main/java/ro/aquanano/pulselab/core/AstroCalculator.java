package ro.aquanano.pulselab.core;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Offline, approximate equatorial positions and local civil-day rise/set events.
 * The orbital elements and lunar perturbations follow Paul Schlyter,
 * https://www.stjarnhimlen.se/comp/ppcomp.html .
 * This is for display, not navigation or precision astronomical observation.
 */
public final class AstroCalculator {
    public enum Body { SUN, MOON, MERCURY, VENUS, MARS, JUPITER, SATURN }

    public static final class Events {
        public final Instant rise;
        public final Instant set;
        public final double declinationNow;
        public final boolean aboveHorizon;
        public Events(Instant rise, Instant set, double declinationNow, boolean aboveHorizon) {
            this.rise = rise;
            this.set = set;
            this.declinationNow = declinationNow;
            this.aboveHorizon = aboveHorizon;
        }
    }

    private static final class Position {
        final double ra, dec, distance;
        Position(double ra, double dec, double distance) {
            this.ra = ra; this.dec = dec; this.distance = distance;
        }
    }

    private static final class Vector {
        final double x, y, z, radius;
        Vector(double x, double y, double z, double radius) {
            this.x = x; this.y = y; this.z = z; this.radius = radius;
        }
    }

    private AstroCalculator() { }

    public static Events calculate(Body body, LocalDate date, ZoneId zone,
                                   double latitude, double longitude, Instant now) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Invalid coordinates");
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        Instant rise = null, set = null;
        Instant previousTime = start;
        double previous = altitude(body, start, latitude, longitude);
        // Local days can have 23 or 25 hours; sample the actual instant interval.
        for (Instant next = start.plusSeconds(600); !next.isAfter(end); next = next.plusSeconds(600)) {
            double value = altitude(body, next, latitude, longitude);
            if (previous <= 0 && value > 0 && rise == null)
                rise = crossing(body, previousTime, next, latitude, longitude);
            if (previous >= 0 && value < 0 && set == null)
                set = crossing(body, previousTime, next, latitude, longitude);
            previousTime = next;
            previous = value;
        }
        if (previousTime.isBefore(end)) {
            double value = altitude(body, end, latitude, longitude);
            if (previous <= 0 && value > 0 && rise == null)
                rise = crossing(body, previousTime, end, latitude, longitude);
            if (previous >= 0 && value < 0 && set == null)
                set = crossing(body, previousTime, end, latitude, longitude);
        }
        return new Events(rise, set, position(body, now).dec,
            altitude(body, now, latitude, longitude) > 0);
    }

    private static Instant crossing(Body body, Instant low, Instant high,
                                    double latitude, double longitude) {
        double lowValue = altitude(body, low, latitude, longitude);
        while (Duration.between(low, high).getSeconds() > 1) {
            Instant mid = low.plusSeconds(Duration.between(low, high).getSeconds() / 2);
            double midValue = altitude(body, mid, latitude, longitude);
            if ((lowValue <= 0) == (midValue <= 0)) {
                low = mid;
                lowValue = midValue;
            } else high = mid;
        }
        return high;
    }

    private static double altitude(Body body, Instant time, double lat, double lon) {
        Position p = position(body, time);
        double jd = time.getEpochSecond() / 86400.0 + 2440587.5;
        double t = (jd - 2451545.0) / 36525.0;
        double gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0)
            + 0.000387933 * t * t - t * t * t / 38710000.0;
        double hourAngle = rad(gmst + lon - p.ra);
        double sinAlt = sin(lat) * sin(p.dec) + cos(lat) * cos(p.dec) * Math.cos(hourAngle);
        double altitude = deg(Math.asin(Math.max(-1, Math.min(1, sinAlt))));
        // Geocentric lunar altitude corrected for parallax at the horizon.
        // Refraction ~0.583°, lunar semidiameter ~0.2725° * horizontal parallax.
        double horizon = body == Body.SUN ? -0.833 :
            body == Body.MOON ? deg(Math.asin(1.0 / p.distance)) * (1 - 0.2725) - 0.583 :
            -0.583;
        return altitude - horizon;
    }

    private static Position position(Body body, Instant instant) {
        double jd = instant.getEpochSecond() / 86400.0 + 2440587.5;
        double d = jd - 2451543.5;
        double obliquity = 23.4393 - 3.563e-7 * d;
        double sunW = 282.9404 + 4.70935e-5 * d;
        double sunM = 356.0470 + 0.9856002585 * d;
        Vector sun = orbit(0, 0, sunW, 1, 0.016709 - 1.151e-9 * d, sunM);
        if (body == Body.SUN) return equatorial(sun.x, sun.y, sun.z, obliquity, sun.radius);
        if (body == Body.MOON) {
            double node = 125.1228 - 0.0529538083 * d;
            double w = 318.0634 + 0.1643573223 * d;
            double m = 115.3654 + 13.0649929509 * d;
            Vector moon = orbit(node, 5.1454, w, 60.2666, 0.054900, m);
            double lon = deg(Math.atan2(moon.y, moon.x));
            double lat = deg(Math.atan2(moon.z, Math.hypot(moon.x, moon.y)));
            double elong = m + w + node - sunM - sunW;
            double f = m + w;
            lon += -1.274*sin(m-2*elong) + 0.658*sin(2*elong) - 0.186*sin(sunM)
                -0.059*sin(2*m-2*elong) -0.057*sin(m-2*elong+sunM)
                +0.053*sin(m+2*elong) +0.046*sin(2*elong-sunM)
                +0.041*sin(m-sunM) -0.035*sin(elong) -0.031*sin(m+sunM)
                -0.015*sin(2*f-2*elong) +0.011*sin(m-4*elong);
            lat += -0.173*sin(f-2*elong) -0.055*sin(m-f-2*elong)
                -0.046*sin(m+f-2*elong) +0.033*sin(f+2*elong)
                +0.017*sin(2*m+f);
            double distance = moon.radius - 0.58*cos(m-2*elong) - 0.46*cos(2*elong);
            return equatorial(distance*cos(lat)*cos(lon), distance*cos(lat)*sin(lon),
                distance*sin(lat), obliquity, distance);
        }
        Vector planet;
        switch (body) {
            case MERCURY:
                planet = orbit(48.3313+3.24587e-5*d, 7.0047+5e-8*d,
                    29.1241+1.01444e-5*d, .387098, .205635+5.59e-10*d,
                    168.6562+4.0923344368*d); break;
            case VENUS:
                planet = orbit(76.6799+2.46590e-5*d, 3.3946+2.75e-8*d,
                    54.8910+1.38374e-5*d, .723330, .006773-1.302e-9*d,
                    48.0052+1.6021302244*d); break;
            case MARS:
                planet = orbit(49.5574+2.11081e-5*d, 1.8497-1.78e-8*d,
                    286.5016+2.92961e-5*d, 1.523688, .093405+2.516e-9*d,
                    18.6021+.5240207766*d); break;
            case JUPITER:
                planet = orbit(100.4542+2.76854e-5*d, 1.3030-1.557e-7*d,
                    273.8777+1.64505e-5*d, 5.20256, .048498+4.469e-9*d,
                    19.8950+.0830853001*d); break;
            case SATURN:
                planet = orbit(113.6634+2.38980e-5*d, 2.4886-1.081e-7*d,
                    339.3939+2.97661e-5*d, 9.55475, .055546-9.499e-9*d,
                    316.9670+.0334442282*d); break;
            default: throw new IllegalArgumentException("Unknown body");
        }
        double x = planet.x + sun.x, y = planet.y + sun.y, z = planet.z + sun.z;
        return equatorial(x, y, z, obliquity, Math.sqrt(x*x+y*y+z*z));
    }

    /** Geocentric ecliptic longitude, in degrees [0, 360). */
    public static double eclipticLongitude(Body body, Instant instant) {
        if (body != Body.SUN && body != Body.MOON)
            throw new IllegalArgumentException("Only Sun and Moon supported");
        Position p = position(body, instant);
        double d = instant.getEpochSecond() / 86400.0 + 2440587.5 - 2451543.5;
        double eps = rad(23.4393 - 3.563e-7 * d);
        double ra = rad(p.ra), dec = rad(p.dec);
        double x = Math.cos(dec) * Math.cos(ra);
        double y = Math.cos(dec) * Math.sin(ra) * Math.cos(eps)
            + Math.sin(dec) * Math.sin(eps);
        double longitude = deg(Math.atan2(y, x));
        return (longitude + 360) % 360;
    }

    private static Vector orbit(double node, double inclination, double perihelion,
                                double axis, double eccentricity, double anomaly) {
        double m = rad(anomaly);
        double e = m;
        for (int j = 0; j < 8; j++)
            e -= (e - eccentricity * Math.sin(e) - m) / (1 - eccentricity * Math.cos(e));
        double xp = axis * (Math.cos(e) - eccentricity);
        double yp = axis * Math.sqrt(1 - eccentricity*eccentricity) * Math.sin(e);
        double n = rad(node), i = rad(inclination), w = rad(perihelion);
        double cw = Math.cos(w), sw = Math.sin(w), cn = Math.cos(n), sn = Math.sin(n);
        double x = (cw*cn-sw*sn*Math.cos(i))*xp + (-sw*cn-cw*sn*Math.cos(i))*yp;
        double y = (cw*sn+sw*cn*Math.cos(i))*xp + (-sw*sn+cw*cn*Math.cos(i))*yp;
        double z = sw*Math.sin(i)*xp + cw*Math.sin(i)*yp;
        return new Vector(x, y, z, Math.hypot(xp, yp));
    }

    private static Position equatorial(double x, double y, double z,
                                       double obliquity, double distance) {
        double ye = y*cos(obliquity) - z*sin(obliquity);
        double ze = y*sin(obliquity) + z*cos(obliquity);
        return new Position(deg(Math.atan2(ye, x)), deg(Math.atan2(ze, Math.hypot(x, ye))),
            distance);
    }

    private static double rad(double degrees) { return Math.toRadians(degrees); }
    private static double deg(double radians) { return Math.toDegrees(radians); }
    private static double sin(double degrees) { return Math.sin(rad(degrees)); }
    private static double cos(double degrees) { return Math.cos(rad(degrees)); }
}
