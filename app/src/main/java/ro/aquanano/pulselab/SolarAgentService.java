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
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.RemoteViews;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import ro.aquanano.pulselab.core.SolarCalculator;
import ro.aquanano.pulselab.core.SolarCycle;

/** Optional foreground agent that keeps the two SolaRitm glyphs in notifications. */
public final class SolarAgentService extends Service {
    static final String PREF_ENABLED = "solar_agent_enabled";
    private static final String CHANNEL_ID = "aquaritm_solar_agent";
    private static final int NOTIFICATION_ID = 1805;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences preferences;
    private final Runnable updater = new Runnable() {
        @Override public void run() { updateNotificationAndSchedule(); }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
        (shared, key) -> {
            if (key != null && key.startsWith("solar_")) {
                handler.removeCallbacks(updater);
                handler.post(updater);
            }
        };

    static void sync(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("presets", MODE_PRIVATE);
        boolean enabled = preferences.getBoolean(PREF_ENABLED, false)
            && SolarAccess.isGranted(preferences);
        Intent service = new Intent(context, SolarAgentService.class);
        if (enabled) context.startForegroundService(service);
        else context.stopService(service);
    }

    @Override public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences("presets", MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
        if (!SolarAccess.isGranted(preferences)) {
            stopSelf();
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
            "Agent SolaRitm", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Afișează cele două simboluri temporale SolaRitm.");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
        startForeground(NOTIFICATION_ID, buildNotification(5, 5, false));
        handler.post(updater);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!preferences.getBoolean(PREF_ENABLED, false)
                || !SolarAccess.isGranted(preferences)) {
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
        if (preferences != null)
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        super.onDestroy();
    }

    private void updateNotificationAndSchedule() {
        if (!preferences.getBoolean(PREF_ENABLED, false)
                || !SolarAccess.isGranted(preferences)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return;
        }
        Reading reading = readCycle(preferences);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, buildNotification(
            reading.large, reading.small, reading.hasCoordinates));
        handler.postDelayed(updater, reading.nextUpdateMillis);
    }

    private Reading readCycle(SharedPreferences preferences) {
        try {
            boolean phone = preferences.getBoolean("solar_use_phone_location", true);
            String prefix = phone ? "solar_phone_" : "solar_manual_";
            String latitudeText = preferences.getString(prefix + "latitude", "");
            String longitudeText = preferences.getString(prefix + "longitude", "");
            if (latitudeText.isEmpty() || longitudeText.isEmpty()) return Reading.unavailable();
            double latitude = Double.parseDouble(latitudeText);
            double longitude = Double.parseDouble(longitudeText);
            boolean includeSunset = preferences.getBoolean("solar_cycle_uses_sunset", false);
            ZonedDateTime zonedNow = ZonedDateTime.now();
            Instant now = zonedNow.toInstant();
            Instant anchor = latestAnchor(zonedNow, latitude, longitude, includeSunset);
            SolarCycle.State state = SolarCycle.at(anchor, now);
            long elapsed = Math.max(0, Duration.between(anchor, now).getSeconds());
            long untilSmallStep = SolarCycle.SMALL_STEP_SECONDS
                - elapsed % SolarCycle.SMALL_STEP_SECONDS;
            long delay = Math.max(1, untilSmallStep) * 1000L + 250L;
            delay = Math.min(delay, millisUntilNextSolarReset(
                zonedNow, latitude, longitude, includeSunset));
            return new Reading(state.large, state.small, true, Math.max(1_000L, delay));
        } catch (Exception ignored) {
            return Reading.unavailable();
        }
    }

    private Instant latestAnchor(ZonedDateTime now, double latitude, double longitude,
                                 boolean includeSunset) {
        Instant instant = now.toInstant();
        LocalDate date = now.toLocalDate();
        ZoneId zone = now.getZone();
        for (int daysBack = 0; daysBack <= 370; daysBack++) {
            SolarCalculator.Events events = SolarCalculator.calculate(date.minusDays(daysBack),
                zone, latitude, longitude);
            Instant latest = null;
            if (events.sunrise != null && !events.sunrise.isAfter(instant)) latest = events.sunrise;
            if (includeSunset && events.sunset != null && !events.sunset.isAfter(instant)
                    && (latest == null || events.sunset.isAfter(latest))) latest = events.sunset;
            if (latest != null) return latest;
        }
        return date.atStartOfDay(zone).toInstant();
    }

    private long millisUntilNextSolarReset(ZonedDateTime now, double latitude, double longitude,
                                            boolean includeSunset) {
        Instant instant = now.toInstant();
        LocalDate date = now.toLocalDate();
        ZoneId zone = now.getZone();
        long best = Long.MAX_VALUE;
        for (int day = 0; day <= 1; day++) {
            SolarCalculator.Events events = SolarCalculator.calculate(date.plusDays(day), zone,
                latitude, longitude);
            if (events.sunrise != null && events.sunrise.isAfter(instant))
                best = Math.min(best, Duration.between(instant, events.sunrise).toMillis() + 250L);
            if (includeSunset && events.sunset != null && events.sunset.isAfter(instant))
                best = Math.min(best, Duration.between(instant, events.sunset).toMillis() + 250L);
        }
        return best;
    }

    private Notification buildNotification(int large, int small, boolean available) {
        RemoteViews content = new RemoteViews(getPackageName(), R.layout.notification_solar_agent);
        Bitmap largeBitmap = SolarGlyphView.renderBitmap(large, dp(42), false);
        Bitmap smallBitmap = SolarGlyphView.renderBitmap(small, dp(24), false);
        content.setImageViewBitmap(R.id.solar_agent_large, largeBitmap);
        content.setImageViewBitmap(R.id.solar_agent_small, smallBitmap);
        content.setTextViewText(R.id.solar_agent_title,
            available ? "SolaRitm" : "SolaRitm • așteaptă coordonate");

        Intent launch = new Intent(this, MainActivity.class)
            .putExtra("open_solaritm", true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, 1805, launch,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(Icon.createWithBitmap(renderStatusIcon(large, small)))
            .setContentTitle("Agent SolaRitm")
            .setContentText(available ? "Ciclurile solare sunt active" : "Deschide SolaRitm pentru locație")
            .setCustomContentView(content)
            .setStyle(new Notification.DecoratedCustomViewStyle())
            .setContentIntent(pending)
            .setColor(0xffd2a335)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .build();
    }

    private Bitmap renderStatusIcon(int large, int small) {
        int size = dp(48);
        Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);
        int largeSize = Math.round(size * .72f);
        int smallSize = Math.round(size * .40f);
        Bitmap first = SolarGlyphView.renderBitmap(large, largeSize, true);
        Bitmap second = SolarGlyphView.renderBitmap(small, smallSize, true);
        canvas.drawBitmap(first, null, new Rect(0, (size - largeSize) / 2,
            largeSize, (size + largeSize) / 2), null);
        canvas.drawBitmap(second, null, new Rect(size - smallSize,
            (size - smallSize) / 2, size, (size + smallSize) / 2), null);
        return icon;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Reading {
        final int large;
        final int small;
        final boolean hasCoordinates;
        final long nextUpdateMillis;

        Reading(int large, int small, boolean hasCoordinates, long nextUpdateMillis) {
            this.large = large;
            this.small = small;
            this.hasCoordinates = hasCoordinates;
            this.nextUpdateMillis = nextUpdateMillis;
        }

        static Reading unavailable() { return new Reading(5, 5, false, 60_000L); }
    }
}
