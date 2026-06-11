package com.allisighs.caesar.sjp.pvp;

import com.allisighs.caesar.sjp.config.SjpConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EntityCuller {

    private static final double NEAR = 6.0;
    private static final double FAR = 128.0;

    @SubscribeEvent
    public void onRenderPre(RenderLivingEvent.Pre<EntityLivingBase> e) {
        SjpConfig cfg = SjpConfig.get();
        if (!cfg.modEnabled || !cfg.cullEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;
        EntityLivingBase ent = e.getEntity();
        if (ent == null || ent == view) return;

        double dist = view.getDistance(ent);
        if (dist <= NEAR) return;
        if (dist >= FAR) { e.setCanceled(true); return; }

        float pt = mc.getRenderPartialTicks();
        Vec3d eyes = view.getPositionEyes(pt);

        if (cfg.cullFov && !inView(view, ent, eyes, pt)) {
            e.setCanceled(true);
            return;
        }
        if (cfg.cullWalls && occluded(mc, ent, eyes, pt)) {
            e.setCanceled(true);
        }
    }

    private boolean inView(Entity view, EntityLivingBase ent, Vec3d eyes, float pt) {
        Vec3d look = view.getLook(pt).normalize();
        double ex = ent.lastTickPosX + (ent.posX - ent.lastTickPosX) * pt;
        double ey = ent.lastTickPosY + (ent.posY - ent.lastTickPosY) * pt + ent.height * 0.5;
        double ez = ent.lastTickPosZ + (ent.posZ - ent.lastTickPosZ) * pt;
        Vec3d dir = new Vec3d(ex - eyes.x, ey - eyes.y, ez - eyes.z).normalize();
        double dot = look.dotProduct(dir);
        return dot > 0.12;
    }

    private boolean occluded(Minecraft mc, EntityLivingBase ent, Vec3d eyes, float pt) {
        double ex = ent.lastTickPosX + (ent.posX - ent.lastTickPosX) * pt;
        double ey = ent.lastTickPosY + (ent.posY - ent.lastTickPosY) * pt;
        double ez = ent.lastTickPosZ + (ent.posZ - ent.lastTickPosZ) * pt;
        double h = ent.height;
        double w = ent.width * 0.5;

        Vec3d[] targets = new Vec3d[]{
                new Vec3d(ex, ey + h * 0.1, ez),
                new Vec3d(ex, ey + h * 0.5, ez),
                new Vec3d(ex, ey + h * 0.9, ez),
                new Vec3d(ex + w, ey + h * 0.5, ez),
                new Vec3d(ex - w, ey + h * 0.5, ez),
                new Vec3d(ex, ey + h * 0.5, ez + w),
                new Vec3d(ex, ey + h * 0.5, ez - w)
        };

        for (Vec3d t : targets) {
            if (!blocked(mc, eyes, t)) return false;
        }
        return true;
    }

    private boolean blocked(Minecraft mc, Vec3d from, Vec3d to) {
        RayTraceResult r = mc.world.rayTraceBlocks(from, to, false, true, false);
        return r != null && r.typeOfHit == RayTraceResult.Type.BLOCK;
    }
}
