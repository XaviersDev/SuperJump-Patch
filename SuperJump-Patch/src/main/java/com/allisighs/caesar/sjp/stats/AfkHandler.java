package com.allisighs.caesar.sjp.stats;

import com.allisighs.caesar.sjp.config.SjpConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.util.Random;

public class AfkHandler {

    private final Random rnd = new Random();
    private Field fTitle, fSub;
    private boolean ready = false;
    private long lastReact = 0L;
    private int jumpRelease = 0;

    private static final String[] SAFE = {
            "ку", "оп", "ам", "ит", "он", "як", "ри", "ха", "то", "ня",
            "qw", "as", "lo", "mi", "ze", "pa", "ko", "ru", "ti", "no"
    };

    public AfkHandler() {
        try {
            Class<?> gui = net.minecraft.client.gui.GuiIngame.class;
            fTitle = find(gui, "displayedTitle", "field_175201_x");
            fSub = find(gui, "displayedSubTitle", "field_175200_y");
            fTitle.setAccessible(true); fSub.setAccessible(true);
            ready = true;
        } catch (Throwable t) { ready = false; }
    }

    private Field find(Class<?> c, String... names) throws NoSuchFieldException {
        for (String n : names) { try { return c.getDeclaredField(n); } catch (NoSuchFieldException ignored) {} }
        throw new NoSuchFieldException(names[0]);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!ready) return;
        SjpConfig cfg = SjpConfig.get();
        if (!cfg.modEnabled || !cfg.antiAfk) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.ingameGUI == null) return;

        if (jumpRelease > 0) {
            jumpRelease--;
            if (jumpRelease == 0) { try { mc.player.setJumping(false); } catch (Throwable ignored) {} }
        }
        long now = System.currentTimeMillis();
        if (now - lastReact < 3000) return;

        try {
            String title = (String) fTitle.get(mc.ingameGUI);
            String sub = (String) fSub.get(mc.ingameGUI);
            String combined = (title == null ? "" : title) + " " + (sub == null ? "" : sub);
            String clean = combined.replaceAll("(?i)\u00a7[0-9A-FK-OR]", "").toUpperCase();
            if (clean.contains("АФК") || clean.contains("AFK") || clean.contains("ОТОШ") || clean.contains("НЕ АКТИВ")) {
                react(mc); lastReact = now;
            }
        } catch (Throwable ignored) {}
    }

    private void react(Minecraft mc) {
        int mode = rnd.nextInt(3);
        if (mode == 0) send(mc, String.valueOf(1 + rnd.nextInt(999)));
        else if (mode == 1) send(mc, SAFE[rnd.nextInt(SAFE.length)]);
        else {
            if (mc.player.onGround) { try { mc.player.setJumping(true); jumpRelease = 3; } catch (Throwable ignored) {} }
            else send(mc, String.valueOf(1 + rnd.nextInt(999)));
        }
    }

    private void send(Minecraft mc, String msg) { try { mc.player.sendChatMessage(msg); } catch (Throwable ignored) {} }
}
