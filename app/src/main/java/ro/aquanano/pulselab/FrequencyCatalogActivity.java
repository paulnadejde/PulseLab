package ro.aquanano.pulselab;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import ro.aquanano.pulselab.core.FrequencyPreset;

/** Online selector for small monoaural frequency presets. */
public final class FrequencyCatalogActivity extends Activity {
    public static final String RESULT_CSV = "frequency_preset_csv";
    public static final String RESULT_NAME = "frequency_preset_name";
    private static final String CATALOG =
        "https://aquanano.eu/aquaweb/aquaritm/catalog_aquaritm.php";

    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(14));
        page.setBackgroundColor(Color.BLACK);

        TextView title = text("Preseturi de frecvențe", 24);
        title.setTextColor(Color.rgb(69, 214, 196));
        page.addView(title);
        page.addView(text(
            "Presetul ales rămâne memorat și poate fi activat din Generator.", 15));

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(text("Încarc lista…", 17));
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button close = button("ÎNCHIDE");
        close.setOnClickListener(v -> finish());
        page.addView(close, UiStyle.centeredButton(this));
        setContentView(page);
        loadCatalog();
    }

    private void loadCatalog() {
        new Thread(() -> {
            try {
                URL catalogUrl = new URL(CATALOG);
                JSONObject root = new JSONObject(PresetStore.fetchCatalog(catalogUrl));
                JSONArray frequencies = root.optJSONArray("frequencies");
                if (frequencies == null) frequencies = new JSONArray();
                JSONArray result = frequencies;
                runOnUiThread(() -> showCatalog(result, catalogUrl));
            } catch (Exception error) {
                runOnUiThread(() ->
                    showError("Catalog indisponibil: " + error.getMessage(), true));
            }
        }, "AquaRitmFrequencyCatalog").start();
    }

    private void showCatalog(JSONArray items, URL catalogUrl) {
        list.removeAllViews();
        if (items.length() == 0) {
            list.addView(text("Niciun preset de frecvențe disponibil.", 15));
            return;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(8), dp(10), dp(12));
            card.addView(text(item.optString("name", "Preset"), 18));
            Button load = button("ÎNCARCĂ");
            load.setOnClickListener(v -> download(item, catalogUrl, load));
            card.addView(load, UiStyle.centeredButton(this));
            list.addView(card);
        }
    }

    private void download(JSONObject item, URL catalogUrl, Button button) {
        button.setEnabled(false);
        button.setText("DESCARC…");
        new Thread(() -> {
            try {
                URL url = new URL(catalogUrl, item.optString("frequency", ""));
                String csv = PresetStore.fetchCatalog(url);
                FrequencyPreset.parseCsv(new StringReader(csv));
                String expected = item.optString("frequency_sha256", "");
                if (!expected.isEmpty() && !expected.equalsIgnoreCase(sha256(csv))) {
                    throw new Exception("Checksum incorect");
                }
                String name = item.optString("name", "Preset de frecvențe");
                runOnUiThread(() -> {
                    Intent result = new Intent();
                    result.putExtra(RESULT_CSV, csv);
                    result.putExtra(RESULT_NAME, name);
                    setResult(RESULT_OK, result);
                    finish();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    button.setText("REÎNCEARCĂ");
                    showError("Încărcare eșuată: " + error.getMessage(), false);
                });
            }
        }, "AquaRitmFrequencyDownload").start();
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(64);
        for (byte item : digest) result.append(String.format("%02x", item & 0xff));
        return result.toString();
    }

    private void showError(String value, boolean clear) {
        if (clear) list.removeAllViews();
        TextView error = text(value, 15);
        error.setTextColor(Color.rgb(255, 105, 105));
        list.addView(error, 0);
    }

    private TextView text(String value, int sp) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(sp);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(5), dp(5), dp(5), dp(5));
        return view;
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        UiStyle.compactButton(button, this);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
