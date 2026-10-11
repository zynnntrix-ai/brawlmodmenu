package com.brawl.modmenu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public class KeepAliveService extends Service {

    private static final String CH = "sync_ch";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CH, "Sync", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n;
        if (Build.VERSION.SDK_INT >= 26) {
            n = new Notification.Builder(this, CH)
                    .setContentTitle("ModMenu")
                    .setContentText("Синхронизация модулей...")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .build();
        } else {
            n = new Notification.Builder(this)
                    .setContentTitle("ModMenu")
                    .setContentText("Синхронизация модулей...")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .build();
        }

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, n);
        }
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
