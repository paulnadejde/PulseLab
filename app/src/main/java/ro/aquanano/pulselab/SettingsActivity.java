package ro.aquanano.pulselab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

import ro.aquanano.pulselab.core.VersionLogic;

public final class SettingsActivity extends Activity {
    private static final int ACCENT = Color.rgb(69, 214, 196);
    private static final int PANEL = Color.rgb(25, 25, 25);
    private static final int BLUE = Color.rgb(33, 112, 165);
    private static final int RED = Color.rgb(174, 55, 62);
    private static final String UPDATE_CATALOG =
        "https://aquanano.eu/aquaweb/aquaritm/aplicatie/catalog_aplicatie.php";
    private static final String PENDING_DOWNLOAD_ID = "update_download_id";
    private static final String PENDING_DOWNLOAD_HASH = "update_download_hash";
    private static final String PENDING_DOWNLOAD_VERSION = "update_download_version";

    private SharedPreferences preferences;
    private Button updateButton;
    private TextView updateStatus;
    private boolean receiverRegistered;
    private boolean verifyingUpdate;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
            long completed = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            if (completed == preferences.getLong(PENDING_DOWNLOAD_ID, -1L)) {
                checkPendingDownload(true);
            }
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("presets", MODE_PRIVATE);
        IntentFilter downloadFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(downloadReceiver, downloadFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, downloadFilter);
        }
        receiverRegistered = true;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        LinearLayout header = row();
        TextView title = text("Setări AquaRitm", 24);
        title.setTextColor(ACCENT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1));
        Button back = button("ÎNAPOI", BLUE);
        header.addView(back, new LinearLayout.LayoutParams(dp(110), dp(52)));
        page.addView(header);
        back.setOnClickListener(v -> finish());

        note(page, "Alege numai economiile care îți sunt utile. Generatorul audio continuă să funcționeze când ecranul este stins.");

        section(page, "Management consum energetic");

        CheckBox screenOff = check("Permite stingerea ecranului chiar dacă este bifat „Menține ecranul aprins”",
            preferences.getBoolean("energy_screen_off", false));
        CheckBox slowUi = check("Actualizează interfața mai rar (o dată pe secundă)",
            preferences.getBoolean("energy_slow_ui", false));
        CheckBox disableStrobe = check("Dezactivează complet stroboscopul",
            preferences.getBoolean("energy_disable_strobe", false));
        page.addView(screenOff);
        page.addView(slowUi);
        page.addView(disableStrobe);

        screenOff.setOnCheckedChangeListener((v, checked) ->
            preferences.edit().putBoolean("energy_screen_off", checked).apply());
        slowUi.setOnCheckedChangeListener((v, checked) ->
            preferences.edit().putBoolean("energy_slow_ui", checked).apply());
        disableStrobe.setOnCheckedChangeListener((v, checked) ->
            preferences.edit().putBoolean("energy_disable_strobe", checked).apply());

        note(page, "Pentru radio, rețea, localizare și luminozitate, Android cere confirmarea ta în panoul sistemului.");

        addSystemButton(page, "MOD AVION", Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        addSystemButton(page, "REȚELE, WI-FI ȘI DATE MOBILE", Settings.ACTION_WIRELESS_SETTINGS);
        addSystemButton(page, "BLUETOOTH", Settings.ACTION_BLUETOOTH_SETTINGS);
        addSystemButton(page, "LOCAȚIE", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        addSystemButton(page, "ECRAN ȘI LUMINOZITATE", Settings.ACTION_DISPLAY_SETTINGS);
        addSystemButton(page, "ECONOMISIRE BATERIE", Settings.ACTION_BATTERY_SAVER_SETTINGS);

        section(page, "Update");
        updateStatus = text("Versiunea instalată: " + installedVersion(), 14);
        updateStatus.setTextColor(Color.rgb(200, 200, 200));
        page.addView(updateStatus);
        updateButton = button("CAUTĂ ACTUALIZĂRI", BLUE);
        page.addView(updateButton, fullButton());
        updateButton.setOnClickListener(v -> {
            if (preferences.getLong(PENDING_DOWNLOAD_ID, -1L) >= 0L) {
                checkPendingDownload(true);
            } else {
                checkForUpdates();
            }
        });

        section(page, "Help");
        Button help = button("DESCHIDE AJUTORUL", Color.rgb(74, 74, 74));
        page.addView(help, fullButton());
        help.setOnClickListener(v -> placeholder("Help"));

        section(page, "Aplicație");
        Button close = button("ÎNCHIDE APLICAȚIA", RED);
        page.addView(close, fullButton());
        close.setOnClickListener(v -> confirmClose());
    }

    @Override protected void onResume() {
        super.onResume();
        if (preferences != null && preferences.getLong(PENDING_DOWNLOAD_ID, -1L) >= 0L) {
            checkPendingDownload(false);
        }
    }

    @Override protected void onDestroy() {
        if (receiverRegistered) unregisterReceiver(downloadReceiver);
        super.onDestroy();
    }

    private void addSystemButton(LinearLayout parent, String label, String action) {
        Button button = button(label, BLUE);
        parent.addView(button, fullButton());
        button.setOnClickListener(v -> openSystemSettings(action));
    }

    private void openSystemSettings(String action) {
        try {
            startActivity(new Intent(action));
        } catch (RuntimeException unavailable) {
            Toast.makeText(this, "Această setare nu este disponibilă pe telefon.", Toast.LENGTH_LONG).show();
        }
    }

    private void checkForUpdates() {
        updateButton.setEnabled(false);
        updateButton.setText("VERIFIC…");
        updateStatus.setText("Citesc catalogul AquaRitm…");
        new Thread(() -> {
            try {
                JSONObject catalog = new JSONObject(
                    PresetStore.fetchCatalog(new URL(UPDATE_CATALOG)));
                JSONObject latest = catalog.optJSONObject("latest");
                if (latest == null) throw new Exception("Catalogul nu conține niciun APK");

                String version = latest.optString("version", "").trim();
                String apk = latest.optString("apk", "").trim();
                String hash = latest.optString("sha256", "").trim().toLowerCase(Locale.ROOT);
                long size = latest.optLong("size_bytes", 0L);
                validateUpdate(version, apk, hash);

                runOnUiThread(() -> showUpdateResult(version, apk, hash, size));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    updateStatus.setText("Nu am putut verifica actualizările.");
                    updateButton.setEnabled(true);
                    updateButton.setText("REÎNCEARCĂ");
                    new AlertDialog.Builder(this)
                        .setTitle("Update indisponibil")
                        .setMessage(error.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        }, "AquaRitmUpdateCheck").start();
    }

    private void validateUpdate(String version, String apk, String hash) throws Exception {
        if (!VersionLogic.isValid(version)) throw new Exception("Versiune invalidă în catalog");
        Uri uri = Uri.parse(apk);
        if (!"https".equalsIgnoreCase(uri.getScheme())
            || !"aquanano.eu".equalsIgnoreCase(uri.getHost())) {
            throw new Exception("Adresă APK neacceptată");
        }
        if (!hash.matches("[0-9a-f]{64}")) throw new Exception("Checksum APK invalid");
    }

    private void showUpdateResult(String version, String apk, String hash, long size) {
        updateButton.setEnabled(true);
        updateButton.setText("CAUTĂ ACTUALIZĂRI");
        String currentVersion = installedVersion();
        if (!VersionLogic.isNewer(version, currentVersion)) {
            updateStatus.setText("AquaRitm " + currentVersion + " este versiunea curentă.");
            new AlertDialog.Builder(this)
                .setTitle("AquaRitm este actualizat")
                .setMessage("Versiunea instalată: " + currentVersion
                    + "\nVersiunea din catalog: " + version)
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        String sizeText = size > 0L ? "\nDimensiune: " + humanSize(size) : "";
        updateStatus.setText("Este disponibil AquaRitm " + version + ".");
        new AlertDialog.Builder(this)
            .setTitle("Actualizare disponibilă")
            .setMessage("Instalat: " + currentVersion
                + "\nDisponibil: " + version + sizeText
                + "\n\nAPK-ul va fi verificat înainte de instalare.")
            .setNegativeButton("MAI TÂRZIU", null)
            .setPositiveButton("DESCARCĂ", (dialog, which) ->
                startUpdateDownload(version, apk, hash))
            .show();
    }

    private void startUpdateDownload(String version, String apk, String hash) {
        try {
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            String filename = "AquaRitm-" + version + "-release.apk";
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apk))
                .setTitle("AquaRitm " + version)
                .setDescription("Actualizare AquaRitm")
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, filename);
            long id = manager.enqueue(request);
            preferences.edit()
                .putLong(PENDING_DOWNLOAD_ID, id)
                .putString(PENDING_DOWNLOAD_HASH, hash)
                .putString(PENDING_DOWNLOAD_VERSION, version)
                .apply();
            updateStatus.setText("Descarc AquaRitm " + version + "…");
            updateButton.setText("VERIFICĂ DESCĂRCAREA");
        } catch (RuntimeException error) {
            updateStatus.setText("Descărcarea nu a putut porni.");
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkPendingDownload(boolean userInitiated) {
        if (verifyingUpdate || updateStatus == null || updateButton == null) return;
        long id = preferences.getLong(PENDING_DOWNLOAD_ID, -1L);
        if (id < 0L) return;

        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        try (Cursor cursor = manager.query(
            new DownloadManager.Query().setFilterById(id))) {
            if (cursor == null || !cursor.moveToFirst()) {
                clearPendingUpdate();
                updateStatus.setText("Descărcarea nu mai este disponibilă.");
                updateButton.setText("CAUTĂ ACTUALIZĂRI");
                return;
            }
            int status = cursor.getInt(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                verifyDownloadedUpdate(id, userInitiated);
            } else if (status == DownloadManager.STATUS_FAILED) {
                int reason = cursor.getInt(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                clearPendingUpdate();
                updateStatus.setText("Descărcare eșuată (cod " + reason + ").");
                updateButton.setText("REÎNCEARCĂ");
            } else {
                updateStatus.setText("Actualizarea se descarcă în fundal…");
                updateButton.setText("VERIFICĂ DESCĂRCAREA");
            }
        } catch (RuntimeException error) {
            updateStatus.setText("Nu pot verifica descărcarea.");
            if (userInitiated) {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void verifyDownloadedUpdate(long id, boolean userInitiated) {
        if (verifyingUpdate) return;
        verifyingUpdate = true;
        updateButton.setEnabled(false);
        updateButton.setText("VERIFIC SHA-256…");
        String expected = preferences.getString(PENDING_DOWNLOAD_HASH, "");
        String version = preferences.getString(PENDING_DOWNLOAD_VERSION, "");
        new Thread(() -> {
            try {
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Uri uri = manager.getUriForDownloadedFile(id);
                if (uri == null) throw new Exception("Fișierul descărcat nu poate fi deschis");
                String actual = sha256(uri);
                if (!actual.equalsIgnoreCase(expected)) {
                    manager.remove(id);
                    throw new Exception("Checksum incorect; APK-ul a fost șters");
                }
                runOnUiThread(() -> {
                    verifyingUpdate = false;
                    continueInstallation(id, uri, version, userInitiated);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    verifyingUpdate = false;
                    clearPendingUpdate();
                    updateButton.setEnabled(true);
                    updateButton.setText("REÎNCEARCĂ");
                    updateStatus.setText("Verificarea APK-ului a eșuat.");
                    new AlertDialog.Builder(this)
                        .setTitle("Actualizare respinsă")
                        .setMessage(error.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        }, "AquaRitmUpdateVerify").start();
    }

    private void continueInstallation(long id, Uri apkUri, String version,
                                      boolean userInitiated) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && !getPackageManager().canRequestPackageInstalls()) {
            updateStatus.setText("APK verificat. Permite instalarea pentru AquaRitm.");
            updateButton.setEnabled(true);
            updateButton.setText("PERMITE INSTALAREA");
            if (userInitiated) {
                Intent permission = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
                startActivity(permission);
            }
            return;
        }

        clearPendingUpdate();
        updateButton.setEnabled(true);
        updateButton.setText("CAUTĂ ACTUALIZĂRI");
        updateStatus.setText("AquaRitm " + version + " este pregătit pentru instalare.");
        Intent install = new Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(install);
        } catch (RuntimeException error) {
            updateStatus.setText("Instalatorul Android nu poate fi deschis.");
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String sha256(Uri uri) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new Exception("Fișierul APK nu poate fi citit");
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        StringBuilder result = new StringBuilder(64);
        for (byte value : digest.digest()) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }

    private void clearPendingUpdate() {
        preferences.edit()
            .remove(PENDING_DOWNLOAD_ID)
            .remove(PENDING_DOWNLOAD_HASH)
            .remove(PENDING_DOWNLOAD_VERSION)
            .apply();
    }

    private String installedVersion() {
        try {
            String version = getPackageManager()
                .getPackageInfo(getPackageName(), 0).versionName;
            return version == null ? "0.0.0" : version;
        } catch (Exception unavailable) {
            return "0.0.0";
        }
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024L * 1024L) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void placeholder(String name) {
        Toast.makeText(this, "Secțiunea " + name + " va fi adăugată într-o versiune viitoare.",
            Toast.LENGTH_LONG).show();
    }

    private void confirmClose() {
        new AlertDialog.Builder(this)
            .setTitle("Închide AquaRitm")
            .setMessage("Oprești toate sunetele și închizi aplicația?")
            .setNegativeButton("ANULEAZĂ", null)
            .setPositiveButton("ÎNCHIDE", (dialog, which) -> {
                stopService(new Intent(this, AudioService.class));
                finishAffinity();
            })
            .show();
    }

    private void section(LinearLayout parent, String value) {
        TextView title = text(value, 19);
        title.setTextColor(ACCENT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(6), dp(22), dp(6), dp(7));
        parent.addView(title);
    }

    private void note(LinearLayout parent, String value) {
        TextView note = text(value, 14);
        note.setTextColor(Color.rgb(200, 200, 200));
        note.setBackground(surface(PANEL, dp(10)));
        note.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(5), 0, dp(8));
        parent.addView(note, params);
    }

    private CheckBox check(String value, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(value);
        box.setTextColor(Color.WHITE);
        box.setTextSize(16);
        box.setChecked(checked);
        box.setPadding(dp(7), dp(7), dp(7), dp(7));
        return box;
    }

    private Button button(String value, int color) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(surface(color, dp(10)));
        button.setPadding(dp(8), dp(4), dp(8), dp(4));
        return button;
    }

    private TextView text(String value, int sp) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(sp);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setPadding(dp(6), dp(5), dp(6), dp(5));
        return text;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private LinearLayout.LayoutParams fullButton() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
        params.setMargins(0, dp(4), 0, dp(4));
        return params;
    }

    private GradientDrawable surface(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.argb(90, 255, 255, 255));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
