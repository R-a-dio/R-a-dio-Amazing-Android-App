package io.r_a_d.radio;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class WebViewChat {

    private WebView webView;

    public WebViewChat(WebView webView)
    {
        this.webView = webView;
    }

    public void start(){

        WebSettings webSetting = this.webView.getSettings();
        webSetting.setJavaScriptEnabled(true);
        webSetting.setSupportZoom(false);

        /* TODO: in the future, it could be nice to have a parameters screen where you can:
         - Set the text zoom
         - Set your username (to not type it every time, would it be possible?)
         - Hide the chat?
         - do more? */
        webSetting.setTextZoom(80);

        webSetting.setSupportMultipleWindows(true);
        // needs to open target="_blank" links as KiwiIRC links have this attribute.
        // shamelessly ripped off https://stackoverflow.com/questions/18187714/android-open-target-blank-links-in-webview-with-external-browser
        this.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg)
            {
                WebView.HitTestResult result = view.getHitTestResult();
                String data = result.getExtra();
                Context context = view.getContext();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                context.startActivity(browserIntent);
                return false;
            }
        });

        webView.loadUrl("file:///android_asset/chat.html");
    }

}
