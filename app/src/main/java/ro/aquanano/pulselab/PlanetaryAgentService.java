package ro.aquanano.pulselab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import ro.aquanano.pulselab.core.PlanetaryHours;

/** Foreground notification refreshed at each planetary-hour boundary. */
public final class PlanetaryAgentService extends Service {
    static final String PREF_ENABLED = "planetary_agent_notification";
    private static final String CHANNEL_ID = "aquaritm_planetary_agent";
    private static final int NOTIFICATION_ID = 1824;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences preferences;
    private final Runnable updater = this::update;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> {
        if (key != null && (key.startsWith("solar_") || PREF_ENABLED.equals(key))) {
            handler.removeCallbacks(updater);
            handler.post(updater);
        }
    };

    static void sync(Context context) {
        boolean enabled = context.getSharedPreferences("presets", MODE_PRIVATE)
            .getBoolean(PREF_ENABLED, false);
        Intent intent = new Intent(context, PlanetaryAgentService.class);
        if (enabled) context.startForegroundService(intent);
        else context.stopService(intent);
    }

    @Override public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences("presets", MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(listener);
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
            "Agent AstraRitm", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Guvernatorul orei planetare curente.");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
        startForeground(NOTIFICATION_ID, build(null));
        handler.post(updater);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!preferences.getBoolean(PREF_ENABLED, false)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        handler.removeCallbacks(updater);
        handler.post(updater);
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        handler.removeCallbacks(updater);
        if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(listener);
        super.onDestroy();
    }

    private void update() {
        if (!preferences.getBoolean(PREF_ENABLED, false)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return;
        }
        PlanetaryHours.Hour hour = null;
        long delay = 60_000L;
        try {
            String prefix = preferences.getBoolean("solar_use_phone_location", true)
                ? "solar_phone_" : "solar_manual_";
            double lat = Double.parseDouble(preferences.getString(prefix + "latitude", ""));
            double lon = Double.parseDouble(preferences.getString(prefix + "longitude", ""));
            Instant now = Instant.now();
            PlanetaryHours.Day day = PlanetaryHours.forMoment(now, ZoneId.systemDefault(), lat, lon);
            if (day != null) {
                hour = day.at(now);
                if (hour != null)
                    delay = Math.max(1_000L, Duration.between(now, hour.end).toMillis() + 250L);
            }
        } catch (IllegalArgumentException ignored) {
            // Wait for a valid phone location or manually entered coordinates.
        }
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, build(hour));
        handler.postDelayed(updater, delay);
    }

    private Notification build(PlanetaryHours.Hour hour) {
        String symbol = hour == null ? "✦" : hour.planet.symbol;
        String title = hour == null ? "AstraRitm • așteaptă locația"
            : "AstraRitm • " + hour.planet.name;
        String detail = hour == null ? "Deschide AstraRitm pentru locație"
            : "Ora planetară " + hour.number + " din 24";
        Intent open = new Intent(this, MainActivity.class)
            .putExtra("open_astraritm", true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, 1824, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(Icon.createWithBitmap(statusIcon(symbol)))
            .setContentTitle(title)
            .setContentText(detail)
            .setContentIntent(pending)
            .setColor(0xff83d7ce)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .build();
    }

    private Bitmap statusIcon(String symbol) {
        int size = Math.round(48 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size * .78f);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        canvas.drawText(symbol, size / 2f,
            size / 2f - (metrics.ascent + metrics.descent) / 2f, paint);
        return bitmap;
    }
}
