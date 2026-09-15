package ro.aquanano.pulselab.core;

/** Numeric comparison for the three-part AquaRitm release versions. */
public final class VersionLogic {
    private VersionLogic() { }

    public static boolean isValid(String version) {
        return version != null && version.matches("\\d+\\.\\d+\\.\\d+");
    }

    public static boolean isNewer(String available, String installed) {
        return compare(available, installed) > 0;
    }

    public static int compare(String left, String right) {
        long[] a = parse(left);
        long[] b = parse(right);
        for (int i = 0; i < 3; i++) {
            int comparison = Long.compare(a[i], b[i]);
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    private static long[] parse(String version) {
        if (!isValid(version)) return new long[]{0L, 0L, 0L};
        String[] parts = version.split("\\.");
        try {
            return new long[]{
                Long.parseLong(parts[0]),
                Long.parseLong(parts[1]),
                Long.parseLong(parts[2])
            };
        } catch (NumberFormatException invalid) {
            return new long[]{0L, 0L, 0L};
        }
    }
}
