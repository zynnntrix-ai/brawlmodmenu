package com.brawl.modmenu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class WelcomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.welcome);

        findViewById(R.id.btnContinue).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, OnboardingActivity.class));
            finish();
        });
    }
}
