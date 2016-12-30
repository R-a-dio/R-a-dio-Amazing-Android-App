package io.r_a_d.radio;

import android.os.AsyncTask;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

class JSONScraperTask extends AsyncTask<String, Void, String> {

    private URL api_url_scraper;
    private ActivityMain activity;

    public JSONScraperTask(ActivityMain activity)
    {
        this.activity = activity;
    }

    @Override
    protected String doInBackground(String... urlToScrape) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            api_url_scraper = new URL(urlToScrape[0]);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            reader = new BufferedReader(new InputStreamReader(api_url_scraper.openStream(), "UTF-8"));
            for (String line; (line = reader.readLine()) != null;) {
                builder.append(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException ignored) {}
        }
        return builder.toString();
    }

    protected void onPostExecute(String json) {
        try {
            activity.setUIJSON(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}