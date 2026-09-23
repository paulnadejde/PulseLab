package ro.aquanano.pulselab.core;

public final class VolumeEnvelopeTests {
    public static void main(String[] args) {
        VolumeEnvelope envelope = new VolumeEnvelope(
            true, 0.0, 1.0, 120.0, true, 0.8, 0.0, 120.0);
        check(envelope.gainAt(0.0, 3600.0), 0.0);
        check(envelope.gainAt(60.0, 3600.0), 0.5);
        check(envelope.gainAt(120.0, 3600.0), 1.0);
        check(envelope.gainAt(1800.0, 3600.0), 0.9);
        check(envelope.gainAt(3480.0, 3600.0), 0.8);
        check(envelope.gainAt(3540.0, 3600.0), 0.4);
        check(envelope.gainAt(3600.0, 3600.0), 0.0);

        VolumeEnvelope fadeInOnly = new VolumeEnvelope(
            true, 0.2, 0.6, 60.0, false, 1.0, 1.0, 0.0);
        check(fadeInOnly.gainAt(30.0, 0.0), 0.4);
        check(fadeInOnly.gainAt(300.0, 0.0), 0.6);

        VolumeEnvelope fadeOutOnly = new VolumeEnvelope(
            false, 1.0, 1.0, 0.0, true, 0.75, 0.25, 60.0);
        check(fadeOutOnly.gainAt(300.0, 600.0), 0.75);
        check(fadeOutOnly.gainAt(570.0, 600.0), 0.5);

        check(VolumeEnvelope.disabled().gainAt(15.0, 60.0), 1.0);
        System.out.println("Volume envelope tests passed");
    }

    private static void check(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-9) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
