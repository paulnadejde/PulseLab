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

import java.net.URL;

/** Online catalog; downloaded content remains available through PresetStore. */
public final class PresetCatalogActivity extends Activity {
    public static final String RESULT_PRESET_ID = "downloaded_preset_id";
    private static final String CATALOG = "https://aquanano.eu/aquaweb/aquaritm/catalog.json";
    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(14));
        page.setBackgroundColor(Color.BLACK);
        TextView title = text("Catalog AquaRitm", 24);
        title.setTextColor(Color.rgb(69, 214, 196));
        page.addView(title);
        TextView note = text("Preseturile descărcate rămân disponibile offline.", 15);
        page.addView(note);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(text("Încarc lista…", 17));
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button close = new Button(this);
        close.setText("ÎNCHIDE");
        close.setOnClickListener(v -> finish());
        page.addView(close);
        setContentView(page);
        loadCatalog();
    }

    private void loadCatalog() {
        new Thread(() -> {
            try {
                URL catalogUrl = new URL(CATALOG);
                JSONObject root = new JSONObject(PresetStore.fetchCatalog(catalogUrl));
                JSONArray presets = root.getJSONArray("presets");
                runOnUiThread(() -> showCatalog(presets, catalogUrl));
            } catch (Exception e) {
                runOnUiThread(() -> showError("Catalog indisponibil: " + e.getMessage()));
            }
        }, "AquaRitmCatalog").start();
    }

    private void showCatalog(JSONArray presets, URL catalogUrl) {
        list.removeAllViews();
        if (presets.length() == 0) { list.addView(text("Catalogul este gol.", 17)); return; }
        for (int i = 0; i < presets.length(); i++) {
            JSONObject item = presets.optJSONObject(i);
            if (item == null) continue;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(12), dp(10), dp(12));
            TextView name = text(item.optString("name", "Preset"), 20);
            name.setTextColor(Color.rgb(69, 214, 196));
            card.addView(name);
            String description = item.optString("description", "");
            if (!description.isEmpty()) card.addView(text(description, 14));
            Button download = new Button(this);
            boolean installed = PresetStore.find(this, item.optString("id")) != null;
            download.setText(installed ? "ACTUALIZEAZĂ" : "DESCARCĂ");
            download.setOnClickListener(v -> download(item, catalogUrl, download));
            card.addView(download);
            list.addView(card);
        }
    }

    private void download(JSONObject item, URL catalogUrl, Button button) {
        button.setEnabled(false);
        button.setText("DESCARC…");
        new Thread(() -> {
            try {
                PresetStore.LocalPreset preset = PresetStore.download(this, item, catalogUrl);
                runOnUiThread(() -> {
                    Intent result = new Intent();
                    result.putExtra(RESULT_PRESET_ID, preset.id);
                    setResult(RESULT_OK, result);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    button.setText("REÎNCEARCĂ");
                    showError("Descărcare eșuată: " + e.getMessage());
                });
            }
        }, "AquaRitmDownload").start();
    }

    private void showError(String value) {
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

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
