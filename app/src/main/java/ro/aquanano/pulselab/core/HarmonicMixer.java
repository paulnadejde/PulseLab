package ro.aquanano.pulselab.core;

/** Pure monoaural waveform composition, independent of the Android audio layer. */
public final class HarmonicMixer {
    private HarmonicMixer() { }

    public static double sample(double fundamentalPhase, boolean second, boolean third) {
        double value = Math.sin(fundamentalPhase);
        int components = 1;
        if (second) {
            value += Math.sin(2.0 * fundamentalPhase);
            components++;
        }
        if (third) {
            value += Math.sin(3.0 * fundamentalPhase);
            components++;
        }
        return value / components;
    }
}
