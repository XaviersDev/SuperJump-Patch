package com.allisighs.caesar.sjp.pvp;

import com.allisighs.caesar.sjp.config.SjpConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PvpEsp {

    private static final double REACH = 3.0;

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

    
    
    
    private boolean visible(Minecraft mc, EntityPlayer p, float pt) {
        Vec3d eyes = mc.player.getPositionEyes(pt);
        double ex = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
        double ey = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
        double ez = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;
        double h = p.height;
        double w = p.width * 0.5;

        Vec3d[] targets = new Vec3d[]{
                new Vec3d(ex, ey + h * 0.1, ez),
                new Vec3d(ex, ey + h * 0.5, ez),
                new Vec3d(ex, ey + h * 0.9, ez),
                new Vec3d(ex, ey + h + 0.75, ez), 
                new Vec3d(ex + w, ey + h * 0.5, ez),
                new Vec3d(ex - w, ey + h * 0.5, ez),
                new Vec3d(ex, ey + h * 0.5, ez + w),
                new Vec3d(ex, ey + h * 0.5, ez - w)
        };

        for (Vec3d t : targets) {
            RayTraceResult r = mc.world.rayTraceBlocks(eyes, t, false, true, false);
            if (r == null || r.typeOfHit != RayTraceResult.Type.BLOCK) {
                return true; 
            }
        }
        return false; 
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        SjpConfig cfg = SjpConfig.get();
        if (!cfg.modEnabled || !cfg.pvpEsp) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        float pt = event.getPartialTicks();
        double px = mc.player.lastTickPosX + (mc.player.posX - mc.player.lastTickPosX) * pt;
        double py = mc.player.lastTickPosY + (mc.player.posY - mc.player.lastTickPosY) * pt;
        double pz = mc.player.lastTickPosZ + (mc.player.posZ - mc.player.lastTickPosZ) * pt;

        float r = ((cfg.pvpColor >> 16) & 0xFF) / 255f;
        float g = ((cfg.pvpColor >> 8) & 0xFF) / 255f;
        float b = (cfg.pvpColor & 0xFF) / 255f;

        
        float t = (mc.world.getTotalWorldTime() + pt);
        float bob = (float) Math.sin(t * 0.15f) * 0.10f;        
        float pulse = 1.0f + (float) Math.sin(t * 0.25f) * 0.12f; 

        GlStateManager.pushMatrix();
        GlStateManager.translate(-px, -py, -pz);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        
        
        
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        for (EntityPlayer p : mc.world.playerEntities) {
            if (!valid(mc, p)) continue;
            if (!inReach(mc, p)) continue;
            if (!visible(mc, p, pt)) continue;

            double ex = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
            double ey = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
            double ez = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;

            if (cfg.pvpArrows) {
                drawArrows(mc, ex, ey + p.height + 0.75 + bob, ez, pulse, r, g, b);
            } else {
                GlStateManager.glLineWidth(2.0f);
                AxisAlignedBB box = p.getEntityBoundingBox()
                        .offset(-p.posX, -p.posY, -p.posZ).offset(ex, ey, ez).grow(0.05);
                RenderGlobal.drawSelectionBoundingBox(box, r, g, b, 0.9f);
                GlStateManager.glLineWidth(1.0f);
            }
        }

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    
    
    private void drawArrows(Minecraft mc, double x, double y, double z,
                            float scale, float r, float g, float b) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0, 1, 0);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1, 0, 0);
        GlStateManager.scale(scale, scale, scale);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        
        
        float halfW = 0.34f;   
        float legT = 0.16f;    
        float drop = 0.26f;    
        float gap = 0.20f;     

        for (int i = 0; i < 2; i++) {
            float topY = -i * gap;            
            chevron(buf, tess, halfW, legT, drop, topY, r, g, b);
        }

        GlStateManager.popMatrix();
    }

    private void chevron(BufferBuilder buf, Tessellator tess,
                         float halfW, float legT, float drop, float topY,
                         float r, float g, float b) {
        
        float shoulderY = topY;
        float tipY = topY - drop;

        
        quad(buf, tess,
                -halfW, shoulderY,
                -halfW + legT, shoulderY,
                0f + legT * 0.5f, tipY,
                0f - legT * 0.5f, tipY,
                r, g, b);
        
        quad(buf, tess,
                halfW - legT, shoulderY,
                halfW, shoulderY,
                0f + legT * 0.5f, tipY,
                0f - legT * 0.5f, tipY,
                r, g, b);
    }

    private void quad(BufferBuilder buf, Tessellator tess,
                      float x1, float y1, float x2, float y2,
                      float x3, float y3, float x4, float y4,
                      float r, float g, float b) {
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buf.pos(x1, y1, 0).color(r, g, b, 0.95f).endVertex();
        buf.pos(x2, y2, 0).color(r, g, b, 0.95f).endVertex();
        buf.pos(x3, y3, 0).color(r, g, b, 0.95f).endVertex();
        buf.pos(x4, y4, 0).color(r, g, b, 0.95f).endVertex();
        tess.draw();
    }
}
