package com.brawl.modmenu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;

public class BoostVpnService extends VpnService {

    private static final String CH = "zbot_vpn";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : "";

        if ("STOP".equals(action)) {
            try { stopSelf(); } catch (Throwable ignored) {}
            return START_NOT_STICKY;
        }

        // Иконка VPN в шторке — через VpnService.Builder
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(
                    CH, "Zbot VPN", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false);
                NotificationManager nm = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) nm.createNotificationChannel(ch);
            }

            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                b = new Notification.Builder(this, CH);
            } else {
                b = new Notification.Builder(this);
            }
            b.setContentTitle("Zbot VPN+")
             .setContentText("Защищённое соединение активно")
             .setSmallIcon(android.R.drawable.ic_lock_lock);
            startForeground(2, b.build());
        } catch (Throwable ignored) {}

        return START_STICKY;
    }
}
