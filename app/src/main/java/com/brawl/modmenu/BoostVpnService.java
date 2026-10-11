package com.brawl.modmenu;

import android.net.VpnService;
import android.content.Intent;

public class BoostVpnService extends VpnService {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: фейковый VPN
        return START_STICKY;
    }
}
