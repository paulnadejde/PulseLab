package ro.aquanano.pulselab;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.util.Locale;

import ro.aquanano.pulselab.core.MetronomeLogic;
import ro.aquanano.pulselab.core.VectorProgram;

public final class MainActivity extends Activity {
    private static final int PICK_VECTOR = 1001;
    private static final int PICK_MUSIC = 1002;
    private static final int ACCENT = Color.rgb(69, 214, 196);
    private static final int PANEL = Color.rgb(21, 21, 21);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private LinearLayout content;
    private AudioService audioService;
    private boolean bound;
    private boolean showGenerator;
    private boolean keepMetro;
    private boolean keepGenerator;
    private TextView status;
    private TextView metroTimer;
    private TextView generatorTimer;
    private EditText[] sequenceValues;
    private CheckBox[] sequenceEnabled;
    private Spinner adjustmentMode;
    private EditText adjustmentValue;
    private SeekBar clickVolume;
    private SeekBar bellVolume;
    private Spinner presetSlot;
    private DigitDialView carrierDial;
    private DigitDialView beatDial;
    private RadioGroup audioMode;
    private SeekBar generatorVolume;
    private SeekBar overlayVolume;
    private Spinner noiseType;
    private EditText sessionMinutes;
    private CheckBox vectorMode;
    private TextView vectorLabel;
    private VectorProgram loadedVector;
    private Uri musicUri;
    private CheckBox strobe;
    private Spinner strobeColor;
    private boolean generatorWasActive;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            audioService = ((AudioService.LocalBinder) binder).service();
            bound = true;
            renderCurrentScreen();
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            bound = false;
            audioService = null;
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        Intent serviceIntent = new Intent(this, AudioService.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9);
        }
        handler.post(uiTicker);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (bound) unbindService(connection);
        super.onDestroy();
    }

    private void renderCurrentScreen() {
        root.removeAllViews();
        root.setBackgroundColor(Color.BLACK);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12), dp(8), dp(12), dp(18));
        root.addView(page, match());

        LinearLayout tabs = horizontal();
        Button metroTab = button("METRONOM");
        Button genTab = button("GENERATOR");
        metroTab.setEnabled(showGenerator);
        genTab.setEnabled(!showGenerator);
        tabs.addView(metroTab, weighted());
        tabs.addView(genTab, weighted());
        page.addView(tabs);
        metroTab.setOnClickListener(v -> { showGenerator = false; renderCurrentScreen(); });
        genTab.setOnClickListener(v -> { showGenerator = true; renderCurrentScreen(); });

        status = text("Motoare oprite", 13);
        status.setTextColor(ACCENT);
        page.addView(status);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, matchWidth());
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (!bound) {
            content.addView(text("Inițializez motorul audio…", 18));
        } else if (showGenerator) {
            buildGenerator();
        } else {
            buildMetronome();
        }
        applyKeepScreenOn();
    }

    private void buildMetronome() {
        title("Secvențe temporale");
        MetronomeLogic logic = audioService.engine().metronome();
        int[] durations = logic.durations();
        boolean[] enabled = logic.enabled();
        sequenceValues = new EditText[4];
        sequenceEnabled = new CheckBox[4];
        for (int i = 0; i < 4; i++) {
            final int index = i;
            LinearLayout row = horizontal();
            CheckBox check = new CheckBox(this);
            check.setText("Secvența " + (i + 1));
            check.setTextColor(Color.WHITE);
            check.setChecked(enabled[i]);
            EditText value = number(String.valueOf(durations[i]));
            sequenceEnabled[i] = check;
            sequenceValues[i] = value;
            row.addView(check, new LinearLayout.LayoutParams(0, dp(54), 2));
            row.addView(value, new LinearLayout.LayoutParams(0, dp(54), 1));
            TextView unit = text(" sec", 15);
            row.addView(unit);
            content.addView(row);
            check.setOnCheckedChangeListener((b, checked) -> {
                logic.setEnabled(index, checked);
                syncMetronomeFields();
            });
            value.setOnFocusChangeListener((v, has) -> { if (!has) syncMetronomeToEngine(); });
        }

        title("Reglaj simultan manual");
        adjustmentMode = spinner(new String[]{"Aditiv (secunde)", "Multiplicativ (bază × increment)"});
        adjustmentValue = decimal("1");
        content.addView(adjustmentMode);
        LinearLayout adjust = horizontal();
        Button minus = button("−");
        Button plus = button("+");
        adjust.addView(minus, weighted());
        adjust.addView(adjustmentValue, weighted());
        adjust.addView(plus, weighted());
        content.addView(adjust);
        adjustmentMode.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override public void selected(int position) {
                if (position == 1) logic.captureMultiplicativeBase();
            }
        });
        minus.setOnClickListener(v -> adjustMetronome(-1));
        plus.setOnClickListener(v -> adjustMetronome(1));

        title("Sunete");
        clickVolume = volumeRow("Click în fiecare secundă", Math.round(audioService.engine().clickVolume() * 100));
        bellVolume = volumeRow("Clopoțel la final de secvență", Math.round(audioService.engine().bellVolume() * 100));

        LinearLayout run = horizontal();
        Button startPause = button(audioService.engine().isMetronomeRunning() ? "PAUZĂ" : "START");
        Button resetValues = button("RESET VALORI");
        run.addView(startPause, weighted());
        run.addView(resetValues, weighted());
        content.addView(run);
        startPause.setOnClickListener(v -> {
            syncMetronomeToEngine();
            setMetroVolumes();
            if (audioService.engine().isMetronomeRunning()) {
                audioService.engine().pauseMetronome();
                audioService.leaveForegroundIfIdle();
            } else {
                audioService.enterForeground();
                audioService.engine().startMetronome();
            }
            startPause.setText(audioService.engine().isMetronomeRunning() ? "PAUZĂ" : "START");
        });
        resetValues.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Resetare metronom")
            .setMessage("Toate cele patru secvențe vor deveni 1 secundă, inclusiv cele inactive.")
            .setNegativeButton("Renunță", null)
            .setPositiveButton("Resetează", (d, w) -> {
                logic.resetValues();
                syncMetronomeFields();
            }).show());

        metroTimer = text("Sesiune: 00:00:00", 20);
        metroTimer.setGravity(Gravity.CENTER);
        content.addView(metroTimer);
        Button resetTimer = button("Reset timer sesiune");
        resetTimer.setOnClickListener(v -> audioService.engine().resetMetronomeSession());
        content.addView(resetTimer);

        title("Preseturi metronom (10 sloturi)");
        String[] slots = new String[10];
        for (int i = 0; i < 10; i++) slots[i] = "Preset " + (i + 1);
        presetSlot = spinner(slots);
        content.addView(presetSlot);
        LinearLayout presets = horizontal();
        Button save = button("SALVEAZĂ");
        Button load = button("ÎNCARCĂ");
        presets.addView(save, weighted());
        presets.addView(load, weighted());
        content.addView(presets);
        save.setOnClickListener(v -> saveMetroPreset());
        load.setOnClickListener(v -> loadMetroPreset());

        CheckBox keep = check("Menține ecranul aprins", keepMetro);
        keep.setOnCheckedChangeListener((b, checked) -> { keepMetro = checked; applyKeepScreenOn(); });
        content.addView(keep);
    }

    private void buildGenerator() {
        title("Mod generator");
        audioMode = new RadioGroup(this);
        audioMode.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton binaural = radio("Binaural", audioService.engine().isBinaural());
        RadioButton mono = radio("Monoaural", !audioService.engine().isBinaural());
        audioMode.addView(binaural, weighted());
        audioMode.addView(mono, weighted());
        audioMode.setOnCheckedChangeListener((group, checkedId) ->
            audioService.engine().setBinauralMode(checkedId == binaural.getId()));
        content.addView(audioMode);

        title("Purtătoare (Hz)");
        carrierDial = new DigitDialView(this, 1);
        carrierDial.setValue(audioService.engine().carrierHz());
        content.addView(carrierDial);
        title("Diferență / frecvență de bătaie (Hz)");
        beatDial = new DigitDialView(this, 2);
        beatDial.setValue(audioService.engine().currentBeatHz());
        content.addView(beatDial);

        generatorVolume = volumeRow("Volum generator", Math.round(audioService.engine().generatorVolume() * 100));
        title("Semnal suprapus");
        noiseType = spinner(new String[]{"Fără suprapunere", "Zgomot alb", "Zgomot roz", "Zgomot brun", "Piesă muzicală"});
        int overlaySelection = audioService.engine().noise().ordinal();
        if (musicUri != null && audioService.engine().noise() == AudioEngine.Noise.NONE) overlaySelection = 4;
        noiseType.setSelection(overlaySelection);
        content.addView(noiseType);
        overlayVolume = volumeRow("Volum semnal suprapus", Math.round(audioService.engine().noiseVolume() * 100));
        Button chooseMusic = button("Alege piesă muzicală (buclă)");
        chooseMusic.setOnClickListener(v -> pickFile(PICK_MUSIC, "audio/*"));
        content.addView(chooseMusic);

        title("Sesiune");
        sessionMinutes = decimal("20");
        LinearLayout duration = horizontal();
        duration.addView(text("Durată constantă", 16), new LinearLayout.LayoutParams(0, dp(52), 2));
        duration.addView(sessionMinutes, weighted());
        duration.addView(text(" minute", 15));
        content.addView(duration);
        vectorMode = check("Folosește vector CSV", false);
        content.addView(vectorMode);
        Button importVector = button("Importă vector CSV");
        importVector.setOnClickListener(v -> pickFile(PICK_VECTOR, "text/*"));
        content.addView(importVector);
        vectorLabel = text(loadedVector == null ? "Niciun vector încărcat" :
            String.format(Locale.US, "Vector: %.0f secunde", loadedVector.totalSeconds()), 13);
        content.addView(vectorLabel);

        title("Stroboscop");
        strobe = check("Activează modularea luminii", false);
        strobeColor = spinner(new String[]{"Alb", "Roșu", "Verde", "Albastru", "Chihlimbar"});
        content.addView(strobe);
        content.addView(strobeColor);
        strobe.setOnCheckedChangeListener((b, checked) -> {
            if (checked) showStrobeWarning();
            else root.setBackgroundColor(Color.BLACK);
        });

        LinearLayout run = horizontal();
        Button start = button("START");
        Button pause = button(audioService.engine().isGeneratorPaused() ? "REIA" : "PAUZĂ");
        Button stop = button("STOP");
        run.addView(start, weighted());
        run.addView(pause, weighted());
        run.addView(stop, weighted());
        content.addView(run);
        start.setOnClickListener(v -> startGenerator());
        pause.setOnClickListener(v -> {
            audioService.engine().toggleGeneratorPause();
            audioService.pauseMusic(audioService.engine().isGeneratorPaused());
            pause.setText(audioService.engine().isGeneratorPaused() ? "REIA" : "PAUZĂ");
        });
        stop.setOnClickListener(v -> stopGenerator());
        generatorTimer = text("Sesiune: 00:00 / 00:00", 20);
        generatorTimer.setGravity(Gravity.CENTER);
        content.addView(generatorTimer);

        CheckBox keep = check("Menține ecranul aprins", keepGenerator);
        keep.setOnCheckedChangeListener((b, checked) -> { keepGenerator = checked; applyKeepScreenOn(); });
        content.addView(keep);
    }

    private void adjustMetronome(int sign) {
        syncMetronomeToEngine();
        double amount = parseDouble(adjustmentValue, adjustmentMode.getSelectedItemPosition() == 0 ? 1 : 0.1);
        if (adjustmentMode.getSelectedItemPosition() == 0)
            audioService.engine().metronome().adjustAdditive(sign * (int) Math.round(amount));
        else
            audioService.engine().metronome().adjustMultiplicative(sign * amount);
        syncMetronomeFields();
    }

    private void syncMetronomeToEngine() {
        MetronomeLogic m = audioService.engine().metronome();
        for (int i = 0; i < 4; i++) {
            m.setDuration(i, Math.max(1, (int) parseDouble(sequenceValues[i], 1)));
            m.setEnabled(i, sequenceEnabled[i].isChecked());
        }
    }

    private void syncMetronomeFields() {
        int[] d = audioService.engine().metronome().durations();
        boolean[] e = audioService.engine().metronome().enabled();
        for (int i = 0; i < 4; i++) {
            sequenceValues[i].setText(String.valueOf(d[i]));
            sequenceEnabled[i].setChecked(e[i]);
        }
    }

    private void setMetroVolumes() {
        audioService.engine().setMetronomeVolumes(clickVolume.getProgress() / 100f,
            bellVolume.getProgress() / 100f);
    }

    private void saveMetroPreset() {
        try {
            syncMetronomeToEngine();
            JSONObject o = new JSONObject();
            o.put("durations", new JSONArray(audioService.engine().metronome().durations()));
            o.put("enabled", new JSONArray(audioService.engine().metronome().enabled()));
            o.put("click", clickVolume.getProgress());
            o.put("bell", bellVolume.getProgress());
            prefs().edit().putString("metro_" + presetSlot.getSelectedItemPosition(), o.toString()).apply();
            toast("Preset salvat");
        } catch (Exception e) { toast("Presetul nu a putut fi salvat"); }
    }

    private void loadMetroPreset() {
        try {
            String raw = prefs().getString("metro_" + presetSlot.getSelectedItemPosition(), null);
            if (raw == null) { toast("Slotul este gol"); return; }
            JSONObject o = new JSONObject(raw);
            JSONArray d = o.getJSONArray("durations");
            JSONArray en = o.getJSONArray("enabled");
            for (int i = 0; i < 4; i++) {
                audioService.engine().metronome().setDuration(i, d.getInt(i));
                audioService.engine().metronome().setEnabled(i, en.getBoolean(i));
            }
            clickVolume.setProgress(o.optInt("click", 35));
            bellVolume.setProgress(o.optInt("bell", 35));
            syncMetronomeFields();
            setMetroVolumes();
            toast("Preset încărcat");
        } catch (Exception e) { toast("Preset invalid"); }
    }

    private void startGenerator() {
        double carrier = carrierDial.getValue();
        double beat = beatDial.getValue();
        if (carrier < 0.1) { toast("Purtătoarea trebuie să fie peste 0 Hz"); return; }
        double maxBeat = Math.min(999.99, carrier / 2.0);
        if (beat > maxBeat) {
            beat = maxBeat;
            beatDial.setValue(beat);
            toast("Frecvența de bătaie a fost limitată la jumătatea purtătoarei");
        }
        int overlay = noiseType.getSelectedItemPosition();
        AudioEngine.Noise n = overlay >= 1 && overlay <= 3 ? AudioEngine.Noise.values()[overlay] : AudioEngine.Noise.NONE;
        audioService.engine().configureGenerator(audioMode.getCheckedRadioButtonId() == audioMode.getChildAt(0).getId(),
            carrier, beat, generatorVolume.getProgress() / 100f, n, overlayVolume.getProgress() / 100f);
        VectorProgram p = vectorMode.isChecked() ? loadedVector : null;
        if (vectorMode.isChecked() && p == null) { toast("Încarcă mai întâi un vector CSV"); return; }
        long limit = Math.round(parseDouble(sessionMinutes, 20) * 60_000);
        audioService.enterForeground();
        audioService.engine().startGenerator(limit, p);
        audioService.setMusicVolume(overlayVolume.getProgress() / 100f);
        if (overlay == 4) audioService.startMusicIfSelected();
        else audioService.stopMusic();
        generatorWasActive = true;
    }

    private void stopGenerator() {
        audioService.engine().stopGenerator();
        audioService.stopMusic();
        audioService.leaveForegroundIfIdle();
        generatorWasActive = false;
        root.setBackgroundColor(Color.BLACK);
    }

    private void pickFile(int code, String mime) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType(mime);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, code);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (Exception ignored) { }
        if (requestCode == PICK_VECTOR) {
            try (InputStreamReader reader = new InputStreamReader(getContentResolver().openInputStream(uri))) {
                loadedVector = VectorProgram.parseCsv(reader);
                toast("Vector încărcat: " + loadedVector.steps().size() + " pași");
                renderCurrentScreen();
            } catch (Exception e) { toast("CSV invalid: " + e.getMessage()); }
        } else if (requestCode == PICK_MUSIC) {
            musicUri = uri;
            audioService.setMusic(uri, 0.2f);
            toast("Piesă selectată");
        }
    }

    private void showStrobeWarning() {
        new AlertDialog.Builder(this)
            .setTitle("Avertisment stroboscop")
            .setMessage("Lumina intermitentă poate provoca disconfort, migrene sau crize persoanelor fotosensibile. Nu priviți ecranul dacă apare orice simptom.")
            .setNegativeButton("Dezactivează", (d, w) -> strobe.setChecked(false))
            .setPositiveButton("Am înțeles", null)
            .setOnCancelListener(d -> strobe.setChecked(false))
            .show();
    }

    private final Runnable uiTicker = new Runnable() {
        @Override public void run() {
            if (bound) {
                AudioEngine e = audioService.engine();
                if (status != null) {
                    String m = e.isMetronomeRunning() ? "Metronom activ" : "Metronom oprit";
                    String g = e.isGeneratorActive() ? (e.isGeneratorPaused() ? "Generator în pauză" : "Generator activ") : "Generator oprit";
                    status.setText(m + "  •  " + g);
                }
                if (metroTimer != null) metroTimer.setText("Sesiune: " + formatTime(e.metronomeSessionMs()));
                if (generatorTimer != null) {
                    generatorTimer.setText("Sesiune: " + formatTime(e.generatorElapsedMs()) + " / " + formatTime(e.generatorLimitMs()));
                }
                if (generatorWasActive && !e.isGeneratorActive()) {
                    audioService.stopMusic();
                    audioService.leaveForegroundIfIdle();
                    generatorWasActive = false;
                    if (strobe != null) strobe.setChecked(false);
                }
                updateStrobe();
            }
            handler.postDelayed(this, 50);
        }
    };

    private void updateStrobe() {
        if (!showGenerator || strobe == null || !strobe.isChecked() || !audioService.engine().isGeneratorActive()
            || audioService.engine().isGeneratorPaused()) {
            root.setBackgroundColor(Color.BLACK);
            return;
        }
        double f = audioService.engine().currentBeatHz();
        double t = android.os.SystemClock.elapsedRealtimeNanos() / 1_000_000_000.0;
        double intensity = 0.06 + 0.94 * (1 + Math.sin(2 * Math.PI * f * t)) / 2;
        int base = selectedStrobeColor();
        int r = (int) (Color.red(base) * intensity);
        int g = (int) (Color.green(base) * intensity);
        int b = (int) (Color.blue(base) * intensity);
        root.setBackgroundColor(Color.rgb(r, g, b));
    }

    private int selectedStrobeColor() {
        switch (strobeColor.getSelectedItemPosition()) {
            case 1: return Color.RED;
            case 2: return Color.GREEN;
            case 3: return Color.BLUE;
            case 4: return Color.rgb(255, 160, 20);
            default: return Color.WHITE;
        }
    }

    private void applyKeepScreenOn() {
        if (keepMetro || keepGenerator) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private SeekBar volumeRow(String label, int initial) {
        content.addView(text(label, 15));
        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        bar.setProgress(initial);
        content.addView(bar);
        return bar;
    }

    private void title(String value) {
        TextView t = text(value, 19);
        t.setTextColor(ACCENT);
        t.setPadding(0, dp(18), 0, dp(5));
        content.addView(t);
    }

    private TextView text(String value, int sp) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(6), dp(5), dp(6), dp(5));
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(Color.WHITE);
        return b;
    }

    private CheckBox check(String value, boolean checked) {
        CheckBox b = new CheckBox(this);
        b.setText(value);
        b.setTextColor(Color.WHITE);
        b.setChecked(checked);
        return b;
    }

    private RadioButton radio(String value, boolean checked) {
        RadioButton b = new RadioButton(this);
        b.setId(View.generateViewId());
        b.setText(value);
        b.setTextColor(Color.WHITE);
        b.setChecked(checked);
        return b;
    }

    private EditText number(String value) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setTextColor(Color.WHITE);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        e.setGravity(Gravity.CENTER);
        return e;
    }

    private EditText decimal(String value) {
        EditText e = number(value);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        s.setAdapter(a);
        return s;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0, dp(52), 1); }
    private FrameLayout.LayoutParams match() { return new FrameLayout.LayoutParams(-1, -1); }
    private LinearLayout.LayoutParams matchWidth() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private double parseDouble(EditText e, double fallback) {
        try { return Double.parseDouble(e.getText().toString().replace(',', '.')); }
        catch (Exception ignored) { return fallback; }
    }
    private SharedPreferences prefs() { return getSharedPreferences("presets", MODE_PRIVATE); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
    private static String formatTime(long millis) {
        long total = Math.max(0, millis / 1000);
        return String.format(Locale.US, "%02d:%02d:%02d", total / 3600, (total / 60) % 60, total % 60);
    }

    private abstract static class SimpleItemSelected implements AdapterView.OnItemSelectedListener {
        public abstract void selected(int position);
        @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { selected(pos); }
        @Override public void onNothingSelected(AdapterView<?> p) { }
    }
}
