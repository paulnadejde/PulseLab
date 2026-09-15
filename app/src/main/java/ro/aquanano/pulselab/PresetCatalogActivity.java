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

/** Independent online lists for vectors and audio; downloads remain local. */
public final class PresetCatalogActivity extends Activity {
    public static final String RESULT_PRESET_ID = "downloaded_preset_id";
    public static final String RESULT_RESOURCE_TYPE = "downloaded_resource_type";
    public static final String TYPE_VECTOR = "vector";
    public static final String TYPE_AUDIO = "audio";
    private static final String CATALOG =
        "https://aquanano.eu/aquaweb/aquaritm/catalog_aquaritm.php";
    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(14));
        page.setBackgroundColor(Color.BLACK);
        TextView title = text("Biblioteca AquaRitm", 24);
        title.setTextColor(Color.rgb(69, 214, 196));
        page.addView(title);
        page.addView(text("Vectorii și sunetele se descarcă separat și rămân disponibile offline.", 15));
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
                JSONArray vectors = root.optJSONArray("vectors");
                JSONArray audio = root.optJSONArray("audio");
                if (vectors == null) vectors = new JSONArray();
                if (audio == null) audio = new JSONArray();
                JSONArray finalVectors = vectors;
                JSONArray finalAudio = audio;
                runOnUiThread(() -> showCatalog(finalVectors, finalAudio, catalogUrl));
            } catch (Exception e) {
                runOnUiThread(() -> showError("Catalog indisponibil: " + e.getMessage(), true));
            }
        }, "AquaRitmCatalog").start();
    }

    private void showCatalog(JSONArray vectors, JSONArray audio, URL catalogUrl) {
        list.removeAllViews();
        addSection("VECTORI", vectors, TYPE_VECTOR, catalogUrl);
        addSection("SUNETE", audio, TYPE_AUDIO, catalogUrl);
    }

    private void addSection(String title, JSONArray items, String type, URL catalogUrl) {
        TextView heading = text(title, 20);
        heading.setTextColor(Color.rgb(69, 214, 196));
        list.addView(heading);
        if (items.length() == 0) {
            list.addView(text("Niciun fișier disponibil.", 15));
            return;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(8), dp(10), dp(12));
            card.addView(text(item.optString("name", "Fișier"), 18));
            String description = item.optString("description", "");
            if (!description.isEmpty()) card.addView(text(description, 14));
            boolean installed = TYPE_VECTOR.equals(type)
                ? PresetStore.find(this, item.optString("id")) != null
                : PresetStore.findAudio(this, item.optString("id")) != null;
            Button download = button(installed ? "ACTUALIZEAZĂ" : "DESCARCĂ");
            download.setOnClickListener(v -> download(item, type, catalogUrl, download));
            card.addView(download, UiStyle.centeredButton(this));
            list.addView(card);
        }
    }

    private void download(JSONObject item, String type, URL catalogUrl, Button button) {
        button.setEnabled(false);
        button.setText("DESCARC…");
        new Thread(() -> {
            try {
                String id;
                if (TYPE_AUDIO.equals(type))
                    id = PresetStore.downloadAudio(this, item, catalogUrl).id;
                else
                    id = PresetStore.download(this, item, catalogUrl).id;
                String resultId = id;
                runOnUiThread(() -> {
                    Intent result = new Intent();
                    result.putExtra(RESULT_PRESET_ID, resultId);
                    result.putExtra(RESULT_RESOURCE_TYPE, type);
                    setResult(RESULT_OK, result);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    button.setText("REÎNCEARCĂ");
                    showError("Descărcare eșuată: " + e.getMessage(), false);
                });
            }
        }, "AquaRitmDownload").start();
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
