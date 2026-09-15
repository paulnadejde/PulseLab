package ro.aquanano.pulselab;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import android.os.SystemClock;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import ro.aquanano.pulselab.core.HarmonicMixer;
import ro.aquanano.pulselab.core.MetronomeLogic;
import ro.aquanano.pulselab.core.VectorProgram;

/** One PCM mixer for the oscillator, noise and metronome. */
public final class AudioEngine {
    public enum Noise { NONE, WHITE, PINK, BROWN }

    private static final int SAMPLE_RATE = 48_000;
    private static final int FRAMES = 384;
    private final MetronomeLogic metronome = new MetronomeLogic();
    private final AtomicBoolean alive = new AtomicBoolean();
    private Thread audioThread;
    private AudioTrack track;

    private volatile boolean metronomeRunning;
    private volatile float clickVolume = 0.35f;
    private volatile float bellVolume = 0.35f;
    private long metroAccumulatedMs;
    private long metroStartMs;
    private int samplesToSecond;
    private int clickSamples;
    private int bellSamples;

    private volatile boolean generatorActive;
    private volatile boolean generatorPaused;
    private volatile boolean binaural = true;
    private volatile boolean monoSecondHarmonic;
    private volatile boolean monoThirdHarmonic;
    private volatile double carrierHz = 220.0;
    private volatile double beatHz = 10.0;
    private volatile float generatorVolume = 0.25f;
    private volatile Noise noise = Noise.NONE;
    private volatile float noiseVolume = 0.08f;
    private volatile VectorProgram vector;
    private volatile long sessionLimitMs;
    private long generatorAccumulatedMs;
    private long generatorStartMs;

    private double phaseLeft;
    private double phaseRight;
    private double monoPhase;
    private final Random random = new Random();
    private double pink0, pink1, pink2;
    private double brown;

    public MetronomeLogic metronome() { return metronome; }
    public boolean isMetronomeRunning() { return metronomeRunning; }
    public boolean isGeneratorActive() { return generatorActive; }
    public boolean isGeneratorPaused() { return generatorPaused; }
    public boolean isBinaural() { return binaural; }
    public void setBinauralMode(boolean enabled) { binaural = enabled; }
    public boolean usesSecondHarmonic() { return monoSecondHarmonic; }
    public boolean usesThirdHarmonic() { return monoThirdHarmonic; }
    public void setMonoHarmonics(boolean second, boolean third) {
        monoSecondHarmonic = second;
        monoThirdHarmonic = third;
    }
    public double carrierHz() { return carrierHz; }
    public float generatorVolume() { return generatorVolume; }
    public Noise noise() { return noise; }
    public float noiseVolume() { return noiseVolume; }
    public float clickVolume() { return clickVolume; }
    public float bellVolume() { return bellVolume; }

    public synchronized void start() {
        if (alive.getAndSet(true)) return;
        int min = AudioTrack.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferBytes = Math.max(min, FRAMES * 2 * 2 * 4);
        track = new AudioTrack.Builder()
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferBytes)
            .build();
        audioThread = new Thread(this::renderLoop, "AquaRitmAudio");
        audioThread.start();
    }

    public synchronized void shutdown() {
        alive.set(false);
        if (audioThread != null) audioThread.interrupt();
    }

    public synchronized void startMetronome() {
        metronome.resetPosition();
        samplesToSecond = SAMPLE_RATE;
        metroStartMs = SystemClock.elapsedRealtime();
        metronomeRunning = true;
    }

    public synchronized void pauseMetronome() {
        if (metronomeRunning) metroAccumulatedMs += SystemClock.elapsedRealtime() - metroStartMs;
        metronomeRunning = false;
        metronome.resetPosition();
    }

    public synchronized void resetMetronomeSession() {
        metroAccumulatedMs = 0;
        if (metronomeRunning) metroStartMs = SystemClock.elapsedRealtime();
    }

    public synchronized long metronomeSessionMs() {
        return metroAccumulatedMs + (metronomeRunning ? SystemClock.elapsedRealtime() - metroStartMs : 0);
    }

    public void setMetronomeVolumes(float click, float bell) {
        clickVolume = clamp01(click);
        bellVolume = clamp01(bell);
    }

    public synchronized void startGenerator(long limitMs, VectorProgram program) {
        vector = program;
        sessionLimitMs = program != null ? Math.round(program.totalSeconds() * 1000) : Math.max(0, limitMs);
        generatorAccumulatedMs = 0;
        generatorStartMs = SystemClock.elapsedRealtime();
        generatorPaused = false;
        generatorActive = true;
    }

    public synchronized void toggleGeneratorPause() {
        if (!generatorActive) return;
        if (generatorPaused) {
            generatorStartMs = SystemClock.elapsedRealtime();
            generatorPaused = false;
        } else {
            generatorAccumulatedMs += SystemClock.elapsedRealtime() - generatorStartMs;
            generatorPaused = true;
        }
    }

    public synchronized void stopGenerator() {
        generatorActive = false;
        generatorPaused = false;
        generatorAccumulatedMs = 0;
    }

    public synchronized long generatorElapsedMs() {
        if (!generatorActive) return generatorAccumulatedMs;
        return generatorAccumulatedMs + (!generatorPaused ? SystemClock.elapsedRealtime() - generatorStartMs : 0);
    }

    public long generatorLimitMs() { return sessionLimitMs; }
    public VectorProgram activeVector() { return vector; }
    public boolean isVectorActive() { return generatorActive && vector != null; }
    public VectorProgram.Position currentVectorPosition() {
        VectorProgram p = vector;
        return p == null ? null : p.at(generatorElapsedMs() / 1000.0);
    }
    public double currentBeatHz() {
        VectorProgram p = vector;
        if (p == null) return beatHz;
        return p.at(generatorElapsedMs() / 1000.0).frequencyHz;
    }
    public double currentCarrierHz() {
        VectorProgram p = vector;
        if (p == null) return carrierHz;
        double value = p.at(generatorElapsedMs() / 1000.0).carrierHz;
        return Double.isNaN(value) ? carrierHz : value;
    }

    public void configureGenerator(boolean binauralMode, double carrier, double beat,
                                   boolean secondHarmonic, boolean thirdHarmonic,
                                   float volume, Noise noiseType, float noiseLevel) {
        binaural = binauralMode;
        setMonoHarmonics(secondHarmonic, thirdHarmonic);
        carrierHz = Math.max(0.1, Math.min(9999.9, carrier));
        beatHz = Math.max(0, Math.min(999.99, Math.min(beat, carrierHz / 2.0)));
        generatorVolume = clamp01(volume);
        noise = noiseType == null ? Noise.NONE : noiseType;
        noiseVolume = clamp01(noiseLevel);
    }

    private void renderLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        short[] pcm = new short[FRAMES * 2];
        track.play();
        try {
            while (alive.get()) {
                render(pcm);
                track.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
            }
        } finally {
            try { track.pause(); track.flush(); track.release(); } catch (Exception ignored) { }
        }
    }

    private void render(short[] pcm) {
        boolean renderGenerator = generatorActive && !generatorPaused;
        double bufferDelta = beatHz;
        double bufferCarrier = carrierHz;
        if (renderGenerator) {
            long elapsed = generatorElapsedMs();
            if (sessionLimitMs > 0 && elapsed >= sessionLimitMs) {
                generatorAccumulatedMs = sessionLimitMs;
                generatorActive = false;
                renderGenerator = false;
            } else if (vector != null) {
                VectorProgram.Position position = vector.at(elapsed / 1000.0);
                if (position.finished) {
                    generatorAccumulatedMs = sessionLimitMs;
                    generatorActive = false;
                    renderGenerator = false;
                } else {
                    bufferDelta = position.frequencyHz;
                    if (!Double.isNaN(position.carrierHz)) bufferCarrier = position.carrierHz;
                }
            }
        }
        for (int frame = 0; frame < FRAMES; frame++) {
            double left = 0, right = 0;
            if (renderGenerator) {
                double delta = Math.min(bufferDelta, bufferCarrier / 2.0);
                if (binaural) {
                    double fL = Math.max(0.1, bufferCarrier - delta / 2.0);
                    double fR = Math.max(0.1, bufferCarrier + delta / 2.0);
                    phaseLeft = wrap(phaseLeft + twoPi(fL));
                    phaseRight = wrap(phaseRight + twoPi(fR));
                    left += Math.sin(phaseLeft) * generatorVolume;
                    right += Math.sin(phaseRight) * generatorVolume;
                } else {
                    monoPhase = wrap(monoPhase + twoPi(bufferCarrier));
                    boolean second = monoSecondHarmonic && bufferCarrier * 2.0 < SAMPLE_RATE / 2.0;
                    boolean third = monoThirdHarmonic && bufferCarrier * 3.0 < SAMPLE_RATE / 2.0;
                    double sample = HarmonicMixer.sample(monoPhase, second, third) * generatorVolume;
                    left += sample;
                    right += sample;
                }
                double n = nextNoise() * noiseVolume;
                left += n;
                right += n;
            }

            if (metronomeRunning) {
                if (samplesToSecond-- <= 0) {
                    clickSamples = SAMPLE_RATE / 45;
                    if (metronome.tick()) bellSamples = SAMPLE_RATE / 7;
                    samplesToSecond = SAMPLE_RATE - 1;
                }
                if (clickSamples > 0) {
                    double age = 1.0 - clickSamples / (double) (SAMPLE_RATE / 45);
                    double c = Math.sin(2 * Math.PI * 1700 * age / 45.0) * Math.exp(-9 * age) * clickVolume;
                    left += c; right += c; clickSamples--;
                }
                if (bellSamples > 0) {
                    double age = 1.0 - bellSamples / (double) (SAMPLE_RATE / 7);
                    double b = (Math.sin(2 * Math.PI * 1175 * age / 7.0)
                        + 0.35 * Math.sin(2 * Math.PI * 1762 * age / 7.0))
                        * Math.exp(-7 * age) * bellVolume * 0.7;
                    left += b; right += b; bellSamples--;
                }
            }

            pcm[frame * 2] = toPcm(left);
            pcm[frame * 2 + 1] = toPcm(right);
        }
    }

    private double nextNoise() {
        if (noise == Noise.NONE) return 0;
        double white = random.nextDouble() * 2 - 1;
        if (noise == Noise.WHITE) return white * 0.55;
        if (noise == Noise.PINK) {
            pink0 = 0.99765 * pink0 + white * 0.0990460;
            pink1 = 0.96300 * pink1 + white * 0.2965164;
            pink2 = 0.57000 * pink2 + white * 1.0526913;
            return (pink0 + pink1 + pink2 + white * 0.1848) * 0.12;
        }
        brown = Math.max(-1, Math.min(1, brown + white * 0.018));
        return brown * 0.7;
    }

    private static double twoPi(double frequency) { return 2 * Math.PI * frequency / SAMPLE_RATE; }
    private static double wrap(double phase) { return phase >= 2 * Math.PI ? phase - 2 * Math.PI : phase; }
    private static float clamp01(float v) { return Math.max(0, Math.min(1, v)); }
    private static short toPcm(double v) {
        v = Math.tanh(v * 0.92); // gentle safety limiter
        return (short) Math.round(Math.max(-1, Math.min(1, v)) * 32767);
    }
}
