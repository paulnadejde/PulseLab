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
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import ro.aquanano.pulselab.core.FrequencyPreset;
import ro.aquanano.pulselab.core.MetronomeLogic;
import ro.aquanano.pulselab.core.SolarCalculator;
import ro.aquanano.pulselab.core.SolarCycle;
import ro.aquanano.pulselab.core.VectorProgram;

public final class MainActivity extends Activity {
    private static final int SCREEN_METRONOME = 0;
    private static final int SCREEN_BIOSTIM = 1;
    private static final int SCREEN_MINDEXTRA = 2;
    private static final int SCREEN_SOLARITM = 3;
    private static final int PICK_VECTOR = 1001;
    private static final int PICK_MUSIC = 1002;
    private static final int PICK_ONLINE_PRESET = 1003;
    private static final int PICK_FREQUENCY_PRESET = 1004;
    private static final int LOCATION_PERMISSION_REQUEST = 10;
    private static final int ACCENT = Color.rgb(69, 214, 196);
    private static final int PANEL = Color.rgb(21, 21, 21);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private LinearLayout content;
    private AudioService audioService;
    private boolean bound;
    private int currentScreen = SCREEN_METRONOME;
    private boolean keepMetro;
    private boolean keepGenerator;
    private TextView status;
    private TextView metroTimer;
    private TextView generatorTimer;
    private EditText[] sequenceValues;
    private CheckBox[] sequenceEnabled;
    private Spinner adjustmentMode;
    private EditText adjustmentValue;
    private boolean multiplicativeBaseDirty = true;
    private boolean syncingMetronomeFields;
    private SeekBar clickVolume;
    private SeekBar bellVolume;
    private Spinner presetSlot;
    private EditText presetName;
    private DigitDialView carrierDial;
    private DigitDialView beatDial;
    private CheckBox secondHarmonic;
    private CheckBox thirdHarmonic;
    private Button secondFrequencyButton;
    private Button thirdFrequencyButton;
    private Button frequencyPresetButton;
    private CheckBox useFrequencyPreset;
    private TextView frequencyPresetLabel;
    private FrequencyPreset loadedFrequencyPreset;
    private String loadedFrequencyPresetName;
    private SeekBar generatorVolume;
    private SeekBar overlayVolume;
    private Spinner noiseType;
    private EditText sessionMinutes;
    private CheckBox vectorMode;
    private TextView vectorLabel;
    private TextView vectorStage;
    private Spinner downloadedPreset;
    private List<PresetStore.LocalPreset> downloadedPresets;
    private String selectedLocalPresetId;
    private Spinner downloadedAudio;
    private List<PresetStore.LocalAudio> downloadedAudioFiles;
    private VectorProgram loadedVector;
    private boolean useVector;
    private Uri musicUri;
    private CheckBox strobe;
    private Spinner strobeColor;
    private boolean generatorWasActive;
    private LocationManager locationManager;
    private Spinner solarLocationMode;
    private EditText solarLatitudeInput;
    private EditText solarLongitudeInput;
    private Button solarRefreshLocation;
    private TextView solarLocationStatus;
    private TextView solarSystemStatus;
    private TextView solarSunrise;
    private TextView solarSunset;
    private TextView solarPhase;
    private TextView solarNextEvent;
    private Switch solarUseSunsetSwitch;
    private TextView solarSunriseOnlyLabel;
    private TextView solarSunriseSunsetLabel;
    private SolarGlyphView solarLargeGlyph;
    private SolarGlyphView solarSmallGlyph;
    private double solarLatitude = Double.NaN;
    private double solarLongitude = Double.NaN;
    private String solarCalculationKey;
    private SolarCalculator.Events solarEvents;
    private SolarCalculator.Events solarYesterdayEvents;
    private SolarCalculator.Events solarTomorrowEvents;
    private Instant solarCycleAnchor;

    private final LocationListener solarLocationListener = new LocationListener() {
        @Override public void onLocationChanged(Location location) {
            if (currentScreen != SCREEN_SOLARITM
                    || !prefs().getBoolean("solar_use_phone_location", true)) return;
            prefs().edit()
                .putString("solar_phone_latitude", Double.toString(location.getLatitude()))
                .putString("solar_phone_longitude", Double.toString(location.getLongitude()))
                .putFloat("solar_phone_accuracy", location.hasAccuracy() ? location.getAccuracy() : -1f)
                .putString("solar_phone_provider", location.getProvider() == null ? "telefon" : location.getProvider())
                .apply();
            applySolarCoordinates(location.getLatitude(), location.getLongitude(),
                phoneLocationDescription(location, false));
        }

        @Override public void onProviderEnabled(String provider) { }
        @Override public void onProviderDisabled(String provider) {
            if (solarLocationStatus != null && currentScreen == SCREEN_SOLARITM)
                solarLocationStatus.setText("Furnizor indisponibil: " + provider);
        }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    };

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
        if (getIntent().getBooleanExtra("open_solaritm", false)) currentScreen = SCREEN_SOLARITM;
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
        SolarAgentService.sync(this);
        handler.post(uiTicker);
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs().getBoolean("energy_disable_strobe", false) && strobe != null) {
            strobe.setChecked(false);
            root.setBackgroundColor(Color.BLACK);
        }
        if (bound) renderCurrentScreen();
        else applyKeepScreenOn();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("open_solaritm", false)) {
            currentScreen = SCREEN_SOLARITM;
            renderCurrentScreen();
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopSolarLocationUpdates();
        if (bound) unbindService(connection);
        super.onDestroy();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                     int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST) return;
        boolean granted = false;
        for (int result : grantResults) granted |= result == PackageManager.PERMISSION_GRANTED;
        if (granted) startSolarLocationUpdates();
        else if (solarLocationStatus != null)
            solarLocationStatus.setText("Locație refuzată de Android. Folosește coordonatele manuale.");
    }

    private void renderCurrentScreen() {
        stopSolarLocationUpdates();
        root.removeAllViews();
        root.setBackgroundColor(Color.BLACK);
        carrierDial = null;
        beatDial = null;
        vectorMode = null;
        vectorStage = null;
        strobe = null;
        generatorTimer = null;
        solarLocationMode = null;
        solarLocationStatus = null;
        solarSystemStatus = null;
        solarSunrise = null;
        solarSunset = null;
        solarPhase = null;
        solarNextEvent = null;
        solarLatitudeInput = null;
        solarLongitudeInput = null;
        solarRefreshLocation = null;
        solarUseSunsetSwitch = null;
        solarSunriseOnlyLabel = null;
        solarSunriseSunsetLabel = null;
        solarLargeGlyph = null;
        solarSmallGlyph = null;
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12), dp(8), dp(12), dp(18));
        root.addView(page, match());

        LinearLayout tabs = horizontal();
        tabs.setGravity(Gravity.CENTER);
        NavigationIconView metroTab = new NavigationIconView(this,
            NavigationIconView.METRONOME, currentScreen == SCREEN_METRONOME, "Metronom");
        NavigationIconView bioStimTab = new NavigationIconView(this,
            NavigationIconView.BIOSTIM, currentScreen == SCREEN_BIOSTIM, "BioStim");
        NavigationIconView mindExtraTab = new NavigationIconView(this,
            NavigationIconView.MINDEXTRA, currentScreen == SCREEN_MINDEXTRA, "MindExtra");
        NavigationIconView solaRitmTab = new NavigationIconView(this,
            NavigationIconView.SOLARITM, currentScreen == SCREEN_SOLARITM, "SolaRitm");
        Button settingsButton = button("☰");
        settingsButton.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
        settingsButton.setTextSize(18);
        settingsButton.setContentDescription("Setări AquaRitm");
        int iconSize = Math.min(dp(62),
            (getResources().getDisplayMetrics().widthPixels - dp(24 + 60)) / 4);
        tabs.addView(metroTab, squareTabParams(iconSize));
        tabs.addView(bioStimTab, squareTabParams(iconSize));
        tabs.addView(mindExtraTab, squareTabParams(iconSize));
        tabs.addView(solaRitmTab, squareTabParams(iconSize));
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(44), dp(40));
        menuParams.setMargins(dp(3), dp(3), dp(3), dp(3));
        tabs.addView(settingsButton, menuParams);
        page.addView(tabs);
        metroTab.setOnClickListener(v -> { currentScreen = SCREEN_METRONOME; renderCurrentScreen(); });
        bioStimTab.setOnClickListener(v -> { currentScreen = SCREEN_BIOSTIM; renderCurrentScreen(); });
        mindExtraTab.setOnClickListener(v -> { currentScreen = SCREEN_MINDEXTRA; renderCurrentScreen(); });
        solaRitmTab.setOnClickListener(v -> { currentScreen = SCREEN_SOLARITM; renderCurrentScreen(); });
        settingsButton.setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        status = text("Motoare oprite", 13);
        status.setTextColor(ACCENT);
        page.addView(status);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, matchWidth());
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (currentScreen == SCREEN_SOLARITM) {
            buildSolaRitm();
        } else if (!bound) {
            content.addView(text("Inițializez motorul audio…", 18));
        } else if (currentScreen == SCREEN_BIOSTIM) {
            buildGenerator(true);
        } else if (currentScreen == SCREEN_MINDEXTRA) {
            buildGenerator(false);
        } else {
            buildMetronome();
        }
        applyKeepScreenOn();
    }

    private void buildMetronome() {
        title("Secvențe temporale");
        MetronomeLogic logic = audioService.engine().metronome();
        double[] durations = logic.durations();
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
            EditText value = decimal(formatSeconds(durations[i]));
            sequenceEnabled[i] = check;
            sequenceValues[i] = value;
            value.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable editable) {
                    if (!syncingMetronomeFields) multiplicativeBaseDirty = true;
                }
            });
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
        adjust.addView(minus, weightedButton());
        adjust.addView(adjustmentValue, weighted());
        adjust.addView(plus, weightedButton());
        content.addView(adjust);
        adjustmentMode.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override public void selected(int position) {
                if (position == 1) {
                    syncMetronomeToEngine();
                    logic.captureMultiplicativeBase();
                    multiplicativeBaseDirty = false;
                    adjustmentValue.setText("0.1");
                } else {
                    adjustmentValue.setText("1");
                }
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
        run.addView(startPause, weightedButton());
        run.addView(resetValues, weightedButton());
        content.addView(run);
        startPause.setOnClickListener(v -> {
            syncMetronomeToEngine();
            ensureMultiplicativeBase();
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
                multiplicativeBaseDirty = true;
                syncMetronomeFields();
            }).show());

        metroTimer = text("Sesiune: 00:00:00", 20);
        metroTimer.setGravity(Gravity.CENTER);
        content.addView(metroTimer);
        Button resetTimer = button("Reset timer sesiune");
        resetTimer.setOnClickListener(v -> audioService.engine().resetMetronomeSession());
        content.addView(resetTimer);

        title("Preseturi metronom (10 sloturi)");
        String[] slots = metroPresetLabels();
        presetSlot = spinner(slots);
        content.addView(presetSlot);
        presetName = new EditText(this);
        presetName.setTextColor(Color.WHITE);
        presetName.setHintTextColor(Color.GRAY);
        presetName.setHint("Numele presetului");
        presetName.setSingleLine(true);
        content.addView(presetName);
        presetSlot.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override public void selected(int position) {
                presetName.setText(metroPresetName(position));
            }
        });
        LinearLayout presets = horizontal();
        Button save = button("SALVEAZĂ");
        Button load = button("ÎNCARCĂ");
        presets.addView(save, weightedButton());
        presets.addView(load, weightedButton());
        content.addView(presets);
        save.setOnClickListener(v -> saveMetroPreset());
        load.setOnClickListener(v -> loadMetroPreset());

        boolean forceScreenOff = prefs().getBoolean("energy_screen_off", false);
        CheckBox keep = check("Menține ecranul aprins", keepMetro && !forceScreenOff);
        keep.setEnabled(!forceScreenOff);
        keep.setAlpha(forceScreenOff ? 0.55f : 1f);
        keep.setOnCheckedChangeListener((b, checked) -> { keepMetro = checked; applyKeepScreenOn(); });
        content.addView(keep);
    }

    private void buildGenerator(boolean bioStim) {
        title(bioStim ? "BioStim • generator monoaural"
                      : "MindExtra • generator binaural");
        TextView modeNote = text(bioStim
            ? "Semnalul rezultat este identic pe canalele stâng și drept."
            : "Stânga: f0 − fm/2  •  Dreapta: f0 + fm/2", 14);
        modeNote.setTextColor(Color.LTGRAY);
        content.addView(modeNote);

        if (bioStim) {
        title("Componente suplimentare");
        boolean secondEnabled = prefs().getBoolean("mono_h2_enabled",
            audioService.engine().usesSecondHarmonic());
        boolean thirdEnabled = prefs().getBoolean("mono_h3_enabled",
            audioService.engine().usesThirdHarmonic());
        boolean customSecond = prefs().getBoolean("mono_h2_custom",
            audioService.engine().usesCustomSecondFrequency());
        boolean customThird = prefs().getBoolean("mono_h3_custom",
            audioService.engine().usesCustomThirdFrequency());
        double secondHz = preferenceDouble("mono_h2_hz",
            audioService.engine().customSecondFrequencyHz());
        double thirdHz = preferenceDouble("mono_h3_hz",
            audioService.engine().customThirdFrequencyHz());
        int secondLevel = Math.max(0, Math.min(100,
            prefs().getInt("mono_h2_volume", 20)));
        int thirdLevel = Math.max(0, Math.min(100,
            prefs().getInt("mono_h3_volume", 20)));
        audioService.engine().setMonoHarmonics(secondEnabled, thirdEnabled);
        audioService.engine().setMonoFrequencyOverrides(
            customSecond, secondHz, customThird, thirdHz);
        audioService.engine().setMonoComponentLevels(
            secondLevel / 100f, thirdLevel / 100f);

        loadedFrequencyPreset = storedFrequencyPreset();
        loadedFrequencyPresetName = prefs().getString(
            "frequency_preset_name", "Niciun preset încărcat");

        secondHarmonic = check("Adaugă componenta a doua", secondEnabled);
        secondFrequencyButton = button(harmonicFrequencyButtonText(2));
        thirdHarmonic = check("Adaugă componenta a treia", thirdEnabled);
        thirdFrequencyButton = button(harmonicFrequencyButtonText(3));
        frequencyPresetButton = button("ÎNCARCĂ PRESET DE FRECVENȚE");
        useFrequencyPreset = check("Folosește presetul de frecvențe",
            loadedFrequencyPreset != null
                && prefs().getBoolean("frequency_preset_enabled", false));
        useFrequencyPreset.setEnabled(loadedFrequencyPreset != null);
        useFrequencyPreset.setAlpha(loadedFrequencyPreset != null ? 1f : 0.4f);
        frequencyPresetLabel = text(loadedFrequencyPresetName, 14);
        frequencyPresetLabel.setTextColor(ACCENT);
        content.addView(secondHarmonic);
        content.addView(secondFrequencyButton);
        content.addView(thirdHarmonic);
        content.addView(thirdFrequencyButton);
        content.addView(frequencyPresetButton);
        content.addView(useFrequencyPreset);
        content.addView(frequencyPresetLabel);

        secondHarmonic.setOnCheckedChangeListener((button, checked) -> {
            audioService.engine().setMonoHarmonics(checked, thirdHarmonic.isChecked());
            prefs().edit().putBoolean("mono_h2_enabled", checked).apply();
        });
        thirdHarmonic.setOnCheckedChangeListener((button, checked) -> {
            audioService.engine().setMonoHarmonics(secondHarmonic.isChecked(), checked);
            prefs().edit().putBoolean("mono_h3_enabled", checked).apply();
        });
        secondFrequencyButton.setOnClickListener(v -> showHarmonicFrequencyDialog(2));
        thirdFrequencyButton.setOnClickListener(v -> showHarmonicFrequencyDialog(3));
        frequencyPresetButton.setOnClickListener(v ->
            startActivityForResult(
                new Intent(this, FrequencyCatalogActivity.class),
                PICK_FREQUENCY_PRESET));
        useFrequencyPreset.setOnCheckedChangeListener((button, checked) -> {
            prefs().edit().putBoolean("frequency_preset_enabled", checked).apply();
            if (checked && loadedFrequencyPreset != null) {
                applyFrequencyPreset(loadedFrequencyPreset);
            }
        });
        }

        title(bioStim ? "Frecvență fundamentală f0 (Hz)" : "Purtătoare f0 (Hz)");
        carrierDial = new DigitDialView(this, 1);
        carrierDial.setValue(preferenceDouble(
            bioStim ? "biostim_carrier_hz" : "mindextra_carrier_hz",
            audioService.engine().carrierHz()));
        content.addView(carrierDial, UiStyle.frequencyDial(this));
        if (bioStim && useFrequencyPreset.isChecked() && loadedFrequencyPreset != null) {
            applyFrequencyPreset(loadedFrequencyPreset);
        }
        if (!bioStim) {
            title("Diferență / frecvență de bătaie fm (Hz)");
            beatDial = new DigitDialView(this, 2);
            beatDial.setValue(preferenceDouble(
                "mindextra_beat_hz", audioService.engine().currentBeatHz()));
            content.addView(beatDial, UiStyle.frequencyDial(this));
        }

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
        downloadedAudioFiles = PresetStore.listAudio(this);
        String[] audioLabels = downloadedAudioFiles.isEmpty()
            ? new String[]{"Niciun sunet descărcat"}
            : downloadedAudioFiles.stream().map(a -> a.name).toArray(String[]::new);
        downloadedAudio = spinner(audioLabels);
        content.addView(downloadedAudio);
        Button loadAudio = button("ÎNCARCĂ SUNET LOCAL");
        loadAudio.setEnabled(!downloadedAudioFiles.isEmpty());
        loadAudio.setAlpha(downloadedAudioFiles.isEmpty() ? 0.4f : 1f);
        loadAudio.setOnClickListener(v -> {
            if (!downloadedAudioFiles.isEmpty())
                loadDownloadedAudio(downloadedAudioFiles.get(downloadedAudio.getSelectedItemPosition()));
        });
        content.addView(loadAudio);
        Button onlineCatalog = button("CATALOG ONLINE");
        onlineCatalog.setOnClickListener(v -> startActivityForResult(
            new Intent(this, PresetCatalogActivity.class), PICK_ONLINE_PRESET));
        content.addView(onlineCatalog);

        title("Sesiune");
        sessionMinutes = decimal(prefs().getString(
            bioStim ? "biostim_session_minutes" : "mindextra_session_minutes", "20"));
        LinearLayout duration = horizontal();
        duration.addView(text("Durată constantă", 16), new LinearLayout.LayoutParams(0, dp(52), 2));
        duration.addView(sessionMinutes, weighted());
        duration.addView(text(" minute", 15));
        content.addView(duration);
        if (!bioStim) {
        vectorMode = check("Folosește vector CSV", useVector);
        vectorMode.setOnCheckedChangeListener((b, checked) -> useVector = checked);
        content.addView(vectorMode);
        Button importVector = button("Importă vector CSV");
        importVector.setOnClickListener(v -> pickFile(PICK_VECTOR, "text/*"));
        content.addView(importVector);
        vectorLabel = text(loadedVector == null ? "Niciun vector încărcat" :
            String.format(Locale.US, "Vector: %.0f secunde", loadedVector.totalSeconds()), 13);
        content.addView(vectorLabel);
        vectorStage = text("Vector inactiv", 16);
        vectorStage.setGravity(Gravity.CENTER);
        content.addView(vectorStage);
        Button showVectorGraph = button("AFIȘEAZĂ GRAFICUL VECTORULUI");
        showVectorGraph.setEnabled(loadedVector != null);
        showVectorGraph.setAlpha(loadedVector != null ? 1f : 0.4f);
        showVectorGraph.setOnClickListener(v -> showVectorGraph());
        content.addView(showVectorGraph);

        title("Preseturi vectoriale descărcate");
        downloadedPresets = PresetStore.list(this);
        String[] localLabels = downloadedPresets.isEmpty() ? new String[]{"Niciun preset descărcat"} :
            downloadedPresets.stream().map(p -> p.name).toArray(String[]::new);
        downloadedPreset = spinner(localLabels);
        content.addView(downloadedPreset);
        if (selectedLocalPresetId != null) {
            for (int i = 0; i < downloadedPresets.size(); i++) {
                if (selectedLocalPresetId.equals(downloadedPresets.get(i).id)) {
                    downloadedPreset.setSelection(i);
                    break;
                }
            }
        }
        Button loadLocal = button("ÎNCARCĂ LOCAL");
        loadLocal.setEnabled(!downloadedPresets.isEmpty());
        loadLocal.setAlpha(downloadedPresets.isEmpty() ? 0.4f : 1f);
        loadLocal.setOnClickListener(v -> {
            if (!downloadedPresets.isEmpty())
                loadDownloadedPreset(downloadedPresets.get(downloadedPreset.getSelectedItemPosition()));
        });
        content.addView(loadLocal);

        title("Stroboscop");
        boolean strobeDisabled = prefs().getBoolean("energy_disable_strobe", false);
        strobe = check(strobeDisabled
            ? "Modularea luminii este dezactivată din Setări"
            : "Activează modularea luminii", false);
        strobe.setEnabled(!strobeDisabled);
        strobe.setAlpha(strobeDisabled ? 0.55f : 1f);
        strobeColor = spinner(new String[]{"Alb", "Roșu", "Verde", "Albastru", "Chihlimbar"});
        strobeColor.setEnabled(!strobeDisabled);
        strobeColor.setAlpha(strobeDisabled ? 0.55f : 1f);
        content.addView(strobe);
        content.addView(strobeColor);
        strobe.setOnCheckedChangeListener((b, checked) -> {
            if (checked) showStrobeWarning();
            else root.setBackgroundColor(Color.BLACK);
        });
        }

        LinearLayout run = horizontal();
        Button start = button("START");
        boolean thisInstrumentActive = generatorMatchesScreen(audioService.engine(), bioStim);
        Button pause = button(thisInstrumentActive && audioService.engine().isGeneratorPaused()
            ? "REIA" : "PAUZĂ");
        Button stop = button("STOP");
        pause.setEnabled(thisInstrumentActive);
        pause.setAlpha(thisInstrumentActive ? 1f : 0.4f);
        stop.setEnabled(thisInstrumentActive);
        stop.setAlpha(thisInstrumentActive ? 1f : 0.4f);
        run.addView(start, weightedButton());
        run.addView(pause, weightedButton());
        run.addView(stop, weightedButton());
        content.addView(run);
        start.setOnClickListener(v -> {
            if (startGenerator(bioStim)) {
                pause.setEnabled(true);
                pause.setAlpha(1f);
                pause.setText("PAUZĂ");
                stop.setEnabled(true);
                stop.setAlpha(1f);
            }
        });
        pause.setOnClickListener(v -> {
            audioService.engine().toggleGeneratorPause();
            audioService.pauseMusic(audioService.engine().isGeneratorPaused());
            pause.setText(audioService.engine().isGeneratorPaused() ? "REIA" : "PAUZĂ");
        });
        stop.setOnClickListener(v -> {
            stopGenerator();
            pause.setText("PAUZĂ");
            pause.setEnabled(false);
            pause.setAlpha(0.4f);
            stop.setEnabled(false);
            stop.setAlpha(0.4f);
        });
        generatorTimer = text("Sesiune: 00:00 / 00:00", 20);
        generatorTimer.setGravity(Gravity.CENTER);
        content.addView(generatorTimer);

        boolean forceScreenOff = prefs().getBoolean("energy_screen_off", false);
        CheckBox keep = check("Menține ecranul aprins", keepGenerator && !forceScreenOff);
        keep.setEnabled(!forceScreenOff);
        keep.setAlpha(forceScreenOff ? 0.55f : 1f);
        keep.setOnCheckedChangeListener((b, checked) -> { keepGenerator = checked; applyKeepScreenOn(); });
        content.addView(keep);
    }

    private void buildSolaRitm() {
        title("SolaRitm • Răsărit și apus");
        TextView note = text(
            "Calcul local pentru limita convențională de −0,833°. Data, ora și fusul orar sunt preluate din Android.",
            14);
        note.setTextColor(Color.LTGRAY);
        content.addView(note);

        solarSunrise = text("Răsărit: --:--", 24);
        solarSunrise.setTextColor(Color.rgb(255, 190, 72));
        solarSunset = text("Apus: --:--", 24);
        solarSunset.setTextColor(Color.rgb(185, 103, 214));
        content.addView(solarSunrise);
        content.addView(solarSunset);

        solarPhase = text("Stare solară indisponibilă", 18);
        solarPhase.setTextColor(ACCENT);
        solarNextEvent = text("Următorul eveniment: --", 16);
        content.addView(solarPhase);
        content.addView(solarNextEvent);

        addSolarCyclePanel();

        Button settings = button("SETĂRI SOLARITM");
        settings.setOnClickListener(v -> showSolarSettingsDialog());
        content.addView(settings);

        TextView accuracyNote = text(
            "Orele sunt estimări astronomice. Relieful, clădirile, altitudinea și condițiile atmosferice pot deplasa momentul observat.",
            13);
        accuracyNote.setTextColor(Color.LTGRAY);
        content.addView(accuracyNote);

        setSolarLocationMode(prefs().getBoolean("solar_use_phone_location", true));
    }

    private void addSolarCyclePanel() {
        int available = getResources().getDisplayMetrics().widthPixels - dp(48);
        int size = Math.min(dp(320), available);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(12), dp(14), dp(12), dp(14));
        GradientDrawable frame = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(35, 29, 20), Color.rgb(17, 16, 16)});
        frame.setCornerRadius(dp(12));
        frame.setStroke(dp(3), Color.rgb(207, 160, 57));
        panel.setBackground(frame);
        panel.setElevation(dp(7));
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(size, size);
        panelParams.gravity = Gravity.CENTER_HORIZONTAL;
        panelParams.setMargins(dp(4), dp(14), dp(4), dp(10));

        TextView selectorTitle = text("Resetarea ciclului", 13);
        selectorTitle.setGravity(Gravity.CENTER);
        selectorTitle.setTextColor(Color.rgb(225, 190, 105));
        panel.addView(selectorTitle, new LinearLayout.LayoutParams(-1, dp(34)));

        LinearLayout selector = horizontal();
        solarSunriseOnlyLabel = text("Doar răsărit", 12);
        solarSunriseOnlyLabel.setGravity(Gravity.CENTER);
        solarSunriseSunsetLabel = text("Răsărit și apus", 12);
        solarSunriseSunsetLabel.setGravity(Gravity.CENTER);
        solarUseSunsetSwitch = new Switch(this);
        solarUseSunsetSwitch.setShowText(false);
        boolean includeSunset = prefs().getBoolean("solar_cycle_uses_sunset", false);
        solarUseSunsetSwitch.setChecked(includeSunset);
        selector.addView(solarSunriseOnlyLabel, new LinearLayout.LayoutParams(0, dp(54), 1f));
        selector.addView(solarUseSunsetSwitch,
            new LinearLayout.LayoutParams(dp(54), dp(54)));
        selector.addView(solarSunriseSunsetLabel, new LinearLayout.LayoutParams(0, dp(54), 1f));
        panel.addView(selector);
        refreshSolarModeLabels(includeSunset);
        solarUseSunsetSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs().edit().putBoolean("solar_cycle_uses_sunset", checked).apply();
            solarCalculationKey = null;
            refreshSolarModeLabels(checked);
            updateSolarDisplay();
        });

        LinearLayout glyphRow = horizontal();
        glyphRow.setGravity(Gravity.CENTER);
        solarLargeGlyph = new SolarGlyphView(this);
        solarSmallGlyph = new SolarGlyphView(this);
        glyphRow.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
        glyphRow.addView(solarLargeGlyph,
            new LinearLayout.LayoutParams(dp(124), dp(124)));
        glyphRow.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
        glyphRow.addView(solarSmallGlyph,
            new LinearLayout.LayoutParams(dp(62), dp(62)));
        glyphRow.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
        panel.addView(glyphRow, new LinearLayout.LayoutParams(-1, 0, 1f));
        content.addView(panel, panelParams);
    }

    private void refreshSolarModeLabels(boolean includeSunset) {
        if (solarSunriseOnlyLabel == null || solarSunriseSunsetLabel == null) return;
        solarSunriseOnlyLabel.setTextColor(includeSunset ? Color.GRAY : Color.WHITE);
        solarSunriseSunsetLabel.setTextColor(includeSunset ? Color.WHITE : Color.GRAY);
    }

    private void showSolarSettingsDialog() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(8), dp(18), dp(8));

        panel.addView(text("Sursa coordonatelor", 16));
        solarLocationMode = spinner(new String[]{"Locația telefonului", "Coordonate manuale"});
        boolean usePhone = prefs().getBoolean("solar_use_phone_location", true);
        solarLocationMode.setSelection(usePhone ? 0 : 1);
        panel.addView(solarLocationMode);
        solarLocationStatus = text(currentSolarLocationDescription(usePhone), 14);
        solarLocationStatus.setTextColor(ACCENT);
        panel.addView(solarLocationStatus);

        LinearLayout coordinates = horizontal();
        solarLatitudeInput = coordinateInput(
            prefs().getString("solar_manual_latitude", ""), "Latitudine");
        solarLongitudeInput = coordinateInput(
            prefs().getString("solar_manual_longitude", ""), "Longitudine");
        LinearLayout.LayoutParams coordinateParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        coordinateParams.setMargins(dp(4), dp(3), dp(4), dp(3));
        coordinates.addView(solarLatitudeInput, coordinateParams);
        LinearLayout.LayoutParams longitudeParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        longitudeParams.setMargins(dp(4), dp(3), dp(4), dp(3));
        coordinates.addView(solarLongitudeInput, longitudeParams);
        panel.addView(coordinates);

        Button remember = button("MEMOREAZĂ COORDONATE NOI");
        remember.setOnClickListener(v -> saveManualSolarCoordinates());
        panel.addView(remember);
        solarRefreshLocation = button("ACTUALIZEAZĂ LOCAȚIA");
        solarRefreshLocation.setEnabled(usePhone);
        solarRefreshLocation.setAlpha(usePhone ? 1f : .4f);
        solarRefreshLocation.setOnClickListener(v -> startSolarLocationUpdates());
        panel.addView(solarRefreshLocation);
        solarSystemStatus = text("", 13);
        solarSystemStatus.setTextColor(Color.LTGRAY);
        panel.addView(solarSystemStatus);

        solarLocationMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean selectedPhone = position == 0;
                if (selectedPhone != prefs().getBoolean("solar_use_phone_location", true))
                    setSolarLocationMode(selectedPhone);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Setări SolaRitm")
            .setView(scroll)
            .setPositiveButton("ÎNCHIDE", null)
            .create();
        dialog.setOnDismissListener(ignored -> {
            solarLocationMode = null;
            solarLatitudeInput = null;
            solarLongitudeInput = null;
            solarRefreshLocation = null;
            solarLocationStatus = null;
            solarSystemStatus = null;
        });
        dialog.show();
        updateSolarDisplay();
    }

    private String currentSolarLocationDescription(boolean usePhone) {
        if (!Double.isFinite(solarLatitude) || !Double.isFinite(solarLongitude))
            return usePhone ? "Caut locația telefonului…" : "Nu sunt memorate coordonate manuale.";
        String source = usePhone ? "Locația telefonului" : "Coordonate manuale memorate";
        if (usePhone) {
            String provider = prefs().getString("solar_phone_provider", "telefon");
            float accuracy = prefs().getFloat("solar_phone_accuracy", -1f);
            source += " • " + provider
                + (accuracy >= 0 ? String.format(Locale.US, " • precizie ±%.0f m", accuracy) : "");
        }
        return String.format(Locale.US, "%s\nLat %.6f° • Long %.6f°",
            source, solarLatitude, solarLongitude);
    }

    private EditText coordinateInput(String value, String hint) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setHint(hint);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setSingleLine(true);
        input.setGravity(Gravity.CENTER);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
            | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        return input;
    }

    private void setSolarLocationMode(boolean usePhone) {
        prefs().edit().putBoolean("solar_use_phone_location", usePhone).apply();
        if (solarRefreshLocation != null) {
            solarRefreshLocation.setEnabled(usePhone);
            solarRefreshLocation.setAlpha(usePhone ? 1f : 0.4f);
        }
        if (usePhone) {
            loadRememberedPhoneLocation();
            startSolarLocationUpdates();
        } else {
            stopSolarLocationUpdates();
            loadManualSolarCoordinates();
        }
    }

    private void saveManualSolarCoordinates() {
        try {
            double latitude = parseCoordinate(solarLatitudeInput.getText().toString(), -90, 90);
            double longitude = parseCoordinate(solarLongitudeInput.getText().toString(), -180, 180);
            String lat = String.format(Locale.US, "%.6f", latitude);
            String lon = String.format(Locale.US, "%.6f", longitude);
            solarLatitudeInput.setText(lat);
            solarLongitudeInput.setText(lon);
            prefs().edit()
                .putString("solar_manual_latitude", lat)
                .putString("solar_manual_longitude", lon)
                .apply();
            toast("Coordonate memorate");
            if (!prefs().getBoolean("solar_use_phone_location", true))
                applySolarCoordinates(latitude, longitude, "Coordonate manuale memorate");
        } catch (Exception error) {
            toast("Coordonate invalide. Latitudine −90…90; longitudine −180…180.");
        }
    }

    private void loadManualSolarCoordinates() {
        String lat = prefs().getString("solar_manual_latitude", "");
        String lon = prefs().getString("solar_manual_longitude", "");
        if (lat.isEmpty() || lon.isEmpty()) {
            solarLatitude = Double.NaN;
            solarLongitude = Double.NaN;
            solarCalculationKey = null;
            if (solarLocationStatus != null)
                solarLocationStatus.setText("Introdu coordonatele și apasă MEMOREAZĂ COORDONATE NOI.");
            clearSolarDisplay();
            return;
        }
        try {
            applySolarCoordinates(parseCoordinate(lat, -90, 90), parseCoordinate(lon, -180, 180),
                "Coordonate manuale memorate");
        } catch (Exception error) {
            clearSolarDisplay();
        }
    }

    private void loadRememberedPhoneLocation() {
        try {
            String lat = prefs().getString("solar_phone_latitude", "");
            String lon = prefs().getString("solar_phone_longitude", "");
            if (lat.isEmpty() || lon.isEmpty()) {
                solarLatitude = Double.NaN;
                solarLongitude = Double.NaN;
                solarCalculationKey = null;
                solarCycleAnchor = null;
                clearSolarDisplay();
                return;
            }
            float accuracy = prefs().getFloat("solar_phone_accuracy", -1f);
            String provider = prefs().getString("solar_phone_provider", "telefon");
            String description = "Ultima locație memorată • " + provider
                + (accuracy >= 0 ? String.format(Locale.US, " • precizie ±%.0f m", accuracy) : "");
            applySolarCoordinates(Double.parseDouble(lat), Double.parseDouble(lon), description);
        } catch (Exception ignored) { }
    }

    private void startSolarLocationUpdates() {
        if (currentScreen != SCREEN_SOLARITM
                || !prefs().getBoolean("solar_use_phone_location", true)) return;
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
        boolean coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            if (solarLocationStatus != null)
                solarLocationStatus.setText("Acordă accesul la locație cât timp folosești aplicația.");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }
        if (locationManager == null)
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            if (solarLocationStatus != null) solarLocationStatus.setText("Serviciul de locație nu este disponibil.");
            return;
        }

        // Reînregistrarea este intenționată la apăsarea butonului de actualizare,
        // dar același listener nu trebuie păstrat de mai multe ori.
        stopSolarLocationUpdates();

        Location best = null;
        boolean listening = false;
        for (String provider : locationManager.getProviders(true)) {
            try {
                Location last = locationManager.getLastKnownLocation(provider);
                if (last != null && (best == null || last.getTime() > best.getTime())) best = last;
                locationManager.requestLocationUpdates(provider, 15_000L, 100f,
                    solarLocationListener, Looper.getMainLooper());
                listening = true;
            } catch (SecurityException | IllegalArgumentException ignored) { }
        }
        if (best != null) {
            applySolarCoordinates(best.getLatitude(), best.getLongitude(),
                phoneLocationDescription(best, true));
        } else if (solarLocationStatus != null) {
            solarLocationStatus.setText(listening
                ? "Caut locația telefonului…" : "Activează locația telefonului sau folosește coordonate manuale.");
        }
    }

    private void stopSolarLocationUpdates() {
        if (locationManager == null) return;
        try { locationManager.removeUpdates(solarLocationListener); }
        catch (SecurityException ignored) { }
    }

    private String phoneLocationDescription(Location location, boolean cached) {
        String provider = location.getProvider() == null ? "telefon" : location.getProvider();
        return (cached ? "Locație recentă" : "Locația telefonului") + " • " + provider
            + (location.hasAccuracy()
                ? String.format(Locale.US, " • precizie ±%.0f m", location.getAccuracy()) : "");
    }

    private void applySolarCoordinates(double latitude, double longitude, String description) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return;
        solarLatitude = latitude;
        solarLongitude = longitude;
        solarCalculationKey = null;
        if (solarLocationStatus != null) {
            solarLocationStatus.setText(String.format(Locale.US,
                "%s\nLat %.6f° • Long %.6f°", description, latitude, longitude));
        }
        updateSolarDisplay();
    }

    private void clearSolarDisplay() {
        if (solarSunrise != null) solarSunrise.setText("Răsărit: --:--");
        if (solarSunset != null) solarSunset.setText("Apus: --:--");
        if (solarPhase != null) solarPhase.setText("Stare solară indisponibilă");
        if (solarNextEvent != null) solarNextEvent.setText("Următorul eveniment: --");
        if (solarLargeGlyph != null) solarLargeGlyph.setValue(5);
        if (solarSmallGlyph != null) solarSmallGlyph.setValue(5);
    }

    private void updateSolarDisplay() {
        if (currentScreen != SCREEN_SOLARITM || solarSunrise == null
                || !Double.isFinite(solarLatitude) || !Double.isFinite(solarLongitude)) return;
        ZonedDateTime now = ZonedDateTime.now();
        LocalDate date = now.toLocalDate();
        ZoneId zone = now.getZone();
        boolean includeSunset = prefs().getBoolean("solar_cycle_uses_sunset", false);
        String key = date + "|" + zone.getId() + "|" + solarLatitude + "|" + solarLongitude
            + "|" + includeSunset;
        if (!key.equals(solarCalculationKey)) {
            solarEvents = SolarCalculator.calculate(date, zone, solarLatitude, solarLongitude);
            solarYesterdayEvents = SolarCalculator.calculate(date.minusDays(1), zone,
                solarLatitude, solarLongitude);
            solarTomorrowEvents = SolarCalculator.calculate(date.plusDays(1), zone,
                solarLatitude, solarLongitude);
            solarCycleAnchor = latestSolarCycleAnchor(now, includeSunset);
            solarCalculationKey = key;
        }
        advanceSolarCycleAnchor(now.toInstant(), includeSunset);
        if (solarSystemStatus != null) {
            solarSystemStatus.setText(String.format(Locale.US, "%s • %s • UTC%s",
                date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), zone.getId(),
                now.getOffset().getId().equals("Z") ? "+00:00" : now.getOffset().getId()));
        }
        updateSolarGlyphs(now.toInstant());

        if (!solarEvents.hasRiseAndSet()) {
            solarSunrise.setText("Răsărit: nu are loc astăzi");
            solarSunset.setText("Apus: nu are loc astăzi");
            solarPhase.setText(solarEvents.polarDay ? "Soare deasupra orizontului toată ziua"
                : "Soare sub orizont toată ziua");
            solarNextEvent.setText("Următorul eveniment necesită calcul pentru o dată viitoare.");
            return;
        }

        DateTimeFormatter clock = DateTimeFormatter.ofPattern("HH:mm:ss");
        solarSunrise.setText("Răsărit: " + clock.format(solarEvents.sunrise.atZone(zone)));
        solarSunset.setText("Apus: " + clock.format(solarEvents.sunset.atZone(zone)));
        Instant instant = now.toInstant();
        Instant next;
        String nextName;
        if (instant.isBefore(solarEvents.sunrise)) {
            solarPhase.setText("Înainte de răsărit");
            next = solarEvents.sunrise;
            nextName = "răsărit";
        } else if (instant.isBefore(solarEvents.sunset)) {
            solarPhase.setText("Soarele este deasupra orizontului");
            next = solarEvents.sunset;
            nextName = "apus";
        } else {
            solarPhase.setText("După apus");
            next = solarTomorrowEvents.sunrise;
            nextName = "răsăritul de mâine";
        }
        if (next == null) solarNextEvent.setText("Următorul eveniment: nedeterminat");
        else solarNextEvent.setText("Până la " + nextName + ": "
            + formatSolarCountdown(Duration.between(instant, next)));
    }

    private Instant latestSolarCycleAnchor(ZonedDateTime now, boolean includeSunset) {
        Instant instant = now.toInstant();
        LocalDate date = now.toLocalDate();
        ZoneId zone = now.getZone();
        for (int daysBack = 0; daysBack <= 370; daysBack++) {
            SolarCalculator.Events events;
            if (daysBack == 0) events = solarEvents;
            else if (daysBack == 1) events = solarYesterdayEvents;
            else events = SolarCalculator.calculate(date.minusDays(daysBack), zone,
                solarLatitude, solarLongitude);
            Instant latest = null;
            if (events.sunrise != null && !events.sunrise.isAfter(instant)) latest = events.sunrise;
            if (includeSunset && events.sunset != null && !events.sunset.isAfter(instant)
                    && (latest == null || events.sunset.isAfter(latest))) latest = events.sunset;
            if (latest != null) return latest;
        }
        return date.atStartOfDay(zone).toInstant();
    }

    private void advanceSolarCycleAnchor(Instant now, boolean includeSunset) {
        if (solarEvents == null) return;
        Instant candidate = null;
        if (solarEvents.sunrise != null && !solarEvents.sunrise.isAfter(now))
            candidate = solarEvents.sunrise;
        if (includeSunset && solarEvents.sunset != null && !solarEvents.sunset.isAfter(now)
                && (candidate == null || solarEvents.sunset.isAfter(candidate)))
            candidate = solarEvents.sunset;
        if (candidate != null && (solarCycleAnchor == null || candidate.isAfter(solarCycleAnchor)))
            solarCycleAnchor = candidate;
    }

    private void updateSolarGlyphs(Instant now) {
        if (solarCycleAnchor == null || solarLargeGlyph == null || solarSmallGlyph == null) return;
        SolarCycle.State state = SolarCycle.at(solarCycleAnchor, now);
        solarLargeGlyph.setValue(state.large);
        solarSmallGlyph.setValue(state.small);
    }

    private static String formatSolarCountdown(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long days = seconds / 86_400;
        long hours = (seconds % 86_400) / 3_600;
        long minutes = (seconds % 3_600) / 60;
        long remaining = seconds % 60;
        return days > 0
            ? String.format(Locale.US, "%d zile %02d:%02d:%02d", days, hours, minutes, remaining)
            : String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, remaining);
    }

    private static double parseCoordinate(String value, double minimum, double maximum) {
        double parsed = Double.parseDouble(value.trim().replace(',', '.'));
        if (!Double.isFinite(parsed) || parsed < minimum || parsed > maximum)
            throw new IllegalArgumentException("coordinate");
        return parsed;
    }

    private String harmonicFrequencyButtonText(int order) {
        boolean second = order == 2;
        boolean custom = second
            ? audioService.engine().usesCustomSecondFrequency()
            : audioService.engine().usesCustomThirdFrequency();
        int level = Math.round((second
            ? audioService.engine().monoSecondLevel()
            : audioService.engine().monoThirdLevel()) * 100f);
        if (!custom) return String.format(Locale.US,
            second ? "ALTĂ FRECVENȚĂ 2: standard 2 × f0 • %d%%"
                   : "ALTĂ FRECVENȚĂ 3: standard 3 × f0 • %d%%",
            level);
        double hz = second
            ? audioService.engine().customSecondFrequencyHz()
            : audioService.engine().customThirdFrequencyHz();
        return String.format(Locale.US,
            "ALTĂ FRECVENȚĂ %d: %.1f Hz • %d%%", order, hz, level);
    }

    private void showHarmonicFrequencyDialog(int order) {
        boolean second = order == 2;
        double currentHz = second
            ? audioService.engine().customSecondFrequencyHz()
            : audioService.engine().customThirdFrequencyHz();
        boolean useCustom = second
            ? audioService.engine().usesCustomSecondFrequency()
            : audioService.engine().usesCustomThirdFrequency();
        int currentLevel = Math.round((second
            ? audioService.engine().monoSecondLevel()
            : audioService.engine().monoThirdLevel()) * 100f);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(10), dp(18), dp(4));
        DigitDialView frequency = new DigitDialView(this, 1);
        frequency.setValue(currentHz);
        CheckBox useFrequency = check("Folosește această frecvență", useCustom);
        TextView levelLabel = text("Volum: " + currentLevel + "% din fundamentală", 15);
        SeekBar level = new SeekBar(this);
        level.setMax(100);
        level.setProgress(currentLevel);
        level.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                levelLabel.setText("Volum: " + value + "% din fundamentală");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        panel.addView(frequency, UiStyle.frequencyDial(this));
        panel.addView(useFrequency);
        panel.addView(levelLabel);
        panel.addView(level);

        new AlertDialog.Builder(this)
            .setTitle(second ? "Componenta a doua" : "Componenta a treia")
            .setView(panel)
            .setNegativeButton("ANULEAZĂ", null)
            .setPositiveButton("SALVEAZĂ", (dialog, which) -> {
                double hz = frequency.getValue();
                boolean selected = useFrequency.isChecked();
                int volume = level.getProgress();
                AudioEngine engine = audioService.engine();
                float secondVolume = second
                    ? volume / 100f : engine.monoSecondLevel();
                float thirdVolume = second
                    ? engine.monoThirdLevel() : volume / 100f;
                engine.setMonoComponentLevels(secondVolume, thirdVolume);
                if (second) {
                    engine.setMonoFrequencyOverrides(selected, hz,
                        engine.usesCustomThirdFrequency(), engine.customThirdFrequencyHz());
                    prefs().edit()
                        .putBoolean("mono_h2_custom", selected)
                        .putString("mono_h2_hz", Double.toString(hz))
                        .putInt("mono_h2_volume", volume)
                        .apply();
                    secondFrequencyButton.setText(harmonicFrequencyButtonText(2));
                } else {
                    engine.setMonoFrequencyOverrides(
                        engine.usesCustomSecondFrequency(), engine.customSecondFrequencyHz(),
                        selected, hz);
                    prefs().edit()
                        .putBoolean("mono_h3_custom", selected)
                        .putString("mono_h3_hz", Double.toString(hz))
                        .putInt("mono_h3_volume", volume)
                        .apply();
                    thirdFrequencyButton.setText(harmonicFrequencyButtonText(3));
                }
            })
            .show();
    }

    private FrequencyPreset storedFrequencyPreset() {
        String csv = prefs().getString("frequency_preset_csv", "");
        if (csv.isEmpty()) return null;
        try {
            return FrequencyPreset.parseCsv(new StringReader(csv));
        } catch (Exception invalid) {
            prefs().edit()
                .remove("frequency_preset_csv")
                .putBoolean("frequency_preset_enabled", false)
                .apply();
            return null;
        }
    }

    private void applyFrequencyPreset(FrequencyPreset preset) {
        if (preset == null || carrierDial == null) return;
        carrierDial.setValue(preset.fundamentalHz);
        secondHarmonic.setChecked(preset.hasSecond());
        thirdHarmonic.setChecked(preset.hasThird());

        AudioEngine engine = audioService.engine();
        double secondHz = preset.hasSecond()
            ? preset.secondFrequencyHz : engine.customSecondFrequencyHz();
        double thirdHz = preset.hasThird()
            ? preset.thirdFrequencyHz : engine.customThirdFrequencyHz();
        engine.setMonoFrequencyOverrides(
            preset.hasSecond(), secondHz, preset.hasThird(), thirdHz);
        engine.setMonoComponentLevels(
            (float) (preset.secondVolumePercent / 100.0),
            (float) (preset.thirdVolumePercent / 100.0));

        prefs().edit()
            .putString("biostim_carrier_hz", Double.toString(preset.fundamentalHz))
            .putBoolean("mono_h2_enabled", preset.hasSecond())
            .putBoolean("mono_h3_enabled", preset.hasThird())
            .putBoolean("mono_h2_custom", preset.hasSecond())
            .putBoolean("mono_h3_custom", preset.hasThird())
            .putString("mono_h2_hz", Double.toString(secondHz))
            .putString("mono_h3_hz", Double.toString(thirdHz))
            .putInt("mono_h2_volume", (int) Math.round(preset.secondVolumePercent))
            .putInt("mono_h3_volume", (int) Math.round(preset.thirdVolumePercent))
            .apply();
        secondFrequencyButton.setText(harmonicFrequencyButtonText(2));
        thirdFrequencyButton.setText(harmonicFrequencyButtonText(3));
        frequencyPresetLabel.setText(loadedFrequencyPresetName);
    }

    private double preferenceDouble(String key, double fallback) {
        try {
            return Double.parseDouble(prefs().getString(key, Double.toString(fallback)));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void adjustMetronome(int sign) {
        syncMetronomeToEngine();
        boolean multiplicative = adjustmentMode.getSelectedItemPosition() == 1;
        ensureMultiplicativeBase();
        double amount = parseDouble(adjustmentValue, multiplicative ? 0.1 : 1.0);
        if (multiplicative) {
            amount = Math.max(0.1, Math.min(2.0, Math.round(amount * 10.0) / 10.0));
            adjustmentValue.setText(String.format(Locale.US, "%.1f", amount));
            audioService.engine().metronome().adjustMultiplicative(sign * amount);
        } else {
            int seconds = Math.max(1, Math.min(10, (int) Math.round(amount)));
            adjustmentValue.setText(Integer.toString(seconds));
            audioService.engine().metronome().adjustAdditive(sign * seconds);
        }
        syncMetronomeFields();
    }

    private void ensureMultiplicativeBase() {
        if (adjustmentMode != null && adjustmentMode.getSelectedItemPosition() == 1
                && multiplicativeBaseDirty) {
            audioService.engine().metronome().captureMultiplicativeBase();
            multiplicativeBaseDirty = false;
        }
    }

    private void syncMetronomeToEngine() {
        MetronomeLogic m = audioService.engine().metronome();
        for (int i = 0; i < 4; i++) {
            m.setDuration(i, Math.max(0.1, parseDouble(sequenceValues[i], 1)));
            m.setEnabled(i, sequenceEnabled[i].isChecked());
        }
    }

    private void syncMetronomeFields() {
        double[] d = audioService.engine().metronome().durations();
        boolean[] e = audioService.engine().metronome().enabled();
        syncingMetronomeFields = true;
        try {
            for (int i = 0; i < 4; i++) {
                sequenceValues[i].setText(formatSeconds(d[i]));
                sequenceEnabled[i].setChecked(e[i]);
            }
        } finally {
            syncingMetronomeFields = false;
        }
    }

    private String formatSeconds(double seconds) {
        double shown = Math.round(seconds * 10.0) / 10.0;
        if (Math.abs(shown - Math.rint(shown)) < 1e-9)
            return Long.toString(Math.round(shown));
        return String.format(Locale.US, "%.1f", shown);
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
            String name = presetName.getText().toString().trim();
            if (name.isEmpty()) name = "Preset " + (presetSlot.getSelectedItemPosition() + 1);
            o.put("name", name);
            prefs().edit().putString("metro_" + presetSlot.getSelectedItemPosition(), o.toString()).apply();
            int selected = presetSlot.getSelectedItemPosition();
            presetSlot.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, metroPresetLabels()));
            presetSlot.setSelection(selected);
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
                audioService.engine().metronome().setDuration(i, d.getDouble(i));
                audioService.engine().metronome().setEnabled(i, en.getBoolean(i));
            }
            multiplicativeBaseDirty = true;
            clickVolume.setProgress(o.optInt("click", 35));
            bellVolume.setProgress(o.optInt("bell", 35));
            syncMetronomeFields();
            setMetroVolumes();
            toast("Preset încărcat");
        } catch (Exception e) { toast("Preset invalid"); }
    }

    private boolean startGenerator(boolean bioStim) {
        if (bioStim && useFrequencyPreset != null && useFrequencyPreset.isChecked()
            && loadedFrequencyPreset != null) {
            applyFrequencyPreset(loadedFrequencyPreset);
        }
        double carrier = carrierDial.getValue();
        double beat = bioStim ? 0.0 : beatDial.getValue();
        if (carrier < 0.1) {
            toast("Frecvența fundamentală trebuie să fie peste 0 Hz");
            return false;
        }
        if (!bioStim) {
            double maxBeat = Math.min(999.99, carrier / 2.0);
            if (beat > maxBeat) {
                beat = maxBeat;
                beatDial.setValue(beat);
                toast("Frecvența de bătaie a fost limitată la jumătatea purtătoarei");
            }
        }
        int overlay = noiseType.getSelectedItemPosition();
        AudioEngine.Noise n = overlay >= 1 && overlay <= 3 ? AudioEngine.Noise.values()[overlay] : AudioEngine.Noise.NONE;
        boolean vectorRequested = !bioStim && vectorMode != null && vectorMode.isChecked();
        if (!bioStim) useVector = vectorRequested;
        VectorProgram p = vectorRequested ? loadedVector : null;
        if (vectorRequested && p == null) {
            toast("Încarcă mai întâi un vector CSV");
            return false;
        }
        boolean second = bioStim && secondHarmonic.isChecked();
        boolean third = bioStim && thirdHarmonic.isChecked();
        audioService.engine().configureGenerator(!bioStim,
            carrier, beat, second, third,
            generatorVolume.getProgress() / 100f, n, overlayVolume.getProgress() / 100f);
        long limit = Math.round(parseDouble(sessionMinutes, 20) * 60_000);
        SharedPreferences.Editor settings = prefs().edit()
            .putString(bioStim ? "biostim_carrier_hz" : "mindextra_carrier_hz",
                Double.toString(carrier))
            .putString(bioStim ? "biostim_session_minutes" : "mindextra_session_minutes",
                sessionMinutes.getText().toString());
        if (!bioStim) settings.putString("mindextra_beat_hz", Double.toString(beat));
        settings.apply();
        audioService.enterForeground();
        audioService.engine().startGenerator(limit, p);
        audioService.setMusicVolume(overlayVolume.getProgress() / 100f);
        if (overlay == 4) audioService.startMusicIfSelected();
        else audioService.stopMusic();
        generatorWasActive = true;
        return true;
    }

    private boolean generatorMatchesScreen(AudioEngine engine, boolean bioStim) {
        return engine.isGeneratorActive() && engine.isBinaural() != bioStim;
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
        if (requestCode == PICK_FREQUENCY_PRESET
            && resultCode == RESULT_OK && data != null) {
            String csv = data.getStringExtra(FrequencyCatalogActivity.RESULT_CSV);
            String name = data.getStringExtra(FrequencyCatalogActivity.RESULT_NAME);
            try {
                FrequencyPreset preset = FrequencyPreset.parseCsv(new StringReader(csv));
                loadedFrequencyPreset = preset;
                loadedFrequencyPresetName = name == null || name.trim().isEmpty()
                    ? "Preset de frecvențe" : name.trim();
                prefs().edit()
                    .putString("frequency_preset_csv", csv)
                    .putString("frequency_preset_name", loadedFrequencyPresetName)
                    .putBoolean("frequency_preset_enabled", true)
                    .apply();
                toast("Preset încărcat: " + loadedFrequencyPresetName);
                renderCurrentScreen();
            } catch (Exception error) {
                toast("Preset de frecvențe invalid: " + error.getMessage());
            }
            return;
        }
        if (requestCode == PICK_ONLINE_PRESET && resultCode == RESULT_OK && data != null) {
            String id = data.getStringExtra(PresetCatalogActivity.RESULT_PRESET_ID);
            String type = data.getStringExtra(PresetCatalogActivity.RESULT_RESOURCE_TYPE);
            if (PresetCatalogActivity.TYPE_AUDIO.equals(type)) {
                PresetStore.LocalAudio audio = PresetStore.findAudio(this, id);
                if (audio != null) loadDownloadedAudio(audio);
            } else {
                PresetStore.LocalPreset preset = PresetStore.find(this, id);
                if (preset != null) loadDownloadedPreset(preset);
            }
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (Exception ignored) { }
        if (requestCode == PICK_VECTOR) {
            try {
                String csv = readUriText(uri);
                loadedVector = VectorProgram.parseCsv(new StringReader(csv));
                PresetStore.LocalPreset saved = PresetStore.importVector(
                    this, displayName(uri), csv);
                selectedLocalPresetId = saved.id;
                useVector = true;
                toast("Vector salvat local: " + saved.name);
                renderCurrentScreen();
            } catch (Exception e) { toast("Import CSV eșuat: " + e.getMessage()); }
        } else if (requestCode == PICK_MUSIC) {
            musicUri = uri;
            audioService.setMusic(uri, 0.2f);
            toast("Piesă selectată");
        }
    }

    private void loadDownloadedAudio(PresetStore.LocalAudio audio) {
        musicUri = Uri.fromFile(audio.audioFile);
        audioService.setMusic(musicUri, 0.2f);
        toast("Sunet local selectat: " + audio.name);
        renderCurrentScreen();
    }

    private void loadDownloadedPreset(PresetStore.LocalPreset preset) {
        try (FileReader reader = new FileReader(preset.vectorFile)) {
            loadedVector = VectorProgram.parseCsv(reader);
            selectedLocalPresetId = preset.id;
            useVector = true;
            musicUri = preset.audioFile == null ? null : Uri.fromFile(preset.audioFile);
            audioService.setMusic(musicUri, 0.2f);
            toast("Preset local încărcat: " + preset.name);
            renderCurrentScreen();
        } catch (Exception e) {
            toast("Presetul local nu a putut fi citit: " + e.getMessage());
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
                    if (currentScreen == SCREEN_SOLARITM) {
                        status.setText("SolaRitm • calcul astronomic local");
                    } else {
                        String m = e.isMetronomeRunning() ? "Metronom activ" : "Metronom oprit";
                        String instrument = e.isBinaural() ? "MindExtra" : "BioStim";
                        String g = e.isGeneratorActive()
                            ? instrument + (e.isGeneratorPaused() ? " în pauză" : " activ")
                            : "Generatoare oprite";
                        status.setText(m + "  •  " + g);
                    }
                }
                if (metroTimer != null) metroTimer.setText("Sesiune: " + formatTime(e.metronomeSessionMs()));
                if (generatorTimer != null) {
                    boolean bioStimScreen = currentScreen == SCREEN_BIOSTIM;
                    if (generatorMatchesScreen(e, bioStimScreen)) {
                        generatorTimer.setText("Sesiune: " + formatTime(e.generatorElapsedMs())
                            + " / " + formatTime(e.generatorLimitMs()));
                    } else if (e.isGeneratorActive()) {
                        generatorTimer.setText((e.isBinaural() ? "MindExtra" : "BioStim")
                            + " rulează în cealaltă filă");
                    } else {
                        generatorTimer.setText("Sesiune: 00:00 / 00:00");
                    }
                }
                updateVectorStatus(e);
                if (generatorWasActive && !e.isGeneratorActive()) {
                    audioService.stopMusic();
                    audioService.leaveForegroundIfIdle();
                    generatorWasActive = false;
                    if (strobe != null) strobe.setChecked(false);
                }
                updateStrobe();
            }
            if (currentScreen == SCREEN_SOLARITM) updateSolarDisplay();
            boolean strobeNeedsFastRefresh = strobe != null && strobe.isChecked();
            long delay = !strobeNeedsFastRefresh && prefs().getBoolean("energy_slow_ui", false)
                ? 1000L : 50L;
            handler.postDelayed(this, delay);
        }
    };

    private void updateVectorStatus(AudioEngine engine) {
        if (currentScreen != SCREEN_MINDEXTRA || beatDial == null || vectorStage == null) return;
        if (!engine.isVectorActive()) {
            vectorStage.setText(useVector && loadedVector != null ? "Vector pregătit" : "Vector inactiv");
            return;
        }
        VectorProgram.Position position = engine.currentVectorPosition();
        VectorProgram program = engine.activeVector();
        if (position == null || program == null) return;
        if (Math.abs(beatDial.getValue() - position.frequencyHz) >= 0.005)
            beatDial.setValue(position.frequencyHz);
        if (!Double.isNaN(position.carrierHz)
                && Math.abs(carrierDial.getValue() - position.carrierHz) >= 0.05)
            carrierDial.setValue(position.carrierHz);
        String phase = position.transition ? "tranziție" : "menținere";
        String monoNote = engine.isBinaural() ? "" : " • fm neaplicat audio în monoaural";
        double currentCarrier = Double.isNaN(position.carrierHz) ? engine.currentCarrierHz() : position.carrierHz;
        vectorStage.setText(String.format(Locale.US,
            "VECTOR ACTIV • Etapa %d/%d • %s %s / %s • f0 %.1f Hz • fm %.2f Hz%s",
            position.stepIndex + 1, program.steps().size(), phase,
            formatTime(Math.round(position.phaseElapsedSeconds * 1000)),
            formatTime(Math.round(position.phaseDurationSeconds * 1000)),
            currentCarrier, position.frequencyHz, monoNote));
    }

    private void showVectorGraph() {
        if (loadedVector == null) { toast("Încarcă mai întâi un vector CSV"); return; }
        int count = loadedVector.steps().size();
        double[] durations = new double[count];
        double[] frequencies = new double[count];
        double[] carriers = new double[count];
        double[] transitions = new double[count];
        for (int i = 0; i < count; i++) {
            VectorProgram.Step step = loadedVector.steps().get(i);
            durations[i] = step.durationSeconds;
            carriers[i] = step.carrierHz;
            frequencies[i] = step.frequencyHz;
            transitions[i] = step.transitionSeconds;
        }
        Intent graph = new Intent(this, VectorGraphActivity.class);
        graph.putExtra(VectorGraphActivity.EXTRA_DURATIONS, durations);
        graph.putExtra(VectorGraphActivity.EXTRA_CARRIERS, carriers);
        graph.putExtra(VectorGraphActivity.EXTRA_FREQUENCIES, frequencies);
        graph.putExtra(VectorGraphActivity.EXTRA_TRANSITIONS, transitions);
        startActivity(graph);
    }

    private String[] metroPresetLabels() {
        String[] labels = new String[10];
        for (int i = 0; i < labels.length; i++)
            labels[i] = (i + 1) + " — " + metroPresetName(i);
        return labels;
    }

    private String metroPresetName(int index) {
        String fallback = "Preset " + (index + 1);
        try {
            String raw = prefs().getString("metro_" + index, null);
            return raw == null ? fallback : new JSONObject(raw).optString("name", fallback);
        } catch (Exception ignored) { return fallback; }
    }

    private void updateStrobe() {
        if (currentScreen != SCREEN_MINDEXTRA || strobe == null || !strobe.isChecked()
            || !audioService.engine().isGeneratorActive() || !audioService.engine().isBinaural()
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
        boolean forceScreenOff = prefs().getBoolean("energy_screen_off", false);
        boolean keepCurrentScreenOn = currentScreen == SCREEN_METRONOME
            ? keepMetro
            : (currentScreen == SCREEN_BIOSTIM || currentScreen == SCREEN_MINDEXTRA)
                && keepGenerator;
        if (!forceScreenOff && keepCurrentScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
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
        b.setTypeface(Typeface.DEFAULT_BOLD);
        UiStyle.compactButton(b, this);
        b.setLayoutParams(UiStyle.centeredButton(this));
        int color;
        String upper = value.toUpperCase(Locale.ROOT);
        if (upper.contains("START") || upper.contains("REIA")) color = Color.rgb(28, 135, 82);
        else if (upper.contains("PAUZ")) color = Color.rgb(190, 123, 22);
        else if (upper.contains("STOP") || upper.contains("RESET")) color = Color.rgb(174, 55, 62);
        else if (upper.contains("SALVE")) color = Color.rgb(116, 70, 178);
        else if (upper.contains("ÎNCARC") || upper.contains("IMPORT") || upper.contains("GRAFIC") || upper.contains("ALEGE"))
            color = Color.rgb(33, 112, 165);
        else color = Color.rgb(58, 75, 91);
        applyButtonSurface(b, color);
        return b;
    }

    private Button tabButton(String value, boolean active) {
        Button b = button(value);
        b.setPadding(dp(2), 0, dp(2), 0);
        applyButtonSurface(b, active ? ACCENT : Color.rgb(52, 57, 61));
        b.setTextColor(active ? Color.BLACK : Color.WHITE);
        return b;
    }

    private void applyButtonSurface(Button button, int color) {
        GradientDrawable normal = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{lighten(color, 0.18f), color, darken(color, 0.18f)});
        normal.setCornerRadius(dp(8));
        normal.setStroke(dp(1), lighten(color, 0.32f));
        GradientDrawable pressed = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{darken(color, 0.24f), darken(color, 0.08f)});
        pressed.setCornerRadius(dp(8));
        pressed.setStroke(dp(1), darken(color, 0.35f));
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, normal);
        button.setBackground(states);
        button.setElevation(dp(3));
    }

    private static int lighten(int color, float amount) {
        return Color.rgb(
            Math.min(255, Math.round(Color.red(color) + (255 - Color.red(color)) * amount)),
            Math.min(255, Math.round(Color.green(color) + (255 - Color.green(color)) * amount)),
            Math.min(255, Math.round(Color.blue(color) + (255 - Color.blue(color)) * amount)));
    }

    private static int darken(int color, float amount) {
        return Color.rgb(Math.round(Color.red(color) * (1 - amount)),
            Math.round(Color.green(color) * (1 - amount)),
            Math.round(Color.blue(color) * (1 - amount)));
    }

    private CheckBox check(String value, boolean checked) {
        CheckBox b = new CheckBox(this);
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
    private LinearLayout.LayoutParams weightedButton() { return UiStyle.weightedButton(this); }
    private LinearLayout.LayoutParams squareTabParams(int size) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }
    private FrameLayout.LayoutParams match() { return new FrameLayout.LayoutParams(-1, -1); }
    private LinearLayout.LayoutParams matchWidth() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private double parseDouble(EditText e, double fallback) {
        try { return Double.parseDouble(e.getText().toString().replace(',', '.')); }
        catch (Exception ignored) { return fallback; }
    }
    private String readUriText(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        if (input == null) throw new Exception("Fișierul nu poate fi citit");
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                result.append(buffer, 0, read);
                if (result.length() > 1_000_000) throw new Exception("Fișier prea mare");
            }
            return result.toString();
        }
    }
    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
            new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String value = cursor.getString(0);
                if (value != null && !value.trim().isEmpty()) return value;
            }
        } catch (Exception ignored) { }
        String fallback = uri.getLastPathSegment();
        return fallback == null || fallback.trim().isEmpty() ? "vector.csv" : fallback;
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
