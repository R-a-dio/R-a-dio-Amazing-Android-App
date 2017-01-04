package io.r_a_d.radio;

import android.os.AsyncTask;
import android.util.Xml;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Kethsar on 1/2/2017.
 */

public class Requestor {
    private CookieManager mCookieManager;
    private ActivityMain mActivity;
    private final String REQUEST_URL = "https://r-a-d.io/request/%1$d";
    private String mToken;

    public Requestor(ActivityMain activity){
        mActivity = activity;
        mCookieManager = new CookieManager();
        mToken = null;
    }

    public void setToken(String token){
        mToken = token;
    }

    public String getToken(){
        return mToken;
    }

    public void Request(Integer songID){
        String request = String.format(REQUEST_URL, songID);

        if(mToken == null){
            CookieTask ct = new CookieTask(this);
            ct.execute(mCookieManager);
        }

        new RequestTask(this, mActivity).execute(request);
    }

    protected class CookieTask extends AsyncTask<CookieManager, Void, String>{
        private final String RADIO_SEARCH = "https://r-a-d.io/search";
        private Requestor mRequestor;

        public CookieTask(Requestor req){
            mRequestor = req;
        }

        @Override
        protected String doInBackground(CookieManager... params) {
            URL searchURL = null;
            XmlPullParser xmlParser = Xml.newPullParser();
            String retVal = null;
            BufferedReader reader = null;

            CookieHandler.setDefault(params[0]);
            try {
                xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            }
            catch (XmlPullParserException ex) {}

            try {
                searchURL = new URL(RADIO_SEARCH);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            try {
                reader = new BufferedReader(new InputStreamReader(searchURL.openStream(), "UTF-8"));
                for (String line; (line = reader.readLine()) != null;) {
                    line = line.trim();
                    Pattern p = Pattern.compile("value=\"(\\w+)\"");
                    Matcher m = p.matcher(line);

                    if(line.startsWith("<form")) {
                        if (m.find()) {
                            retVal = m.group(1);
                            break;
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (reader != null) try { reader.close(); } catch (IOException ignored) {}
            }

            return retVal;
        }

        @Override
        protected void onPostExecute(String s) {
            mRequestor.setToken(s);
        }
    }

    protected class RequestTask extends AsyncTask<String, Void, String> {
        private ActivityMain mActivity;
        private Requestor mRequestor;

        public RequestTask(Requestor req, ActivityMain activity){
            mRequestor = req;
            mActivity = activity;
        }

        @Override
        protected String doInBackground(String... params) {
            String reqString = params[0];
            String response = "";

            try{
                URL reqURL = new URL(reqString);
                HttpsURLConnection conn = (HttpsURLConnection)reqURL.openConnection();
                JSONObject tokenObject = new JSONObject();
                byte[] requestBytes = null;

                tokenObject.put("_token", mRequestor.getToken());
                requestBytes = tokenObject.toString().getBytes();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setChunkedStreamingMode(0);
                conn.setRequestProperty("Content-Type", "application/json");

                OutputStream os = conn.getOutputStream();
                os.write(requestBytes);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                } else {
                    response = "";
                }
            }
            catch (IOException|JSONException ex){
                ex.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            JSONObject response;
            try {
                response = new JSONObject(s);
                String key = (String)response.names().get(0);
                String value = response.getString(key);

                Toast.makeText(mActivity, value, Toast.LENGTH_LONG).show();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
