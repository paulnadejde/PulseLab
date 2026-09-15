package ro.aquanano.pulselab.core;

import java.io.BufferedReader;
import java.io.Reader;

/** One-row monoaural frequency preset with two optional components. */
public final class FrequencyPreset {
    private static final String HEADER =
        "fundamental_hz,frequency_2_hz,volume_2_percent,frequency_3_hz,volume_3_percent";

    public final double fundamentalHz;
    public final double secondFrequencyHz;
    public final double secondVolumePercent;
    public final double thirdFrequencyHz;
    public final double thirdVolumePercent;

    private FrequencyPreset(double fundamentalHz,
                            double secondFrequencyHz, double secondVolumePercent,
                            double thirdFrequencyHz, double thirdVolumePercent) {
        this.fundamentalHz = fundamentalHz;
        this.secondFrequencyHz = secondFrequencyHz;
        this.secondVolumePercent = secondVolumePercent;
        this.thirdFrequencyHz = thirdFrequencyHz;
        this.thirdVolumePercent = thirdVolumePercent;
    }

    public boolean hasSecond() { return !Double.isNaN(secondFrequencyHz); }
    public boolean hasThird() { return !Double.isNaN(thirdFrequencyHz); }

    public static FrequencyPreset parseCsv(Reader source) throws Exception {
        BufferedReader reader = new BufferedReader(source);
        boolean headerSeen = false;
        FrequencyPreset result = null;
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (lineNumber == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (!headerSeen) {
                String normalized = line.toLowerCase().replace(" ", "");
                if (!HEADER.equals(normalized)) {
                    throw new Exception("antet invalid la linia " + lineNumber);
                }
                headerSeen = true;
                continue;
            }

            if (result != null) {
                throw new Exception("presetul trebuie să conțină un singur rând de valori");
            }
            String[] values = line.split(",", -1);
            if (values.length != 5) {
                throw new Exception("sunt necesari exact 5 parametri la linia " + lineNumber);
            }

            double fundamental = requiredFrequency(values[0], "fundamentală", lineNumber);
            double second = optionalFrequency(values[1], "frecvența 2", lineNumber);
            double secondVolume = componentVolume(values[2], second, "volumul 2", lineNumber);
            double third = optionalFrequency(values[3], "frecvența 3", lineNumber);
            double thirdVolume = componentVolume(values[4], third, "volumul 3", lineNumber);
            result = new FrequencyPreset(
                fundamental, second, secondVolume, third, thirdVolume);
        }

        if (!headerSeen) throw new Exception("lipsește antetul CSV");
        if (result == null) throw new Exception("lipsește rândul de valori");
        return result;
    }

    private static double requiredFrequency(String raw, String name, int line) throws Exception {
        double value = number(raw, name, line);
        if (value < 0.1 || value > 9999.9) {
            throw new Exception(name + " trebuie să fie între 0.1 și 9999.9 Hz");
        }
        return value;
    }

    private static double optionalFrequency(String raw, String name, int line) throws Exception {
        if (raw.trim().isEmpty()) return Double.NaN;
        return requiredFrequency(raw, name, line);
    }

    private static double componentVolume(String raw, double frequency,
                                          String name, int line) throws Exception {
        if (Double.isNaN(frequency)) {
            if (!raw.trim().isEmpty() && number(raw, name, line) != 0.0) {
                throw new Exception(name + " este definit fără frecvență");
            }
            return 0.0;
        }
        if (raw.trim().isEmpty()) return 20.0;
        double value = number(raw, name, line);
        if (value < 0.0 || value > 100.0) {
            throw new Exception(name + " trebuie să fie între 0 și 100%");
        }
        return value;
    }

    private static double number(String raw, String name, int line) throws Exception {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException invalid) {
            throw new Exception(name + ": număr invalid la linia " + line);
        }
    }
}
