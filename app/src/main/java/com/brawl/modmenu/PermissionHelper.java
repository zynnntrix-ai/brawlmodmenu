package com.brawl.modmenu;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

public class PermissionHelper {

    public static final int REQ_PHOTOS = 200;
    public static final int REQ_STORAGE = 201;

    public static void requestPhotos(Activity act) {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                if (act.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                    act.requestPermissions(new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    }, REQ_PHOTOS);
                }
            } else {
                if (act.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    act.requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQ_STORAGE);
                }
            }
        } catch (Throwable ignored) {}
    }
}
