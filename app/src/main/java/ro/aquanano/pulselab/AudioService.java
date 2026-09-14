package ro.aquanano.pulselab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

public final class AudioService extends Service {
    private static final String CHANNEL = "pulselab_audio";
    private static final int NOTIFICATION_ID = 1804;
    private final LocalBinder binder = new LocalBinder();
    private AudioEngine engine;
    private MediaPlayer music;
    private Uri musicUri;
    private float musicVolume = 0.2f;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable completionWatcher = new Runnable() {
        @Override public void run() {
            if (music != null && !engine.isGeneratorActive()) {
                stopMusic();
                leaveForegroundIfIdle();
            }
            handler.postDelayed(this, 200);
        }
    };

    public final class LocalBinder extends Binder {
        public AudioService service() { return AudioService.this; }
    }

    @Override public void onCreate() {
        super.onCreate();
        engine = new AudioEngine();
        engine.start();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "AquaRitm audio",
            NotificationManager.IMPORTANCE_LOW));
        handler.post(completionWatcher);
    }

    @Override public IBinder onBind(Intent intent) { return binder; }
    public AudioEngine engine() { return engine; }

    public void enterForeground() {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, launch,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("AquaRitm")
            .setContentText("Sesiune audio activă")
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
        startForeground(NOTIFICATION_ID, n);
    }

    public void leaveForegroundIfIdle() {
        if (!engine.isMetronomeRunning() && !engine.isGeneratorActive()) stopForeground(STOP_FOREGROUND_REMOVE);
    }

    public void setMusic(Uri uri, float volume) {
        musicUri = uri;
        musicVolume = volume;
        stopMusic();
    }

    public void setMusicVolume(float volume) {
        musicVolume = volume;
        if (music != null) music.setVolume(volume, volume);
    }

    public void startMusicIfSelected() {
        if (musicUri == null) return;
        try {
            stopMusic();
            music = new MediaPlayer();
            music.setDataSource(this, musicUri);
            music.setLooping(true);
            music.setVolume(musicVolume, musicVolume);
            music.prepare();
            music.start();
        } catch (Exception e) {
            stopMusic();
        }
    }

    public void pauseMusic(boolean paused) {
        if (music == null) return;
        if (paused && music.isPlaying()) music.pause();
        else if (!paused && !music.isPlaying()) music.start();
    }

    public void stopMusic() {
        if (music != null) {
            try { music.stop(); } catch (Exception ignored) { }
            music.release();
            music = null;
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(completionWatcher);
        stopMusic();
        engine.shutdown();
        super.onDestroy();
    }
}
