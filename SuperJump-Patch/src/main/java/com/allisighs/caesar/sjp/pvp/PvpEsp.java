package com.allisighs.caesar.sjp.pvp;

import com.allisighs.caesar.sjp.config.SjpConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

public class PvpEsp {

    private static final double REACH = 3.0;
    private final Set<Integer> glowing = new HashSet<Integer>();

    private boolean valid(Minecraft mc, EntityPlayer p) {
        if (p == mc.player) return false;
        if (!p.isEntityAlive()) return false;
        if (p.isInvisible()) return false;
        if (p.isSpectator()) return false;
        if (p.isInvisibleToPlayer(mc.player)) return false;
        return true;
    }

    private boolean inReach(Minecraft mc, EntityPlayer p) {
        return mc.player.getDistance(p) <= REACH + 0.3;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        SjpConfig cfg = SjpConfig.get();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        Set<Integer> nowGlow = new HashSet<Integer>();
        if (cfg.modEnabled && cfg.pvpEsp && !cfg.pvpHitbox) {
            for (EntityPlayer p : mc.world.playerEntities) {
                if (!valid(mc, p)) continue;
                if (inReach(mc, p)) { p.setGlowing(true); nowGlow.add(p.getEntityId()); }
            }
        }
        for (Integer id : glowing) {
            if (!nowGlow.contains(id)) {
                net.minecraft.entity.Entity ent = mc.world.getEntityByID(id);
                if (ent != null) ent.setGlowing(false);
            }
        }
        glowing.clear();
        glowing.addAll(nowGlow);
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        SjpConfig cfg = SjpConfig.get();
        if (!cfg.modEnabled || !cfg.pvpEsp || !cfg.pvpHitbox) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        float pt = event.getPartialTicks();
        double px = mc.player.lastTickPosX + (mc.player.posX - mc.player.lastTickPosX) * pt;
        double py = mc.player.lastTickPosY + (mc.player.posY - mc.player.lastTickPosY) * pt;
        double pz = mc.player.lastTickPosZ + (mc.player.posZ - mc.player.lastTickPosZ) * pt;

        float r = ((cfg.pvpColor >> 16) & 0xFF) / 255f;
        float g = ((cfg.pvpColor >> 8) & 0xFF) / 255f;
        float b = (cfg.pvpColor & 0xFF) / 255f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-px, -py, -pz);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.glLineWidth(2.0f);

        for (EntityPlayer p : mc.world.playerEntities) {
            if (!valid(mc, p)) continue;
            if (!inReach(mc, p)) continue;
            double ex = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
            double ey = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
            double ez = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;
            AxisAlignedBB box = p.getEntityBoundingBox()
                    .offset(-p.posX, -p.posY, -p.posZ).offset(ex, ey, ez).grow(0.05);
            RenderGlobal.drawSelectionBoundingBox(box, r, g, b, 0.9f);
        }

        GlStateManager.glLineWidth(1.0f);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
