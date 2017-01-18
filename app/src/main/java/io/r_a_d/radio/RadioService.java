package io.r_a_d.radio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

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
    private static final String ACTION_NPAUSE = "io.r_a_d.radio.NPAUSE";
    private static final String ACTION_MUTE = "io.r_a_d.radio.MUTE";
    private static final String ACTION_UNMUTE = "io.r_a_d.radio.UNMUTE";



    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager wifiManager;
    private WifiManager.WifiLock wifiLock;
    private SimpleExoPlayer sep;
    private Notification notification;
    private TelephonyManager mTelephonyManager;
    private AudioManager am;
    private final PhoneStateListener mPhoneListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);

            if (state != TelephonyManager.CALL_STATE_IDLE) {
                mutePlayer();
            } else {
                unmutePlayer();
            }
        }
    };
    private AudioManager.OnAudioFocusChangeListener focusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {

                        case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) :
                            sep.setVolume(0.2f);
                            break;
                        case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) :
                            mutePlayer();
                            break;

                        case (AudioManager.AUDIOFOCUS_LOSS) :
                            stopPlaying();
                            break;

                        case (AudioManager.AUDIOFOCUS_GAIN) :
                            unmutePlayer();
                            break;
                        default: break;
                    }
                }
            };
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)/* || (action.equals(AudioManager.ACTION_HEADSET_PLUG) && (intent.getIntExtra("state", 0) == 0))*/) {
                Intent i = new Intent(context, RadioService.class);
                i.putExtra("action", "io.r_a_d.radio.PAUSE");
                context.startService(i);
            }
        }
    };


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

        am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);


        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        // This stuff is for the broadcast receiver
        IntentFilter filter = new IntentFilter();
//        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(receiver, filter);

    }

    public void mutePlayer() {
        sep.setVolume(0);
    }

    public void unmutePlayer() {
        sep.setVolume(1.0f);
    }

    public void setupMediaPlayer() {
        DataSource.Factory dsf = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "R/a/dio-Android-App"));
        ExtractorsFactory extractors = new DefaultExtractorsFactory();
        MediaSource audioSource = new ExtractorMediaSource(Uri.parse(radio_url), dsf, extractors, null, null);

        sep.prepare(audioSource);
    }

    public void createNotification() {
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        /*RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        contentView.setImageViewResource(R.id.notification_image, R.drawable.normal_logo);
        contentView.setTextViewText(R.id.notification_title, "My custom notification title");
        contentView.setTextViewText(R.id.notification_text, "My custom notification text");
        builder.setContent(contentView);*/
        builder.setContentTitle("R/a/dio is streaming");
        builder.setContentText("Touch to return to app");
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.lollipop_logo);
            builder.setColor(0xFFDF4C3A);
        } else {
            builder.setSmallIcon(R.drawable.normal_logo);
        }
        if ( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        builder.setContentIntent(pendingIntent);
        Intent intent = new Intent(this, RadioService.class);
       NotificationCompat.Action action;
        if(PlayerState.CURRENTLY_PLAYING) {
            intent.putExtra("action", RadioService.ACTION_NPAUSE);
            PendingIntent pendingButtonIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            action = new NotificationCompat.Action.Builder(R.drawable.exo_controls_pause, "Pause", pendingButtonIntent).build();
        } else {
            intent.putExtra("action", RadioService.ACTION_PLAY);
            PendingIntent pendingButtonIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            action = new NotificationCompat.Action.Builder(R.drawable.exo_controls_play, "Play", pendingButtonIntent).build();
        }
        builder.addAction(action);
        notification = builder.build();
    }

    public void beginPlaying() {
        int result = am.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            PlayerState.CURRENTLY_PLAYING = true;
            createNotification();
            setupMediaPlayer();
            sep.setPlayWhenReady(true);
            acquireWakeLocks();
            startForeground(1, notification);
        }
    }

    public void stopPlaying () {
        PlayerState.CURRENTLY_PLAYING = false;
        sep.stop();
        releaseWakeLocks();
        stopForeground(true);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null || intent.getStringExtra("action") == null)return super.onStartCommand(intent, flags, startId);
        if (intent.getStringExtra("action").equals(ACTION_PLAY)) {
            beginPlaying();
        } else if (intent.getStringExtra("action").equals(ACTION_PAUSE)){
            stopPlaying();
        } else if (intent.getStringExtra("action").equals(ACTION_NPAUSE)){
            PlayerState.CURRENTLY_PLAYING = false;
            createNotification();
            sep.stop();
            releaseWakeLocks();
            startForeground(1, notification);
            stopForeground(false);
        } else if (intent.getStringExtra("action").equals(ACTION_MUTE)){
            mutePlayer();
        } else if (intent.getStringExtra("action").equals(ACTION_UNMUTE)){
            unmutePlayer();
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
        mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(receiver);
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