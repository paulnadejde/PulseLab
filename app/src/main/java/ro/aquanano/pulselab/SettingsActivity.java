package ro.aquanano.pulselab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends Activity {
    private static final int ACCENT = Color.rgb(69, 214, 196);
    private static final int PANEL = Color.rgb(25, 25, 25);
    private static final int BLUE = Color.rgb(33, 112, 165);
    private static final int RED = Color.rgb(174, 55, 62);

    private SharedPreferences preferences;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("presets", MODE_PRIVATE);

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
        Button update = button("CAUTĂ ACTUALIZĂRI", Color.rgb(74, 74, 74));
        page.addView(update, fullButton());
        update.setOnClickListener(v -> placeholder("Update"));

        section(page, "Help");
        Button help = button("DESCHIDE AJUTORUL", Color.rgb(74, 74, 74));
        page.addView(help, fullButton());
        help.setOnClickListener(v -> placeholder("Help"));

        section(page, "Aplicație");
        Button close = button("ÎNCHIDE APLICAȚIA", RED);
        page.addView(close, fullButton());
        close.setOnClickListener(v -> confirmClose());
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
