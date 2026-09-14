package ro.aquanano.pulselab.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class VectorProgram {
    public static final class Step {
        public final double durationSeconds;
        /** NaN means: use the carrier selected in the dial (legacy 3-column CSV). */
        public final double carrierHz;
        public final double frequencyHz;
        public final double transitionSeconds;

        public Step(double durationSeconds, double frequencyHz, double transitionSeconds) {
            this(durationSeconds, Double.NaN, frequencyHz, transitionSeconds);
        }

        public Step(double durationSeconds, double carrierHz, double frequencyHz, double transitionSeconds) {
            if (durationSeconds < 0 || (!Double.isNaN(carrierHz) && carrierHz <= 0)
                    || frequencyHz < 0 || transitionSeconds < 0)
                throw new IllegalArgumentException("Invalid vector value");
            this.durationSeconds = durationSeconds;
            this.carrierHz = carrierHz;
            this.frequencyHz = frequencyHz;
            this.transitionSeconds = transitionSeconds;
        }
    }

    public static final class Position {
        public final double carrierHz;
        public final double frequencyHz;
        public final boolean finished;
        public final int stepIndex;
        public final boolean transition;
        public final double phaseElapsedSeconds;
        public final double phaseDurationSeconds;

        public Position(double carrierHz, double frequencyHz, boolean finished, int stepIndex, boolean transition,
                        double phaseElapsedSeconds, double phaseDurationSeconds) {
            this.carrierHz = carrierHz;
            this.frequencyHz = frequencyHz;
            this.finished = finished;
            this.stepIndex = stepIndex;
            this.transition = transition;
            this.phaseElapsedSeconds = phaseElapsedSeconds;
            this.phaseDurationSeconds = phaseDurationSeconds;
        }
    }

    private final List<Step> steps;
    private final double totalSeconds;

    public VectorProgram(List<Step> values) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("Empty vector");
        steps = Collections.unmodifiableList(new ArrayList<>(values));
        double total = 0;
        for (Step s : steps) total += s.durationSeconds + s.transitionSeconds;
        totalSeconds = total;
    }

    public List<Step> steps() { return steps; }
    public double totalSeconds() { return totalSeconds; }

    public Position at(double seconds) {
        if (seconds <= 0) {
            Step first = steps.get(0);
            return new Position(first.carrierHz, first.frequencyHz, false, 0, false, 0, first.durationSeconds);
        }
        double cursor = 0;
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            if (seconds < cursor + s.durationSeconds)
                return new Position(s.carrierHz, s.frequencyHz, false, i, false,
                    seconds - cursor, s.durationSeconds);
            cursor += s.durationSeconds;
            if (seconds < cursor + s.transitionSeconds) {
                double next = i + 1 < steps.size() ? steps.get(i + 1).frequencyHz : s.frequencyHz;
                double nextCarrier = i + 1 < steps.size() ? steps.get(i + 1).carrierHz : s.carrierHz;
                double p = s.transitionSeconds == 0 ? 1 : (seconds - cursor) / s.transitionSeconds;
                double carrier = Double.isNaN(s.carrierHz) || Double.isNaN(nextCarrier)
                    ? Double.NaN : s.carrierHz + (nextCarrier - s.carrierHz) * p;
                return new Position(carrier, s.frequencyHz + (next - s.frequencyHz) * p, false, i, true,
                    seconds - cursor, s.transitionSeconds);
            }
            cursor += s.transitionSeconds;
        }
        int lastIndex = steps.size() - 1;
        Step last = steps.get(lastIndex);
        return new Position(last.carrierHz, last.frequencyHz, true, lastIndex, last.transitionSeconds > 0,
            last.transitionSeconds > 0 ? last.transitionSeconds : last.durationSeconds,
            last.transitionSeconds > 0 ? last.transitionSeconds : last.durationSeconds);
    }

    public static VectorProgram parseCsv(Reader source) throws IOException {
        List<Step> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(source)) {
            String line;
            int lineNo = 0;
            boolean firstRecord = true;
            while ((line = br.readLine()) != null) {
                lineNo++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] p = trimmed.split("[;,]");
                if (p.length < 3) throw new IOException("CSV line " + lineNo + ": three columns required");
                if (firstRecord && p[0].replace("\uFEFF", "").trim()
                        .toLowerCase(Locale.ROOT).contains("duration")) {
                    firstRecord = false;
                    continue;
                }
                firstRecord = false;
                try {
                    double duration = Double.parseDouble(p[0].replace("\uFEFF", "").trim());
                    if (p.length >= 4) {
                        double carrier = Double.parseDouble(p[1].trim());
                        double frequency = Double.parseDouble(p[2].trim());
                        double transition = Double.parseDouble(p[3].trim());
                        result.add(new Step(duration, carrier, frequency, transition));
                    } else {
                        double frequency = Double.parseDouble(p[1].trim());
                        double transition = Double.parseDouble(p[2].trim());
                        result.add(new Step(duration, frequency, transition));
                    }
                } catch (NumberFormatException e) {
                    throw new IOException("CSV line " + lineNo + ": invalid number", e);
                }
            }
        }
        if (result.isEmpty()) throw new IOException("CSV contains no vector steps");
        return new VectorProgram(result);
    }
}
