package ro.aquanano.pulselab;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ro.aquanano.pulselab.core.VectorProgram;

/** Full-screen, landscape diagnostic view for a frequency vector. */
public final class VectorGraphActivity extends Activity {
    public static final String EXTRA_DURATIONS = "vector_durations";
    public static final String EXTRA_FREQUENCIES = "vector_frequencies";
    public static final String EXTRA_TRANSITIONS = "vector_transitions";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioService audioService;
    private boolean bound;
    private VectorProgram program;
    private VectorGraphView graph;
    private TextView frequency;
    private TextView stage;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            audioService = ((AudioService.LocalBinder) binder).service();
            bound = true;
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            bound = false;
            audioService = null;
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        program = readProgram(getIntent());
        if (program == null) { finish(); return; }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        graph = new VectorGraphView(this, program);
        root.addView(graph, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(16), dp(6), dp(16), dp(6));
        frequency = label("0.00 Hz", 24);
        stage = label("Etapa 1/" + program.steps().size(), 15);
        info.addView(frequency);
        info.addView(stage);
        FrameLayout.LayoutParams infoParams = new FrameLayout.LayoutParams(-2, -2);
        infoParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        root.addView(info, infoParams);

        Button close = new Button(this);
        close.setText("ÎNCHIDE");
        close.setTextColor(Color.WHITE);
        close.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(130), dp(54));
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(0, dp(8), dp(8), 0);
        root.addView(close, closeParams);
        setContentView(root);
        handler.post(ticker);
    }

    @Override protected void onStart() {
        super.onStart();
        bindService(new Intent(this, AudioService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override protected void onStop() {
        if (bound) unbindService(connection);
        bound = false;
        audioService = null;
        super.onStop();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long elapsedMs = bound && audioService.engine().activeVector() != null
                ? audioService.engine().generatorElapsedMs() : 0;
            double elapsed = Math.min(program.totalSeconds(), elapsedMs / 1000.0);
            VectorProgram.Position p = program.at(elapsed);
            graph.setCursorSeconds(elapsed);
            frequency.setText(String.format(Locale.US, "%.2f Hz", p.frequencyHz));
            String monoNote = bound && !audioService.engine().isBinaural()
                ? " • monoaural: fm nu intră în audio" : "";
            stage.setText(String.format(Locale.US, "Etapa %d/%d • %s • %s / %s%s",
                p.stepIndex + 1, program.steps().size(), p.transition ? "tranziție" : "menținere",
                formatTime(p.phaseElapsedSeconds), formatTime(p.phaseDurationSeconds), monoNote));
            handler.postDelayed(this, 50);
        }
    };

    private VectorProgram readProgram(Intent intent) {
        double[] d = intent.getDoubleArrayExtra(EXTRA_DURATIONS);
        double[] f = intent.getDoubleArrayExtra(EXTRA_FREQUENCIES);
        double[] t = intent.getDoubleArrayExtra(EXTRA_TRANSITIONS);
        if (d == null || f == null || t == null || d.length == 0 || d.length != f.length || d.length != t.length)
            return null;
        List<VectorProgram.Step> steps = new ArrayList<>();
        for (int i = 0; i < d.length; i++) steps.add(new VectorProgram.Step(d[i], f[i], t[i]));
        return new VectorProgram(steps);
    }

    private TextView label(String value, int sp) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(sp);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static String formatTime(double seconds) {
        long total = Math.max(0, Math.round(seconds));
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60);
    }

    private static final class VectorGraphView extends View {
        private final VectorProgram program;
        private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint curve = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint cursor = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private double cursorSeconds;

        VectorGraphView(Context context, VectorProgram value) {
            super(context);
            program = value;
            axis.setColor(Color.rgb(105, 105, 105));
            axis.setStrokeWidth(2);
            curve.setColor(Color.rgb(69, 214, 196));
            curve.setStyle(Paint.Style.STROKE);
            curve.setStrokeWidth(5);
            cursor.setColor(Color.rgb(255, 196, 48));
            cursor.setStrokeWidth(3);
            text.setColor(Color.WHITE);
            text.setTextSize(28);
        }

        void setCursorSeconds(double value) {
            cursorSeconds = Math.max(0, Math.min(program.totalSeconds(), value));
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float left = 70, top = 85, right = getWidth() - 35, bottom = getHeight() - 55;
            if (right <= left || bottom <= top) return;
            canvas.drawLine(left, bottom, right, bottom, axis);
            canvas.drawLine(left, top, left, bottom, axis);

            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (VectorProgram.Step s : program.steps()) {
                min = Math.min(min, s.frequencyHz);
                max = Math.max(max, s.frequencyHz);
            }
            if (max - min < 0.01) { min -= 1; max += 1; }
            double margin = (max - min) * 0.12;
            min = Math.max(0, min - margin);
            max += margin;

            Path path = new Path();
            double time = 0;
            VectorProgram.Step first = program.steps().get(0);
            path.moveTo(x(time, left, right), y(first.frequencyHz, min, max, top, bottom));
            for (int i = 0; i < program.steps().size(); i++) {
                VectorProgram.Step s = program.steps().get(i);
                time += s.durationSeconds;
                path.lineTo(x(time, left, right), y(s.frequencyHz, min, max, top, bottom));
                double next = i + 1 < program.steps().size()
                    ? program.steps().get(i + 1).frequencyHz : s.frequencyHz;
                time += s.transitionSeconds;
                path.lineTo(x(time, left, right), y(next, min, max, top, bottom));
            }
            canvas.drawPath(path, curve);

            VectorProgram.Position p = program.at(cursorSeconds);
            float cx = x(cursorSeconds, left, right);
            float cy = y(p.frequencyHz, min, max, top, bottom);
            canvas.drawLine(cx, top, cx, bottom, cursor);
            canvas.drawCircle(cx, cy, 9, cursor);
            canvas.drawText(String.format(Locale.US, "%.2f Hz", max), 6, top + 10, text);
            canvas.drawText(String.format(Locale.US, "%.2f Hz", min), 6, bottom, text);
            canvas.drawText("0", left, getHeight() - 14, text);
            String end = formatTime(program.totalSeconds());
            canvas.drawText(end, right - text.measureText(end), getHeight() - 14, text);
        }

        private float x(double seconds, float left, float right) {
            return (float) (left + (right - left) * seconds / Math.max(0.001, program.totalSeconds()));
        }

        private float y(double hz, double min, double max, float top, float bottom) {
            return (float) (bottom - (bottom - top) * (hz - min) / (max - min));
        }
    }
}
