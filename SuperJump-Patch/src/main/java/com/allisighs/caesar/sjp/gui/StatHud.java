package com.allisighs.caesar.sjp.gui;

import com.allisighs.caesar.sjp.config.SjpConfig;
import com.allisighs.caesar.sjp.stats.GameStat;
import com.allisighs.caesar.sjp.stats.StatManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class StatHud {
    private final Minecraft mc = Minecraft.getMinecraft();
    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.currentScreen != null) return;
        draw();
    }
    private void draw() {
        SjpConfig cfg = SjpConfig.get();
        if (!cfg.modEnabled || !cfg.hudEnabled) return;
        if (mc.player == null || mc.gameSettings.showDebugInfo) return;
        String nick = StatManager.INST.playerName;
        if (nick == null || nick.trim().isEmpty()) nick = mc.player.getName();
        nick = nick.trim();
        FontRenderer fr = mc.fontRenderer;
        java.util.List<GameStat> shown = new java.util.ArrayList<GameStat>();
        for (GameStat gs : StatManager.INST.stats) {
            if (!cfg.isGameOn(gs.name)) continue;
            if (cfg.hideNegativeKo && gs.ko < 0) continue;
            shown.add(gs);
        }
        if (cfg.sortByScore) {
            java.util.Collections.sort(shown, new java.util.Comparator<GameStat>() {
                @Override public int compare(GameStat a, GameStat b) { return Integer.compare(b.ko, a.ko); }
            });
        }
        int rows = 2 + (shown.isEmpty() ? 1 : shown.size());
        int contentH = (int) (rows * 11 * cfg.hudScale);
        int contentW = (int) (160 * cfg.hudScale);
        ScaledResolution sr = new ScaledResolution(mc);
        int[] pos = cfg.resolveHud(sr.getScaledWidth(), sr.getScaledHeight(), contentW, contentH);
        float scale = cfg.hudScale;
        GlStateManager.pushMatrix();
        GlStateManager.translate(pos[0], pos[1], 0);
        GlStateManager.scale(scale, scale, scale);
        int y = 0;
        fr.drawStringWithShadow(nick + "  |  " + StatManager.INST.totalKo, 0, y, cfg.colorTitle);
        y += 11;
        fr.drawStringWithShadow("\u00a77------------------------", 0, y, 0x555555);
        y += 11;
        if (shown.isEmpty()) {
            if (StatManager.INST.isCapturing()) fr.drawStringWithShadow("\u00a77Обновление...", 0, y, cfg.colorGame);
            else fr.drawStringWithShadow("\u00a78ожидание...", 0, y, cfg.colorGame);
        } else {
            for (GameStat gs : shown) {
                int valColor = valueColor(cfg, gs.ko);
                fr.drawStringWithShadow(gs.name, 0, y, cfg.colorGame);
                String val = String.valueOf(gs.ko);
                int w = fr.getStringWidth(val);
                fr.drawStringWithShadow(val, 150 - w, y, valColor);
                y += 11;
            }
        }
        GlStateManager.popMatrix();
    }
    private int valueColor(SjpConfig cfg, int ko) {
        if (ko < 0) return cfg.colorNegative;
        if (!cfg.colorTiers) return cfg.colorValue;
        if (ko >= cfg.tierHigh) return 0x55FF55;
        if (ko >= cfg.tierMid) return 0xFFFF55;
        return 0xAAAAAA;
    }
}
