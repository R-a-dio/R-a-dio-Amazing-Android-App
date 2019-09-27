package io.r_a_d.radio;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Handler;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import android.support.v4.media.MediaBrowserCompat;
import androidx.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import java.util.ArrayList;
import java.util.List;

public class RadioService extends MediaBrowserServiceCompat {

    // Define actions used in intents to control the service
    public static final String ACTION_PLAY = "io.r_a_d.radio.PLAY";
    public static final String ACTION_PAUSE = "io.r_a_d.radio.PAUSE";
    public static final String ACTION_UPDATE_TAGS = "io.r_a_d.radio.UPDATE_TAGS";
    private static final String ACTION_NPAUSE = "io.r_a_d.radio.NPAUSE";
    private static final String ACTION_MUTE = "io.r_a_d.radio.MUTE";
    private static final String ACTION_UNMUTE = "io.r_a_d.radio.UNMUTE";

    // Define the stream URL
    private static final String RADIO_URL = "https://stream.r-a-d.io/main.mp3";

    // Define the channel used by the notifications
    private static final String CHANNEL_ID = "io.r_a_d.radio.NOTIFICATIONS";

    // Define the media root id for the MediaBrowser stuff
    private static final String MEDIA_ROOT_ID = "radio_root_id";

    // Define manager locks
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    // Define the player volume
    private float m_volume;

    // Boolean keeping track of foreground
    private boolean m_foreground;

    // Define the music player
    private SimpleExoPlayer sep;

    // Define the notification in android's swipedown menu
    private Notification notification;

    // Define the managers
    private TelephonyManager mTelephonyManager;
    private AudioManager am;
    private NotificationManager m_nm;

    // Define the builders
    private NotificationCompat.Builder m_builder;
    private PlaybackStateCompat.Builder m_pbsBuilder;
    private MediaMetadataCompat.Builder m_metaBuilder;

    // Define the media session
    private MediaSessionCompat m_mediaSession;

    // Define the binder that gets use in conjunction with the main activity
    private final IBinder m_binder = new RadioBinder();

    // Define the listener that will mute/unmute audio if there's an incoming call
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

    // Define the listener that will control what happens when focus is changed such
    // as when headphones are unplugged
    private AudioManager.OnAudioFocusChangeListener focusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {

                        case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) :
                            if(sep != null)
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

    // Define the broadcast receiver to handle any broadcasts
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                Intent i = new Intent(context, RadioService.class);
                i.putExtra("action", "io.r_a_d.radio.PAUSE");
                context.startService(i);
            }
        }
    };

    @Override
    public void onCreate() {
        m_volume = 1.0f;
        m_foreground = false;

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null)
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lock:KilimDankLock");

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null)
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "KilimDankWifiLock");

        createMediaPlayer();
        createMediaSession();

        am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        m_nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotification();

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        // This stuff is for the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(receiver, filter);

        PlayerState.setServiceStatus(true);
    }

    /**
     * Disable ability to browse for content, we can't browse internet radio
     *
     * @param clientPackageName
     * @param clientUid
     * @param rootHints
     * @return
     */
    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        // Clients can connect, but you can't browse internet radio
        // so onLoadChildren returns nothing. This disables the ability to browse for content.
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    /**
     * For MediaBrowser clients send nothing, we can't browse internet radio
     *
     * @param parentMediaId
     * @param result
     */
    @Override
    public void onLoadChildren(String parentMediaId, Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    private void createMediaSession()
    {
        m_mediaSession = new MediaSessionCompat(this, "RadioMediaSession");
        m_mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS & MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        m_mediaSession.setActive(true);
        m_mediaSession.setCallback(m_msCallback);

        m_pbsBuilder = new PlaybackStateCompat.Builder();
        m_pbsBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f, SystemClock.elapsedRealtime());

        m_metaBuilder = new MediaMetadataCompat.Builder();
        m_mediaSession.setPlaybackState(m_pbsBuilder.build());
    }

    private void mutePlayer() {
        if(sep != null) {
            m_volume = sep.getVolume();
            sep.setVolume(0);
        }
    }

    private void unmutePlayer() {
        if(sep != null)
            sep.setVolume(m_volume);
    }

    private void setupMediaPlayer() {
        DataSource.Factory dsf = new DefaultHttpDataSourceFactory("R/a/dio-Android-App");

        MediaSource audioSource = new ExtractorMediaSource.Factory(dsf)
                .createMediaSource(Uri.parse(RADIO_URL));

        if(sep != null)
            sep.prepare(audioSource);
    }

    private void createNotification() {
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        String channelID = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelID = createNotificationChannel();
        }

        m_builder = new NotificationCompat.Builder(this, channelID);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            m_builder.setSmallIcon(R.drawable.lollipop_logo);
            m_builder.setColor(0xFFDF4C3A);
        } else {
            m_builder.setSmallIcon(R.drawable.normal_logo);
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            m_builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }

        m_builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(m_mediaSession.getSessionToken())
                .setShowActionsInCompactView(0));

        m_builder.setContentIntent(pendingIntent);
    }

    private void updateNotification() {
        if (m_builder == null) {
            createNotification();
        }

        m_builder.setContentTitle(PlayerState.getTitle());
        m_builder.setContentText(PlayerState.getArtist());

        if (m_builder.mActions.isEmpty()) {
            Intent intent = new Intent(this, RadioService.class);
            NotificationCompat.Action action;

            if (PlayerState.isPlaying()) {
                intent.putExtra("action", RadioService.ACTION_NPAUSE);
                PendingIntent pendingButtonIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                action = new NotificationCompat.Action.Builder(R.drawable.exo_controls_pause, "Pause", pendingButtonIntent).build();
            } else {
                intent.putExtra("action", RadioService.ACTION_PLAY);
                PendingIntent pendingButtonIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                action = new NotificationCompat.Action.Builder(R.drawable.exo_controls_play, "Play", pendingButtonIntent).build();
            }

            m_builder.addAction(action);
        }

        notification = m_builder.build();

        m_nm.notify(1, notification);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String chanName = "R/a/dio Stream Service";

        NotificationChannel chan = new NotificationChannel(CHANNEL_ID, chanName, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        m_nm.createNotificationChannel(chan);

        return CHANNEL_ID;
    }

    public void beginPlaying() {
        int result = am.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            PlayerState.setPlayingStatus(true);

            if (m_foreground)
                m_builder.mActions.clear();

            updateNotification();
            setupMediaPlayer();

            if(sep != null)
                sep.setPlayWhenReady(true);

            acquireWakeLocks();

            if (!m_foreground) {
                startForeground(1, notification);
                m_foreground = true;
            }

            updateTags();

            m_pbsBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f, SystemClock.elapsedRealtime());
            m_mediaSession.setPlaybackState(m_pbsBuilder.build());
        }
    }

    public void stopPlaying () {
        PlayerState.setPlayingStatus(false);

        if(sep != null)
            sep.stop();

        releaseWakeLocks();

        if (m_foreground) {
            m_builder.mActions.clear();
            stopForeground(true);
            m_foreground = false;

            m_pbsBuilder.setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f, SystemClock.elapsedRealtime());
            m_mediaSession.setPlaybackState(m_pbsBuilder.build());
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null || intent.getStringExtra("action") == null) return super.onStartCommand(intent, flags, startId);

        if (MediaButtonReceiver.handleIntent(m_mediaSession, intent) != null) return super.onStartCommand(intent, flags, startId);

        switch (intent.getStringExtra("action")) {
            case ACTION_PLAY:
                beginPlaying();
                break;
            case ACTION_PAUSE:
                stopPlaying();
                break;
            case ACTION_UPDATE_TAGS:
                updateTags();
                break;
            case ACTION_NPAUSE:
                PlayerState.setPlayingStatus(false);
                m_builder.mActions.clear();
                updateNotification();

                if (sep != null)
                    sep.stop();

                releaseWakeLocks();

                m_pbsBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f, SystemClock.elapsedRealtime());
                m_mediaSession.setPlaybackState(m_pbsBuilder.build());
                break;
            case ACTION_MUTE:
                mutePlayer();
                break;
            case ACTION_UNMUTE:
                unmutePlayer();
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(sep != null) {
            sep.stop();
            sep.release();
            sep = null;
        }

        releaseWakeLocks();
        mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(receiver);
        m_mediaSession.release();
        PlayerState.setServiceStatus(false);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if(!PlayerState.isPlaying()) {
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return m_binder;
    }

    @SuppressLint("WakelockTimeout")
    private void acquireWakeLocks() {
        wakeLock.acquire();
        wifiLock.acquire();
    }

    private void releaseWakeLocks() {
        if(wakeLock.isHeld()) wakeLock.release();
        if(wifiLock.isHeld()) wifiLock.release();
    }

    private void createMediaPlayer() {
        sep = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(),
                new DefaultLoadControl());
    }

    public void updateTags() {
        if (m_foreground) {
            updateNotification();

            m_metaBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, PlayerState.getTitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, PlayerState.getArtist())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0);

            m_mediaSession.setMetadata(m_metaBuilder.build());

            Intent i = new Intent("com.android.music.metachanged");
            i.putExtra("artist", PlayerState.getArtist());
            i.putExtra("track", PlayerState.getTitle());
            i.putExtra("duration", 0);
            i.putExtra("position", 0);
            sendBroadcast(i);
        }
    }

    public boolean isForeground() {
        return m_foreground;
    }

    public class RadioBinder extends Binder {
        RadioService getService() {
            return RadioService.this;
        }
    }

    private MediaSessionCompat.Callback m_msCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            beginPlaying();
        }

        @Override
        public void onPause() {
            stopPlaying();
        }

        @Override
        public void onStop() {
            stopPlaying();
        }
    };
}