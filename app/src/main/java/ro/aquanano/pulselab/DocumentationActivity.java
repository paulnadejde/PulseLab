package ro.aquanano.pulselab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.Locale;

/** Dynamic end-user documentation catalog hosted by AquaNano. */
public final class DocumentationActivity extends Activity {
    private static final int ACCENT = Color.rgb(69, 214, 196);
    private static final int BLUE = Color.rgb(33, 112, 165);
    private static final int PANEL = Color.rgb(25, 25, 25);
    private static final String CATALOG =
        "https://aquanano.eu/aquaweb/aquaritm/documentatie/catalog_documentatie.php";

    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(14));
        page.setBackgroundColor(Color.BLACK);

        TextView title = text("Ajutor AquaRitm", 24);
        title.setTextColor(ACCENT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(title);
        page.addView(text(
            "Alege modulul dorit. Documentul se deschide prin aplicația PDF sau browserul instalat pe telefon.",
            15));

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(text("Încarc documentația…", 17));
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button close = button("ÎNCHIDE", Color.rgb(74, 74, 74));
        close.setOnClickListener(v -> finish());
        page.addView(close, fullButton());
        setContentView(page);

        loadCatalog();
    }

    private void loadCatalog() {
        list.removeAllViews();
        list.addView(text("Încarc documentația…", 17));
        new Thread(() -> {
            try {
                URL catalogUrl = new URL(CATALOG);
                JSONObject root = new JSONObject(PresetStore.fetchCatalog(catalogUrl));
                JSONArray documents = root.optJSONArray("documents");
                if (documents == null) documents = new JSONArray();
                JSONArray result = documents;
                runOnUiThread(() -> showCatalog(result, catalogUrl));
            } catch (Exception error) {
                runOnUiThread(() -> showError(
                    "Catalogul documentației nu poate fi citit: " + error.getMessage()));
            }
        }, "AquaRitmDocumentationCatalog").start();
    }

    private void showCatalog(JSONArray documents, URL catalogUrl) {
        list.removeAllViews();
        if (documents.length() == 0) {
            list.addView(text("Nu există documente disponibile momentan.", 15));
            return;
        }

        for (int i = 0; i < documents.length(); i++) {
            JSONObject document = documents.optJSONObject(i);
            if (document == null) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackground(surface(PANEL, dp(10)));

            String title = document.optString("title", "Documentație");
            TextView name = text(title, 18);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(name);

            String format = document.optString("format", "").toUpperCase(Locale.ROOT);
            long size = document.optLong("size_bytes", 0L);
            String details = format;
            if (size > 0L) details += (details.isEmpty() ? "" : " • ") + humanSize(size);
            if (!details.isEmpty()) {
                TextView info = text(details, 13);
                info.setTextColor(Color.rgb(190, 190, 190));
                card.addView(info);
            }

            Button open = button("DESCHIDE", BLUE);
            open.setOnClickListener(v -> openDocument(document, catalogUrl));
            card.addView(open, fullButton());

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.setMargins(0, dp(7), 0, dp(7));
            list.addView(card, cardParams);
        }
    }

    private void openDocument(JSONObject document, URL catalogUrl) {
        try {
            String raw = document.optString("document", "").trim();
            if (raw.isEmpty()) throw new Exception("Adresa documentului lipsește");
            URL resolved = new URL(catalogUrl, raw);
            Uri uri = Uri.parse(resolved.toString());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"aquanano.eu".equalsIgnoreCase(uri.getHost())) {
                throw new Exception("Adresa documentului nu este acceptată");
            }
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception error) {
            new AlertDialog.Builder(this)
                .setTitle("Documentul nu poate fi deschis")
                .setMessage(error.getMessage())
                .setPositiveButton("OK", null)
                .show();
        }
    }

    private void showError(String message) {
        list.removeAllViews();
        TextView error = text(message, 15);
        error.setTextColor(Color.rgb(255, 105, 105));
        list.addView(error);
        Button retry = button("REÎNCEARCĂ", BLUE);
        retry.setOnClickListener(v -> loadCatalog());
        list.addView(retry, fullButton());
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024L * 1024L)
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private TextView text(String value, int sp) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(sp);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(6), dp(6), dp(6), dp(6));
        return view;
    }

    private Button button(String value, int color) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        UiStyle.compactButton(button, this);
        button.setBackground(surface(color, dp(10)));
        return button;
    }

    private LinearLayout.LayoutParams fullButton() {
        return UiStyle.centeredButton(this);
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
