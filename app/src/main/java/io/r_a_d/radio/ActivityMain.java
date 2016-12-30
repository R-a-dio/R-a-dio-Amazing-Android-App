package io.r_a_d.radio;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

public class ActivityMain extends AppCompatActivity implements ViewPager.OnPageChangeListener{

    private boolean playing = false;
    private SimpleExoPlayer sep;
    private Integer api_update_delay = 10000;
    private ViewPager viewPager;
    private JSONScraperTask jsonTask = new JSONScraperTask(this);
    private String radio_url = "https://stream.r-a-d.io/main.mp3";
    private String api_url = "https://r-a-d.io/api";
    public JSONObject current_ui_json;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == 0){
                updateUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homescreen);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new CustomPagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);

        viewPager.addOnPageChangeListener(this);

        scrapeJSON(api_url);
        setupMediaPlayer();

        handler.postDelayed(new Runnable(){
            public void run(){
                scrapeJSON(api_url);
                handler.postDelayed(this, api_update_delay);
            }
        }, api_update_delay);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sep.stop();
        sep.release();
        sep = null;
    }

    @Override
    public void onPageSelected(int position) {
        TextView title_text = (TextView)findViewById(R.id.radio);
        View page = viewPager.getChildAt(position);

        int pageID = page.getId();

        switch (pageID){
            case R.id.now_playing_page:
                title_text.setText(R.string.app_name);
                break;
            case R.id.requests_page:
                title_text.setText(R.string.request_page);
                break;
            case R.id.news_page:
                title_text.setText(R.string.news_page);
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        return;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        return;
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

    public void updateUI(){
        try {
            View now_playing = viewPager.getChildAt(0);
            JSONObject djdata = new JSONObject(current_ui_json.getString("dj"));
            JSONArray queue_list = current_ui_json.getJSONArray("queue");
            JSONArray last_played_list = current_ui_json.getJSONArray("lp");

            TextView np = (TextView)now_playing.findViewById(R.id.tags);
            String tags = current_ui_json.getString("np");


            TextView ls = (TextView)now_playing.findViewById(R.id.listeners);
            String listeners = current_ui_json.getString("listeners");

            TextView dj_name = (TextView)now_playing.findViewById(R.id.dj_name);
            String djname = djdata.getString("djname");

            Integer song_length = current_ui_json.getInt("end_time") - current_ui_json.getInt("start_time");
            Integer song_length_minutes = song_length / 60;
            Integer song_length_seconds = song_length % 60;
            Integer song_length_position = current_ui_json.getInt("current") - current_ui_json.getInt("start_time");
            Integer song_length_position_minutes = song_length_position / 60;
            Integer song_length_position_seconds = song_length_position % 60;
            TextView te = (TextView)now_playing.findViewById(R.id.time_elapsed);
            TextView tt = (TextView)now_playing.findViewById(R.id.total_time);
            ProgressBar pb = (ProgressBar)now_playing.findViewById(R.id.progressBar3);


            //String[] djcolor = djdata.getString("djcolor").split(" ");
            //Integer djhex = Color.rgb(Integer.valueOf(djcolor[0]), Integer.valueOf(djcolor[1]), Integer.valueOf(djcolor[2]));

            TextView nextsong = (TextView)now_playing.findViewById(R.id.nextsong);
            String ns = queue_list.getJSONObject(0).getString("meta");

            if(!np.getText().toString().equals(tags))
                np.setText(tags);
                tt.setText(song_length_minutes.toString() + ":" + String.format("%02d", song_length_seconds));
                pb.setMax(song_length);
            if (!np.isSelected()) {
                np.setMarqueeRepeatLimit(-1);
                np.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                np.setHorizontallyScrolling(true);
                np.setMaxLines(1);
                np.setSelected(true);
            }
            te.setText(song_length_position_minutes.toString() + ":" + String.format("%02d", song_length_position_seconds));
            pb.setProgress(song_length_position);

            ls.setText("Listeners: " + listeners);


            if(!dj_name.getText().toString().equals(djname))
                dj_name.setText(djname);
            //dj_name.setTextColor(djhex);

            if(!nextsong.getText().toString().equals(ns))
                nextsong.setText(ns);
            if (!nextsong.isSelected()) {
                nextsong.setMarqueeRepeatLimit(-1);
                nextsong.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                nextsong.setHorizontallyScrolling(true);
                nextsong.setMaxLines(1);
                nextsong.setSelected(true);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void scrapeJSON(String urlToScrape){
        jsonTask.cancel(false);
        jsonTask = new JSONScraperTask(this);
        jsonTask.execute(urlToScrape);
    }

    public void setUIJSON(String jsonString) throws JSONException {
        current_ui_json = new JSONObject(new JSONObject(jsonString).getString("main"));
        updateUI();
    }

    public void togglePlayPause(View v) {
        ImageButton img = (ImageButton)v.findViewById(R.id.play_pause);
        if(!playing){
            img.setImageResource(R.drawable.pause_small);
            playing = true;
            sep.seekToDefaultPosition();
            sep.setPlayWhenReady(playing);
        } else {
            img.setImageResource(R.drawable.arrow_small);
            playing = false;
            sep.setPlayWhenReady(playing);
        }
    }
}
