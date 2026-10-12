package com.brawl.modmenu;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.splash);

        TextView z = findViewById(R.id.splashZ);
        ProgressBar pb = findViewById(R.id.splashProgress);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(z, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(z, "scaleY", 1f, 1.15f, 1f);
        scaleX.setDuration(1200);
        scaleY.setDuration(1200);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();

        ObjectAnimator progress = ObjectAnimator.ofInt(pb, "progress", 0, 100);
        progress.setDuration(1500);
        progress.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            finish();
        }, 1600);
    }
}
