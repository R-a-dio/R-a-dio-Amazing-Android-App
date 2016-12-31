package io.r_a_d.radio;

import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

class DJImageTask extends AsyncTask<String, Void, RoundedBitmapDrawable> {

    private ActivityMain activity;

    public DJImageTask(ActivityMain activity) {
        this.activity = activity;
    }

    @Override
    protected RoundedBitmapDrawable doInBackground(String... urlToScrape) {
        InputStream is = null;
        RoundedBitmapDrawable roundDrawable = null;
        try {
            is = (InputStream) new URL(urlToScrape[0]).getContent();
            roundDrawable = RoundedBitmapDrawableFactory.create(activity.getResources(), is);
            roundDrawable.setCircular(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roundDrawable;
    }


    protected void onPostExecute(RoundedBitmapDrawable image) {
        activity.setDJImage(image);
    }
}