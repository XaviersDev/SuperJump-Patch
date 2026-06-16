package com.allisighs.caesar.sjp.chat;

import com.allisighs.caesar.sjp.config.SjpConfig;
import com.allisighs.caesar.sjp.config.SjpConfig.GgSet;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Auto-GG: when a Hide-and-Seek round ends, automatically sends a chosen "set"
 * of gg-style messages — the same lines players already type by hand to earn
 * karma. No gameplay advantage; just chat automation.
 *
 * A set holds up to 3 lines. Lines in a set are sent one after another with a
 * 55 ms gap (the minimum the server allows for three messages). Which set is
 * picked each round follows the global order (sequential / random); the order
 * of lines inside a set follows that set's own rule.
 */
public class AutoGgHandler {

    private final Random rnd = new Random();

    // queue of lines waiting to be sent, each with its own fire time
    private static class Pending {
        long fireAt;
        String text;
        Pending(long fireAt, String text) { this.fireAt = fireAt; this.text = text; }
    }
    private final ArrayDeque<Pending> queue = new ArrayDeque<Pending>();

    private long lastTrigger = 0L;
    private static final long COOLDOWN_MS = 8000L;

    private boolean isRoundEnd(String clean) {
        String s = clean.toLowerCase();
        boolean pr = s.contains("прятки") || s.contains("[прятки]");
        boolean win = s.contains("одержали победу") || s.contains("победил");
        return pr && win;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        SjpConfig cfg = SjpConfig.get();
        if (!cfg.modEnabled || !cfg.autoGg) return;
        if (e.getType() == ChatType.GAME_INFO) return;
        if (e.getMessage() == null) return;

        String full = e.getMessage().getUnformattedText();
        if (full == null || full.isEmpty()) full = e.getMessage().getFormattedText();
        if (full == null || full.isEmpty()) return;

        String clean = full.replaceAll("(?i)\u00a7[0-9A-FK-OR]", "");
        if (!isRoundEnd(clean)) return;

        long now = System.currentTimeMillis();
        if (now - lastTrigger < COOLDOWN_MS) return;
        lastTrigger = now;

        scheduleSet(cfg, now);
    }

    private void scheduleSet(SjpConfig cfg, long now) {
        GgSet set = pickSet(cfg);
        if (set == null || set.messages == null || set.messages.isEmpty()) return;

        List<String> lines = orderedLines(set);
        if (lines.isEmpty()) return;

        // initial human-like delay before the first line, then 55 ms steps
        int min = Math.max(0, cfg.autoGgMinDelay);
        int max = Math.max(min + 1, cfg.autoGgMaxDelay);
        long first = now + min + (long) (rnd.nextDouble() * (max - min));

        queue.clear();
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i);
            if (t == null || t.trim().isEmpty()) continue;
            long at = first + (long) i * SjpConfig.GG_LINE_DELAY_MS;
            queue.addLast(new Pending(at, t));
        }
    }

    // choose which set to use this round, per the global order rule
    private GgSet pickSet(SjpConfig cfg) {
        List<GgSet> sets = cfg.autoGgSets;
        if (sets == null || sets.isEmpty()) return null;
        if (cfg.autoGgRandom) {
            return sets.get(rnd.nextInt(sets.size()));
        } else {
            if (cfg.autoGgSeqIndex >= sets.size()) cfg.autoGgSeqIndex = 0;
            GgSet s = sets.get(cfg.autoGgSeqIndex);
            cfg.autoGgSeqIndex = (cfg.autoGgSeqIndex + 1) % sets.size();
            cfg.save();
            return s;
        }
    }

    // order the lines inside a set per the set's own rule, capped at GG_MAX_LINES
    private List<String> orderedLines(GgSet set) {
        List<String> src = new ArrayList<String>(set.messages);
        if (set.randomLine) Collections.shuffle(src, rnd);
        while (src.size() > SjpConfig.GG_MAX_LINES) src.remove(src.size() - 1);
        return src;
    }

    private long lastSent = 0L;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (queue.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) { queue.clear(); return; }

        long now = System.currentTimeMillis();
        // send at most enough lines such that consecutive sends stay >= 55 ms
        // apart on the wall clock, never faster than the server allows
        if (now < queue.peekFirst().fireAt) return;
        if (now - lastSent < SjpConfig.GG_LINE_DELAY_MS) return;
        Pending p = queue.pollFirst();
        try { mc.player.sendChatMessage(p.text); } catch (Throwable ignored) {}
        lastSent = now;
    }
}
