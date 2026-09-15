package ro.aquanano.pulselab.core;

import java.io.StringReader;

public final class CoreTests {
    public static void main(String[] args) throws Exception {
        MetronomeLogic m = new MetronomeLogic();
        m.setDuration(0, 3);
        assert !m.tick();
        assert !m.tick();
        assert m.tick();
        m.setEnabled(1, true);
        m.setDuration(1, 4);
        m.captureMultiplicativeBase();
        m.adjustMultiplicative(0.5);
        assert m.durations()[0] == 5;
        assert m.durations()[1] == 6;
        m.adjustAdditive(-2);
        assert m.durations()[0] == 3;
        assert m.durations()[1] == 4;

        String csv = "# AquaRitm vector\n# Comments may precede the header\n"
                + "duration_seconds,frequency_hz,transition_seconds\n10,10,2\n5,6,0\n";
        VectorProgram v = VectorProgram.parseCsv(new StringReader(csv));
        assert Math.abs(v.totalSeconds() - 17) < 1e-9;
        assert Math.abs(v.at(11).frequencyHz - 8) < 1e-9;
        assert v.at(11).stepIndex == 0;
        assert v.at(11).transition;
        assert Math.abs(v.at(11).phaseElapsedSeconds - 1) < 1e-9;
        assert v.at(18).finished;

        String csv4 = "duration_seconds,carrier_hz,frequency_hz,transition_seconds\n"
                + "10,200,10,2\n5,220,6,0\n";
        VectorProgram v4 = VectorProgram.parseCsv(new StringReader(csv4));
        assert Math.abs(v4.at(11).carrierHz - 210) < 1e-9;
        assert Math.abs(v4.at(11).frequencyHz - 8) < 1e-9;

        assert Math.abs(HarmonicMixer.mix(1.0, false, 0.7, false, -0.2) - 1.0) < 1e-9;
        assert Math.abs(HarmonicMixer.mix(1.0, true, 0.0, false, -0.2) - 0.5) < 1e-9;
        assert Math.abs(HarmonicMixer.mix(1.0, true, 0.0, true, -1.0)) < 1e-9;

        assert VersionLogic.isValid("0.1.10");
        assert !VersionLogic.isValid("release");
        assert VersionLogic.compare("0.1.10", "0.1.9") > 0;
        assert VersionLogic.compare("1.0.0", "0.99.999") > 0;
        assert VersionLogic.compare("0.1.10", "0.1.10") == 0;
        assert VersionLogic.isNewer("0.1.11", "0.1.10");
        assert !VersionLogic.isNewer("0.1.9", "0.1.10");

        for (int i = 0; i < 10_000; i++) {
            double fundamental = Math.sin(i * 0.017);
            double second = Math.sin(i * 0.031);
            double third = Math.sin(i * 0.047);
            assert Math.abs(HarmonicMixer.mix(
                fundamental, false, second, false, third)) <= 1.0;
            assert Math.abs(HarmonicMixer.mix(
                fundamental, true, second, false, third)) <= 1.0;
            assert Math.abs(HarmonicMixer.mix(
                fundamental, false, second, true, third)) <= 1.0;
            assert Math.abs(HarmonicMixer.mix(
                fundamental, true, second, true, third)) <= 1.0;
        }
        System.out.println("Core tests passed");
    }
}
