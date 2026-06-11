package com.allisighs.caesar.sjp.chat;

import com.allisighs.caesar.sjp.config.SjpConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

public class ChatManager {

    private UnlimitedChat chat;
    private boolean installed = false, restored = false;
    private int delay = 0;
    private int rollingId = 0x5A0000;
    private static final int WINDOW = 5;
    private static final long SPAM_WINDOW_MS = 12000L;

    private static class Entry {
        String text; int id; int count; long time;
        Entry(String text, int id, long time) { this.text = text; this.id = id; this.count = 1; this.time = time; }
    }

    private final java.util.ArrayDeque<Entry> recent = new java.util.ArrayDeque<Entry>();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!SjpConfig.get().unlimitedChat) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) { installed = false; restored = false; return; }
        if (!installed) install(mc);
        if (installed && !restored) { delay++; if (delay > 20) { chat.restore(); restored = true; } }
    }

    private void install(Minecraft mc) {
        try {
            chat = new UnlimitedChat(mc);
            Field f = findField(mc.ingameGUI.getClass(), "persistantChatGUI", "field_73840_e");
            f.setAccessible(true);
            f.set(mc.ingameGUI, chat);
            installed = true; delay = 0;
        } catch (Throwable t) { installed = false; }
    }

    private Field findField(Class<?> c, String... names) throws NoSuchFieldException {
        Class<?> cur = c;
        while (cur != null) {
            for (String n : names) { try { return cur.getDeclaredField(n); } catch (NoSuchFieldException ignored) {} }
            cur = cur.getSuperclass();
        }
        throw new NoSuchFieldException(names[0]);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.getMessage() == null) return;
        if (e.isCanceled()) return;
        if (e.getType() == ChatType.GAME_INFO) return;

        if (SjpConfig.get().antiSpam) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.ingameGUI != null) {
                String now = e.getMessage().getFormattedText();
                long t = System.currentTimeMillis();
                Entry match = null;
                for (Entry en : recent) if (en.text.equals(now) && t - en.time < SPAM_WINDOW_MS) { match = en; break; }
                e.setCanceled(true);
                if (match != null) {
                    match.count++; match.time = t;
                    try {
                        ITextComponent base = e.getMessage().createCopy();
                        base.appendText(" \u00a78(" + match.count + ")");
                        mc.ingameGUI.getChatGUI().deleteChatLine(match.id);
                        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(base, match.id);
                        if (chat != null && installed) chat.remember(now + " \u00a78(" + match.count + ")");
                    } catch (Throwable ignored) {}
                } else {
                    int id = ++rollingId;
                    recent.addLast(new Entry(now, id, t));
                    while (recent.size() > WINDOW) recent.removeFirst();
                    try {
                        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(e.getMessage(), id);
                        if (chat != null && installed) chat.remember(now);
                    } catch (Throwable ignored) {}
                }
                return;
            }
        }
        if (SjpConfig.get().unlimitedChat && chat != null && installed) {
            try { chat.remember(e.getMessage().getFormattedText()); } catch (Throwable ignored) {}
        }
    }
}
