package io.r_a_d.radio;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
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
import java.util.HashMap;
import java.util.Map;

public class ActivityMain extends AppCompatActivity implements ViewPager.OnPageChangeListener{

    private boolean playing = false;
    private boolean songChanged = false;
    private SimpleExoPlayer sep;
    private Integer api_update_delay = 10000;
    private final Integer UPDATE_INTERVAL = 500;
    private ViewPager viewPager;
    private JSONScraperTask jsonTask = new JSONScraperTask(this);
    private String radio_url = "https://stream.r-a-d.io/main.mp3";
    private String api_url = "https://r-a-d.io/api";
    public JSONObject current_ui_json;
    private Thread songCalcThread;
    private final Object lock = new Object();
    private HashMap<String, Integer> songTimes;

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
        songTimes = new HashMap<>();

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new CustomPagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);

        viewPager.addOnPageChangeListener(this);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_dots);
        tabLayout.setupWithViewPager(viewPager, true);

        scrapeJSON(api_url);
        setupMediaPlayer();

        handler.postDelayed(new Runnable(){
            public void run(){
                scrapeJSON(api_url);
                handler.postDelayed(this, api_update_delay);
            }
        }, api_update_delay);

        songCalcThread = new Thread(new Runnable() {
            @Override
            public void run() {
                calculateSongTimes();
            }
        });
        songCalcThread.setDaemon(true);
        songCalcThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sep.stop();
        sep.release();
        sep = null;

        if(songCalcThread.isAlive() && !songCalcThread.isInterrupted())
            songCalcThread.interrupt();
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

            Integer song_start = current_ui_json.getInt("start_time");
            Integer song_end = current_ui_json.getInt("end_time");
            Integer song_length_position = current_ui_json.getInt("current") - song_start;


            //String[] djcolor = djdata.getString("djcolor").split(" ");
            //Integer djhex = Color.rgb(Integer.valueOf(djcolor[0]), Integer.valueOf(djcolor[1]), Integer.valueOf(djcolor[2]));

            TextView nextsong = (TextView)now_playing.findViewById(R.id.nextsong);
            String ns = queue_list.getJSONObject(0).getString("meta");

            if(!np.getText().toString().equals(tags)) {
                np.setText(tags);
                synchronized (lock)
                {
                    songTimes.put("start", song_start);
                    songTimes.put("end", song_end);
                    songTimes.put("position", song_length_position);
                    songChanged = true;
                }
                //pb.setMax(song_length);
            }

            if (!np.isSelected()) {
                np.setMarqueeRepeatLimit(-1);
                np.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                np.setHorizontallyScrolling(true);
                np.setMaxLines(1);
                np.setSelected(true);
            }
            //pb.setProgress(song_length_position);

            ls.setText("Listeners: " + listeners);


            if(!dj_name.getText().toString().equals(djname))
                dj_name.setText(djname);
            //dj_name.setTextColor(djhex);

            if(!nextsong.getText().toString().equals(ns))
                nextsong.setText(ns);

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

    private void updateSongProgress(HashMap<String, Integer> values)
    {
        View now_playing = viewPager.getChildAt(0);

        if(now_playing != null) {
            ProgressBar pb = (ProgressBar) now_playing.findViewById(R.id.progressBar3);
            TextView te = (TextView) now_playing.findViewById(R.id.time_elapsed);
            TextView tt = (TextView) now_playing.findViewById(R.id.total_time);

            if (values.containsKey("length")) {
                pb.setProgress(0);
                pb.setMax(values.get("length"));
            }

            if (values.containsKey("position")) {
                Integer position = values.get("position");

                if (position <= pb.getMax())
                    pb.setProgress(position);
            }

            if (values.containsKey("totalMinutes") && values.containsKey("totalSeconds")) {
                Integer minutes = values.get("totalMinutes");
                Integer seconds = values.get("totalSeconds");
                tt.setText(minutes.toString() + ":" + String.format("%02d", seconds));
            }

            if (values.containsKey("elapsedMinutes") && values.containsKey("elapsedSeconds")) {
                Integer minutes = values.get("elapsedMinutes");
                Integer seconds = values.get("elapsedSeconds");
                te.setText(minutes.toString() + ":" + String.format("%02d", seconds));
            }
        }
    }

    private void calculateSongTimes()
    {
        try{
            while(true) {
                final HashMap<String, Integer> songVals = new HashMap<>();
                Integer start, end, position;

                synchronized (lock) {
                    if(songTimes.containsKey("start"))
                        start = songTimes.get("start");
                    else
                        start = 0;

                    if(songTimes.containsKey("end"))
                        end = songTimes.get("end");
                    else
                        end = 0;

                    if(songTimes.containsKey("position"))
                        position = songTimes.get("position");
                    else
                        position = 0;

                    if(songChanged){
                        songChanged = false;
                        position = position * 1000;
                        Integer length = end - start;
                        Integer totalMinutes = length / 60;
                        Integer totalSeconds = length % 60;

                        songVals.put("length", length * 1000);
                        songVals.put("totalMinutes", totalMinutes);
                        songVals.put("totalSeconds", totalSeconds);
                    }
                    else{
                        position += UPDATE_INTERVAL;
                        songTimes.put("position", position);
                    }
                }

                songVals.put("position", position);
                songVals.put("elapsedMinutes", (position / 1000) / 60);
                songVals.put("elapsedSeconds", (position / 1000) % 60);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSongProgress(songVals);
                    }
                });

                Thread.sleep(UPDATE_INTERVAL);
            }
        }
        catch(InterruptedException ex) {}
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
