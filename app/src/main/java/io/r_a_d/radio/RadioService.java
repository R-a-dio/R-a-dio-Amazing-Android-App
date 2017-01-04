package io.r_a_d.radio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class RadioService extends Service {

    private static final String ACTION_PLAY = "io.r_a_d.radio.PLAY";
    private static final String ACTION_PAUSE = "io.r_a_d.radio.PAUSE";



    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager wifiManager;
    private WifiManager.WifiLock wifiLock;
    private SimpleExoPlayer sep;
    private Notification notification;


    private String radio_url = "https://stream.r-a-d.io/main.mp3";

    public RadioService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KilimDankLock");
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "KilimDankWifiLock");
        createMediaPlayer();

        Intent notificationIntent = new Intent(this, ActivityMain.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentTitle("R/a/dio is streaming");
        builder.setContentText("Touch to return to app");
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.lollipop_logo);
            builder.setColor(0xFFDF4C3A);
        } else {
            builder.setSmallIcon(R.drawable.normal_logo);
        }
        builder.setContentIntent(pendingIntent);
        notification = builder.build();

    }



    public void setupMediaPlayer() {
        DataSource.Factory dsf = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "R/a/dio-Android-App"));
        ExtractorsFactory extractors = new DefaultExtractorsFactory();
        MediaSource audioSource = new ExtractorMediaSource(Uri.parse(radio_url), dsf, extractors, null, null);

        sep.prepare(audioSource);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getStringExtra("action").equals(ACTION_PLAY)) {
            setupMediaPlayer();
            sep.setPlayWhenReady(true);
            acquireWakeLocks();
            startForeground(1, notification);
        } else if (intent.getStringExtra("action").equals(ACTION_PAUSE)){
            sep.stop();
            releaseWakeLocks();
            stopForeground(true);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sep.stop();
        sep.release();
        sep = null;
        releaseWakeLocks();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if(!PlayerState.CURRENTLY_PLAYING) {
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void acquireWakeLocks() {
        wakeLock.acquire();
        wifiLock.acquire();
    }

    public void releaseWakeLocks() {
        if(wakeLock.isHeld()) wakeLock.release();
        if(wifiLock.isHeld()) wifiLock.release();
    }

    public void createMediaPlayer() {
        TrackSelector tSelector = new DefaultTrackSelector();
        LoadControl lc = new DefaultLoadControl();
        sep = ExoPlayerFactory.newSimpleInstance(this, tSelector, lc);
    }
}