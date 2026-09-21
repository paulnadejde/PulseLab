package ro.aquanano.pulselab.core;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class CoreTests {
    public static void main(String[] args) throws Exception {
        MetronomeLogic m = new MetronomeLogic();
        m.setDuration(0, 3);
        assert !m.advance(1);
        assert !m.advance(1);
        assert m.advance(1);
        m.setEnabled(1, true);
        m.setDuration(1, 4);
        m.setMultiplicativeMode(true);
        m.adjustMultiplicative(0.1);
        assert Math.abs(m.durations()[0] - 3.0) < 1e-9;
        assert Math.abs(m.durations()[1] - 4.0) < 1e-9;
        assert Math.abs(m.beatIntervalSeconds() - 1.1) < 1e-9;
        m.adjustMultiplicative(0.1);
        assert Math.abs(m.beatIntervalSeconds() - 1.2) < 1e-9;
        m.lockMode();
        m.setMultiplicativeMode(false);
        assert m.isMultiplicativeMode();
        m.adjustAdditive(-2);
        assert Math.abs(m.durations()[0] - 3.0) < 1e-9;
        assert Math.abs(m.durations()[1] - 4.0) < 1e-9;
        m.resetPosition();
        assert !m.advance(3.59);
        assert m.advance(0.01);
        m.unlockMode();
        m.setDuration(0, 3);
        m.setDuration(1, 5);
        assert Math.abs(m.durations()[0] - 3.0) < 1e-9;
        assert Math.abs(m.durations()[1] - 5.0) < 1e-9;

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

        assert Math.abs(HarmonicMixer.mix(1.0, false, 0.7, 0.2, false, -0.2, 0.2) - 1.0) < 1e-9;
        assert Math.abs(HarmonicMixer.mix(1.0, true, 0.0, 0.2, false, -0.2, 0.2) - 0.5) < 1e-9;
        assert Math.abs(HarmonicMixer.mix(1.0, true, 0.0, 1.0, true, -1.0, 1.0)) < 1e-9;

        assert VersionLogic.isValid("0.1.10");
        assert !VersionLogic.isValid("release");
        assert VersionLogic.compare("0.1.10", "0.1.9") > 0;
        assert VersionLogic.compare("1.0.0", "0.99.999") > 0;
        assert VersionLogic.compare("0.1.10", "0.1.10") == 0;
        assert VersionLogic.isNewer("0.1.11", "0.1.10");
        assert !VersionLogic.isNewer("0.1.9", "0.1.10");

        String frequencyCsv =
            "# Frecvență test\n"
                + "fundamental_hz,frequency_2_hz,volume_2_percent,frequency_3_hz,volume_3_percent\n"
                + "220.0,440.0,20,660.0,15\n";
        FrequencyPreset frequencyPreset =
            FrequencyPreset.parseCsv(new StringReader(frequencyCsv));
        assert Math.abs(frequencyPreset.fundamentalHz - 220.0) < 1e-9;
        assert frequencyPreset.hasSecond();
        assert Math.abs(frequencyPreset.secondVolumePercent - 20.0) < 1e-9;
        assert frequencyPreset.hasThird();
        assert Math.abs(frequencyPreset.thirdVolumePercent - 15.0) < 1e-9;

        String oneComponent =
            "fundamental_hz,frequency_2_hz,volume_2_percent,frequency_3_hz,volume_3_percent\n"
                + "180.0,360.0,20,0,0\n";
        FrequencyPreset one = FrequencyPreset.parseCsv(new StringReader(oneComponent));
        assert one.hasSecond();
        assert !one.hasThird();
        assert Math.abs(HarmonicMixer.mix(1.0, true, 1.0, 0.2, false, 0.0, 0.2)
            - 1.0) < 1e-9;
        boolean blankRejected = false;
        try {
            FrequencyPreset.parseCsv(new StringReader(
                "fundamental_hz,frequency_2_hz,volume_2_percent,frequency_3_hz,volume_3_percent\n"
                    + "180.0,360.0,20,,\n"));
        } catch (Exception expected) {
            blankRejected = true;
        }
        assert blankRejected;

        SolarCalculator.Events summer = SolarCalculator.calculate(
            LocalDate.of(2026, 6, 21), ZoneId.of("Europe/Bucharest"), 44.4268, 26.1025);
        assert summer.hasRiseAndSet();
        ZonedDateTime summerRise = summer.sunrise.atZone(summer.zone);
        ZonedDateTime summerSet = summer.sunset.atZone(summer.zone);
        assert summerRise.getHour() == 5;
        assert summerSet.getHour() == 21;
        assert summer.sunrise.isBefore(summer.sunset);

        SolarCalculator.Events winter = SolarCalculator.calculate(
            LocalDate.of(2026, 12, 21), ZoneId.of("Europe/Bucharest"), 44.4268, 26.1025);
        assert winter.hasRiseAndSet();
        assert winter.sunrise.atZone(winter.zone).getHour() == 7;
        assert winter.sunset.atZone(winter.zone).getHour() == 16;

        java.time.Instant reset = java.time.Instant.parse("2026-09-15T04:00:00Z");
        SolarCycle.State cycle = SolarCycle.at(reset, reset);
        assert cycle.large == 5 && cycle.small == 5;
        cycle = SolarCycle.at(reset, reset.plusSeconds(4 * 60 + 47));
        assert cycle.large == 5 && cycle.small == 5;
        cycle = SolarCycle.at(reset, reset.plusSeconds(4 * 60 + 48));
        assert cycle.large == 5 && cycle.small == 4;
        cycle = SolarCycle.at(reset, reset.plusSeconds(24 * 60));
        assert cycle.large == 4 && cycle.small == 5;
        cycle = SolarCycle.at(reset, reset.plusSeconds(5 * 24 * 60));
        assert cycle.large == 5 && cycle.small == 5;

        for (int i = 0; i < 10_000; i++) {
            double fundamental = Math.sin(i * 0.017);
            double second = Math.sin(i * 0.031);
            double third = Math.sin(i * 0.047);
            assert Math.abs(HarmonicMixer.mix(
                fundamental, false, second, 0.2, false, third, 0.2)) <= 1.0;
            assert Math.abs(HarmonicMixer.mix(
                fundamental, true, second, 0.2, false, third, 0.2)) <= 1.0;
            assert Math.abs(HarmonicMixer.mix(
                fundamental, false, second, 0.2, true, third, 0.2)) <= 1.0;
            assert Math.abs(HarmonicMixer.mix(
                fundamental, true, second, 0.2, true, third, 0.2)) <= 1.0;
        }
        System.out.println("Core tests passed");
    }
}
