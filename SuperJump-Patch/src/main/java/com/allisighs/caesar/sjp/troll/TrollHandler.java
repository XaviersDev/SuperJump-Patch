package com.allisighs.caesar.sjp.troll;

import com.allisighs.caesar.sjp.config.SjpConfig;
import com.allisighs.caesar.sjp.config.SjpConfig.TrollProfile;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class TrollHandler {

    private static TrollHandler INSTANCE;
    public static TrollHandler get() { return INSTANCE; }
    public TrollHandler() { INSTANCE = this; }

    private final Random rnd = new Random();
    private long lastTroll = 0L;
    private static final long MIN_GAP_MS = 8000L;
    private static final String MARKER = "тест";

    private String pendingSend = null;
    private long sendAt = 0L;

    public TrollProfile learnTarget = null;
    private long learnUntil = 0L;
    private boolean learnSent = false;
    private long learnSendAt = 0L;

    public void startLearn(TrollProfile pr) {
        learnTarget = pr;
        learnSent = false;
        learnSendAt = System.currentTimeMillis() + 300;
        learnUntil = 0L;
    }

    public boolean isLearning() { return learnTarget != null; }

    private String strip(String s) {
        return s == null ? "" : s.replaceAll("(?i)\u00a7[0-9A-FK-OR]", "");
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();

        if (isLearning() && !learnSent && now >= learnSendAt) {
            learnSent = true;
            learnUntil = now + 4000;
            try { mc.player.sendChatMessage(MARKER); } catch (Throwable ignored) {}
        }
        if (isLearning() && learnSent && now > learnUntil) {
            TrollProfile t = learnTarget;
            learnTarget = null;
            notify(mc, "\u00a7c[Тролль] не поймал сообщение. Попробуй ещё раз на этой игре.");
        }
        if (pendingSend != null && now >= sendAt) {
            String msg = pendingSend;
            pendingSend = null;
            try { mc.player.sendChatMessage(msg); } catch (Throwable ignored) {}
        }
    }

    private void notify(Minecraft mc, String msg) {
        if (mc.ingameGUI != null)
            mc.ingameGUI.getChatGUI().printChatMessage(new net.minecraft.util.text.TextComponentString(msg));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent e) {
        if (e.getMessage() == null) return;
        if (e.getType() == net.minecraft.util.text.ChatType.GAME_INFO) return;

        String raw = strip(e.getMessage().getUnformattedText());
        if (raw.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        String self = mc.player != null ? mc.player.getName() : "";

        boolean privateMsg = isPrivate(raw);

        if (isLearning() && learnSent) {
            String low = raw.toLowerCase();
            int idx = low.indexOf(MARKER);
            if (idx >= 0 && !privateMsg) {
                learnTarget.sep = detectSep(raw, idx);
                learnTarget.prefix = raw.substring(0, idx).trim();
                TrollProfile done = learnTarget;
                learnTarget = null;
                SjpConfig.get().save();
                e.setCanceled(true);
                notify(mc, "\u00a7a[Тролль] профиль \u00a7f" + done.name
                        + "\u00a7a обучен! ловлю текст после \u00a7f\"" + done.sep + "\"");
                return;
            }
            if (idx >= 0 && !privateMsg) { e.setCanceled(true); return; }
        }

        SjpConfig cfg = SjpConfig.get();
        if (!cfg.modEnabled || !cfg.trollEnabled) return;
        if (cfg.trollProfiles.isEmpty()) return;
        if (privateMsg) return;
        if (!self.isEmpty() && raw.contains(self)) return;

        long now = System.currentTimeMillis();
        if (now - lastTroll < MIN_GAP_MS) return;
        int chance = Math.max(1, Math.min(100, cfg.trollChance));
        if (rnd.nextInt(100) >= chance) return;

        for (TrollProfile pr : cfg.trollProfiles) {
            if (!pr.enabled) continue;
            String content = extract(raw, pr);
            if (content == null) continue;
            content = content.trim();
            if (content.isEmpty() || content.startsWith("/")) continue;
            if (content.length() > 100) continue;
            pendingSend = content;
            sendAt = now + 400 + rnd.nextInt(900);
            lastTroll = now;
            return;
        }
    }

    private boolean isPrivate(String raw) {
        if (raw == null) return false;
        if (raw.contains(" -> ")) return true;
        if (raw.contains("-> Мне") || raw.contains("-> Me")) return true;
        if (raw.contains("[Я ") || raw.contains("[ЛС") || raw.contains("[L]") || raw.contains("[Л]")) return true;
        return false;
    }

    private String detectSep(String raw, int markerIdx) {
        String[] seps = {"> ", ": ", "] "};
        String best = "";
        int bestPos = -1;
        for (String sp : seps) {
            int p = raw.lastIndexOf(sp, markerIdx);
            if (p >= 0 && p + sp.length() <= markerIdx && p > bestPos) { bestPos = p; best = sp; }
        }
        if (!best.isEmpty()) return best;
        if (markerIdx > 0) {
            char c = raw.charAt(markerIdx - 1);
            if (c == ' ') return " ";
        }
        return "";
    }

    private String extract(String raw, TrollProfile pr) {
        if (pr.sep != null && !pr.sep.isEmpty()) {
            int i = raw.lastIndexOf(pr.sep);
            if (i < 0) return null;
            return raw.substring(i + pr.sep.length());
        }
        if (pr.prefix != null && !pr.prefix.isEmpty()) {
            if (!raw.startsWith(pr.prefix)) return null;
            return raw.substring(pr.prefix.length());
        }
        return null;
    }
}
