package com.allisighs.caesar.sjp.account;

import com.allisighs.caesar.sjp.config.SjpConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import java.lang.reflect.Field;
import java.util.UUID;

public class AccountManager {
    public static java.util.List<String> list() { return SjpConfig.get().accounts; }
    public static void add(String nick) {
        nick = nick.trim();
        if (nick.isEmpty()) return;
        if (!SjpConfig.get().accounts.contains(nick)) { SjpConfig.get().accounts.add(nick); SjpConfig.get().save(); }
    }
    public static void remove(String nick) { SjpConfig.get().accounts.remove(nick); SjpConfig.get().save(); }
    public static String currentName() { return Minecraft.getMinecraft().getSession().getUsername(); }
    public static boolean switchTo(String nick) {
        try {
            nick = nick.trim();
            if (nick.isEmpty()) return false;
            UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes("UTF-8"));
            Session session = new Session(nick, offline.toString().replace("-", ""), "0", "mojang");
            Field f = findField(Minecraft.class, "session", "field_71449_j");
            f.setAccessible(true);
            f.set(Minecraft.getMinecraft(), session);
            return true;
        } catch (Throwable t) { return false; }
    }
    private static Field findField(Class<?> c, String... names) throws NoSuchFieldException {
        for (String n : names) { try { return c.getDeclaredField(n); } catch (NoSuchFieldException ignored) {} }
        throw new NoSuchFieldException(names[0]);
    }
}
