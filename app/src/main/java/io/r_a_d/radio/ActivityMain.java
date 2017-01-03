package io.r_a_d.radio;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class ActivityMain extends AppCompatActivity implements ViewPager.OnPageChangeListener{

    private boolean songChanged = false;
    private boolean firstSearchClick = true;
    private Integer api_update_delay = 10000;
    private final Integer UPDATE_INTERVAL = 500;
    private ViewPager viewPager;
    private JSONScraperTask jsonTask = new JSONScraperTask(this, 0);
    private DJImageTask djimageTask = new DJImageTask(this);
    private String api_url = "https://r-a-d.io/api";
    private String djimage_api = "https://r-a-d.io/api/dj-image/";
    private String news_api_url = "https://r-a-d.io/api/news/";
    private String current_dj_image;
    public JSONObject current_ui_json;
    private Thread songCalcThread;
    private final Object lock = new Object();
    private HashMap<String, Integer> songTimes;

    private boolean playing = false;

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
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homescreen);
        songTimes = new HashMap<>();

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new CustomPagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);

        viewPager.addOnPageChangeListener(this);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_dots);
        tabLayout.setupWithViewPager(viewPager, true);

        scrapeNews(news_api_url);
        scrapeJSON(api_url);

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

        if(songCalcThread.isAlive() && !songCalcThread.isInterrupted())
            songCalcThread.interrupt();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPageSelected(int position) {
        TextView title_text = (TextView)findViewById(R.id.radio);
        View page = viewPager.getChildAt(position);

        View x = getCurrentFocus();
        if(x != null)
        {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(x.getWindowToken(), 0);
        }

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

    public void openThread(View v) {
        try {
            if (current_ui_json != null) {
                String threadurl = current_ui_json.getString("thread");
                if(!threadurl.isEmpty() && !current_ui_json.getBoolean("isafkstream") && URLUtil.isValidUrl(threadurl)) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(threadurl)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateUI(){
        try {
            View now_playing = viewPager.getChildAt(0);
            TextView lp4 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.lp4);
            TextView lp3 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.lp3);
            TextView lp2 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.lp2);
            TextView lp1 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.lp1);
            TextView lp0 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.lp0);
            TextView q1 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.q1);
            TextView q2 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.q2);
            TextView q3 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.q3);
            TextView q4 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.q4);
            TextView q5 = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.q5);
            TextView threadtxt = (TextView)findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.thread);
            JSONObject djdata = new JSONObject(current_ui_json.getString("dj"));
            JSONArray queue_list = current_ui_json.getJSONArray("queue");
            JSONArray last_played_list = current_ui_json.getJSONArray("lp");

            TextView np = (TextView)now_playing.findViewById(R.id.tags);
            String tags = current_ui_json.getString("np");

            String threadurl = current_ui_json.getString("thread");


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

            String djimgid = djdata.getString("djimage");

            if(current_dj_image == null || !current_dj_image.equals(djimgid)) {
                current_dj_image = djimgid;
                scrapeDJImage(djimage_api + djimgid);
            }

            if(!threadurl.isEmpty() && !current_ui_json.getBoolean("isafkstream") && URLUtil.isValidUrl(threadurl)) {
                threadtxt.setText("Thread Up!");
                threadtxt.setTextColor(ResourcesCompat.getColor(getResources(), R.color.rblue, null));
            } else {
                threadtxt.setText("No Thread Up");
                threadtxt.setTextColor(ResourcesCompat.getColor(getResources(), R.color.dark, null));
            }

            if(!np.getText().toString().equals(tags)) {
                np.setText(tags);
                synchronized (lock)
                {
                    songTimes.put("start", song_start);
                    songTimes.put("end", song_end);
                    songTimes.put("position", song_length_position * 1000);
                    songChanged = true;
                }
                //pb.setMax(song_length);
            }

                lp0.setText(last_played_list.getJSONObject(0).getString("meta"));
                lp1.setText(last_played_list.getJSONObject(1).getString("meta"));
                lp2.setText(last_played_list.getJSONObject(2).getString("meta"));
                lp3.setText(last_played_list.getJSONObject(3).getString("meta"));
                lp4.setText(last_played_list.getJSONObject(4).getString("meta"));
                if(current_ui_json.getBoolean("isafkstream")) {
                    q1.setText(queue_list.getJSONObject(0).getString("meta"));
                    q1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.whited, null));
                    q2.setText(queue_list.getJSONObject(1).getString("meta"));
                    q3.setText(queue_list.getJSONObject(2).getString("meta"));
                    q4.setText(queue_list.getJSONObject(3).getString("meta"));
                    q5.setText(queue_list.getJSONObject(4).getString("meta"));
                    findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.hide_view1).setVisibility(View.VISIBLE);
                    findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.hide_view2).setVisibility(View.VISIBLE);
                    findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.hide_view3).setVisibility(View.VISIBLE);
                    findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.hide_view4).setVisibility(View.VISIBLE);
                } else {
                    q1.setText("No Queue");
                    q1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.dark, null));
                    q2.setText("");
                    q3.setText("");
                    q4.setText("");
                    q5.setText("");
                    findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.hide_view1).setVisibility(View.INVISIBLE);
                    findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.hide_view2).setVisibility(View.INVISIBLE);
                    findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.hide_view3).setVisibility(View.INVISIBLE);
                    findViewById(android.R.id.content).findViewById((R.id.left_drawer)).findViewById(R.id.hide_view4).setVisibility(View.INVISIBLE);
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

            if(!nextsong.getText().toString().equals(ns)) {
                if (current_ui_json.getBoolean("isafkstream")) {
                    nextsong.setText(ns);
                } else {
                    nextsong.setText("No Queue");
                    nextsong.setTextColor(ResourcesCompat.getColor(getResources(), R.color.dark, null));
                }
            }

            // Fix for syncing play/pause button by taking advantage of the fact that this code gets
            // called after the JSON gets scraped everything the main activity is instantiated.
            // I don't know where else it could/should go.
            ImageButton img = (ImageButton)now_playing.findViewById(R.id.play_pause);
            if(PlayerState.CURRENTLY_PLAYING){
                img.setImageResource(R.drawable.pause_small);
            } else {
                img.setImageResource(R.drawable.arrow_small);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void scrapeJSON(String urlToScrape){
        jsonTask.cancel(false);
        jsonTask = new JSONScraperTask(this, 0);
        jsonTask.execute(urlToScrape);
    }

    public void scrapeNews(String urlToScrape){
        new JSONScraperTask(this, 1).execute(urlToScrape);
    }

    public void setUIJSON(String jsonString) throws JSONException {
        current_ui_json = new JSONObject(new JSONObject(jsonString).getString("main"));
        updateUI();
    }

    public void clearFirstSearch(View v) {
        if(firstSearchClick) {
            EditText edts = (EditText)v.findViewById(R.id.searchquery);
            edts.getText().clear();
            firstSearchClick = false;
            edts.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        //performSearch();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    public void setNewsUI(String jsonString) throws JSONException {
        JSONArray newsjson = new JSONArray(jsonString);
        View news_view = viewPager.getChildAt(2);
        TextView newst1 = (TextView)news_view.findViewById(R.id.news_title1);
        TextView newst2 = (TextView)news_view.findViewById(R.id.news_title2);
        TextView newst3 = (TextView)news_view.findViewById(R.id.news_title3);
        newst1.setText(newsjson.getJSONObject(0).getString("title"));
        newst2.setText(newsjson.getJSONObject(1).getString("title"));
        newst3.setText(newsjson.getJSONObject(2).getString("title"));
        TextView news1 = (TextView)news_view.findViewById(R.id.news1);
        TextView news2 = (TextView)news_view.findViewById(R.id.news2);
        TextView news3 = (TextView)news_view.findViewById(R.id.news3);
        news1.setText(Html.fromHtml((newsjson.getJSONObject(0).getString("text"))));
        news2.setText(Html.fromHtml((newsjson.getJSONObject(1).getString("text"))));
        news3.setText(Html.fromHtml((newsjson.getJSONObject(2).getString("text"))));
        //news_view.setText(Html.fromHtml("<h2>Title</h2><br><p>Description here</p>"));
    }

    public void scrapeDJImage(String urlToScrape){
        djimageTask.cancel(false);
        djimageTask = new DJImageTask(this);
        djimageTask.execute(urlToScrape);
    }

    public void setDJImage(RoundedBitmapDrawable djimage) {
        ImageView djavatar = (ImageView)viewPager.getChildAt(0).findViewById(R.id.dj_avatar);
        djavatar.setImageDrawable(djimage);
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

    private boolean isDrawerVisible(View view) {

        Rect scrollBounds = new Rect();
        view.getHitRect(scrollBounds);
        if (view.findViewById(R.id.drawer_layout).findViewById(R.id.left_drawer).getLocalVisibleRect(scrollBounds)) {
            return true;
        } else {
            return false;
        }
    }

    private void playPlayerService() {
        Intent i = new Intent(this, RadioService.class);
        i.putExtra("action", "io.r_a_d.radio.PLAY");
        startService(i);
    }

    private void pausePlayerService() {
        Intent i = new Intent(this, RadioService.class);
        i.putExtra("action", "io.r_a_d.radio.PAUSE");
        startService(i);
    }

    public void togglePlayPause(View v) {
        if(isDrawerVisible(findViewById(android.R.id.content))) return;
        ImageButton img = (ImageButton)v.findViewById(R.id.play_pause);
        if(!PlayerState.CURRENTLY_PLAYING){
            img.setImageResource(R.drawable.pause_small);
            playPlayerService();
            PlayerState.CURRENTLY_PLAYING = true;
        } else {
            img.setImageResource(R.drawable.arrow_small);
            pausePlayerService();
            PlayerState.CURRENTLY_PLAYING = false;
        }
    }
}
