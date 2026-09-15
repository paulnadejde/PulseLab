package ro.aquanano.pulselab.core;

/** Pure monoaural waveform composition, independent of the Android audio layer. */
public final class HarmonicMixer {
    private HarmonicMixer() { }

    public static double mix(double fundamentalSample,
                             boolean secondEnabled, double secondSample, double secondLevel,
                             boolean thirdEnabled, double thirdSample, double thirdLevel) {
        double value = fundamentalSample;
        double normalization = 1.0;
        if (secondEnabled) {
            double level = clamp01(secondLevel);
            value += secondSample * level;
            normalization += level;
        }
        if (thirdEnabled) {
            double level = clamp01(thirdLevel);
            value += thirdSample * level;
            normalization += level;
        }
        return value / normalization;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
