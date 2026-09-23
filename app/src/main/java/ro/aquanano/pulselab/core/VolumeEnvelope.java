package ro.aquanano.pulselab.core;

/** Linear gain envelope used by the MindExtra binaural generator. */
public final class VolumeEnvelope {
    private final boolean fadeInEnabled;
    private final double fadeInStart;
    private final double fadeInEnd;
    private final double fadeInSeconds;
    private final boolean fadeOutEnabled;
    private final double fadeOutStart;
    private final double fadeOutEnd;
    private final double fadeOutSeconds;

    public VolumeEnvelope(boolean fadeInEnabled, double fadeInStart, double fadeInEnd,
                          double fadeInSeconds, boolean fadeOutEnabled,
                          double fadeOutStart, double fadeOutEnd, double fadeOutSeconds) {
        this.fadeInEnabled = fadeInEnabled;
        this.fadeInStart = clamp01(fadeInStart);
        this.fadeInEnd = clamp01(fadeInEnd);
        this.fadeInSeconds = Math.max(0.0, fadeInSeconds);
        this.fadeOutEnabled = fadeOutEnabled;
        this.fadeOutStart = clamp01(fadeOutStart);
        this.fadeOutEnd = clamp01(fadeOutEnd);
        this.fadeOutSeconds = Math.max(0.0, fadeOutSeconds);
    }

    public static VolumeEnvelope disabled() {
        return new VolumeEnvelope(false, 1.0, 1.0, 0.0,
            false, 1.0, 1.0, 0.0);
    }

    public double gainAt(double elapsedSeconds, double totalSeconds) {
        double elapsed = Math.max(0.0, elapsedSeconds);
        if (!fadeInEnabled && !fadeOutEnabled) return 1.0;

        if (fadeInEnabled && elapsed < fadeInSeconds) {
            return interpolate(fadeInStart, fadeInEnd,
                fadeInSeconds == 0.0 ? 1.0 : elapsed / fadeInSeconds);
        }

        if (fadeOutEnabled && totalSeconds > 0.0) {
            double fadeOutAt = Math.max(0.0, totalSeconds - fadeOutSeconds);
            if (elapsed >= fadeOutAt) {
                return interpolate(fadeOutStart, fadeOutEnd,
                    fadeOutSeconds == 0.0 ? 1.0 : (elapsed - fadeOutAt) / fadeOutSeconds);
            }
            if (fadeInEnabled) {
                double middleSeconds = fadeOutAt - fadeInSeconds;
                if (middleSeconds <= 0.0) return fadeOutStart;
                return interpolate(fadeInEnd, fadeOutStart,
                    (elapsed - fadeInSeconds) / middleSeconds);
            }
            return fadeOutStart;
        }

        return fadeInEnabled ? fadeInEnd : 1.0;
    }

    private static double interpolate(double from, double to, double position) {
        double p = Math.max(0.0, Math.min(1.0, position));
        return from + (to - from) * p;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
