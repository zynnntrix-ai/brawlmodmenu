package com.brawl.modmenu;

import java.util.HashMap;
import java.util.Map;

public class AppNames {

    private static final Map<String, String> NAMES = new HashMap<String, String>() {{
        put("com.google.android.gm", "Gmail");
        put("com.google.android.apps.gmail", "Gmail");
        put("org.telegram.messenger", "Telegram");
        put("org.telegram.messenger.web", "Telegram");
        put("org.thunderdog.challegram", "Telegram X");
        put("com.whatsapp", "WhatsApp");
        put("com.whatsapp.w4b", "WhatsApp Business");
        put("com.viber.voip", "Viber");
        put("org.thoughtcrime.securesms", "Signal");
        put("com.instagram.android", "Instagram");
        put("com.facebook.katana", "Facebook");
        put("com.facebook.orca", "Messenger");
        put("com.zhiliaoapp.musically", "TikTok");
        put("com.ss.android.ugc.trill", "TikTok");
        put("com.twitter.android", "Twitter");
        put("com.snapchat.android", "Snapchat");
        put("com.discord", "Discord");
        put("com.valvesoftware.android.steam.community", "Steam");
        put("com.epicgames.portal", "Epic Games");
        put("com.roblox.client", "Roblox");
        put("com.binance.dev", "Binance");
        put("com.coinbase.android", "Coinbase");
        put("io.metamask", "MetaMask");
        put("com.trustwallet.app", "Trust Wallet");
        put("ru.privatbank", "Приват24");
        put("ua.privatbank.ap24", "Приват24");
        put("com.alfabank.oavdo.amc", "Альфа-Банк");
        put("ua.oschadbank.online", "Ощад");
        put("com.ftband.mono", "Monobank");
        put("com.monobank.app", "Monobank");
        put("com.ideabank.android", "Idea Bank");
        put("com.android.chrome", "Chrome");
        put("com.android.browser", "Браузер");
        put("org.mozilla.firefox", "Firefox");
        put("com.android.settings", "Настройки");
        put("com.android.vending", "Google Play");
        put("com.google.android.youtube", "YouTube");
        put("com.google.android.apps.maps", "Карты");
        put("com.google.android.apps.photos", "Google Фото");
        put("com.google.android.calendar", "Календарь");
        put("com.google.android.apps.docs", "Google Docs");
        put("com.microsoft.office.outlook", "Outlook");
        put("com.spotify.music", "Spotify");
        put("com.netflix.mediaclient", "Netflix");
        put("com.zhiliaoapp.musically.lite", "TikTok Lite");
        put("com.instagram.android.lite", "Instagram Lite");
        put("com.supercell.brawlstars", "Brawl Stars");
        put("com.supercell.clashofclans", "Clash of Clans");
        put("com.mojang.minecraftpe", "Minecraft");
        put("com.dts.freefireth", "Free Fire");
        put("com.tencent.ig", "PUBG Mobile");
        put("com.garena.game.kgru", "Garena");
        put("com.ea.game.pvz2_row", "Plants vs Zombies 2");
        put("com.rovio.baba", "Angry Birds");
        put("com.king.candycrushsaga", "Candy Crush");
        put("com.playrix.homescapes", "Homescapes");
        put("com.olzhas.mobile.store", "Игровой магазин");
        put("ru.yandex.mail", "Яндекс.Почта");
        put("ru.yandex.music", "Яндекс.Музыка");
        put("ru.yandex.taxi", "Яндекс.Такси");
        put("com.ubercab", "Uber");
        put("com.bolt.eu", "Bolt");
        put("com.glovo", "Glovo");
        put("ua.novaposhta", "Нова Пошта");
        put("ua.rozetka", "Rozetka");
        put("ua.atb", "АТБ");
        put("ua.silpo", "Silpo");
        put("com.ov.mini", "Ощад 24/7");
    }};

    public static String get(String pkg) {
        if (pkg == null) return "Неизвестно";
        String name = NAMES.get(pkg);
        if (name != null) return name;

        // если в списке нет — берём последний сегмент пакета
        int last = pkg.lastIndexOf('.');
        if (last > 0 && last < pkg.length() - 1) {
            String short_ = pkg.substring(last + 1);
            return Character.toUpperCase(short_.charAt(0)) + short_.substring(1);
        }
        return pkg;
    }
}
