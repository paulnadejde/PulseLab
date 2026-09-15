package ro.aquanano.pulselab.core;

/** Pure monoaural waveform composition, independent of the Android audio layer. */
public final class HarmonicMixer {
    private HarmonicMixer() { }

    public static double mix(double fundamentalSample,
                             boolean secondEnabled, double secondSample,
                             boolean thirdEnabled, double thirdSample) {
        double value = fundamentalSample;
        int components = 1;
        if (secondEnabled) {
            value += secondSample;
            components++;
        }
        if (thirdEnabled) {
            value += thirdSample;
            components++;
        }
        return value / components;
    }
}
