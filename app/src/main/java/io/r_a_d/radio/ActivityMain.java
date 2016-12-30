package io.r_a_d.radio;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

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

import java.io.IOException;

public class ActivityMain extends AppCompatActivity {

    private boolean playing = false;
    private String radio_url = "http://stream.r-a-d.io/main.mp3";
    private SimpleExoPlayer sep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homescreen);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new CustomPagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);
        setupMediaPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sep.stop();
        sep.release();
        sep = null;
    }

    public void setupMediaPlayer() {
        TrackSelector tSelector = new DefaultTrackSelector();
        LoadControl lc = new DefaultLoadControl();
        sep = ExoPlayerFactory.newSimpleInstance(this, tSelector, lc);

        DataSource.Factory dsf = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "R/a/dio-Android-App"));
        ExtractorsFactory extractors = new DefaultExtractorsFactory();
        MediaSource audioSource = new ExtractorMediaSource(Uri.parse(radio_url), dsf, extractors, null, null);

        sep.prepare(audioSource);
    }

    public void togglePlayPause(View v) throws IOException {
        ImageButton img = (ImageButton)v.findViewById(R.id.play_pause);
        if(!playing){
            img.setImageResource(R.drawable.pause_small);
            playing = true;
            sep.setPlayWhenReady(playing);
            sep.seekToDefaultPosition();
        } else {
            img.setImageResource(R.drawable.arrow_small);
            playing = false;
            sep.setPlayWhenReady(playing);
        }
    }
}
