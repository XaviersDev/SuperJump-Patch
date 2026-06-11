package com.allisighs.caesar.sjp;

import com.allisighs.caesar.sjp.config.SjpConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class ZoomHandler {

    public static KeyBinding zoomKey;
    private double target = 1.0;
    private double current = 1.0;
    private double prevCurrent = 1.0;
    private boolean active = false;
    private boolean savedSmooth = false;
    private boolean smoothApplied = false;

    public static void register() {
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        SjpConfig cfg = SjpConfig.get();
        if (mc.player == null) { reset(mc); return; }

        boolean down = cfg.modEnabled && cfg.zoomEnabled
                && mc.currentScreen == null
                && Keyboard.isKeyDown(cfg.zoomKey);

        if (down && !active) {
            active = true;
            if (cfg.zoomCinematic) {
                savedSmooth = mc.gameSettings.smoothCamera;
                mc.gameSettings.smoothCamera = true;
                smoothApplied = true;
            }
            target = Math.min(cfg.zoomMax, Math.max(2.0, cfg.zoomSpeed * 2.0));
        } else if (!down && active) {
            active = false;
            target = 1.0;
            if (smoothApplied) {
                mc.gameSettings.smoothCamera = savedSmooth;
                smoothApplied = false;
            }
        }

        prevCurrent = current;
        double speed = 0.35 + cfg.zoomSpeed * 0.12;
        if (speed > 0.9) speed = 0.9;
        current += (target - current) * speed;
        if (Math.abs(current - target) < 0.01) current = target;
    }

    private void reset(Minecraft mc) {
        active = false;
        target = 1.0;
        current = 1.0;
        prevCurrent = 1.0;
        if (smoothApplied && mc.gameSettings != null) {
            mc.gameSettings.smoothCamera = savedSmooth;
            smoothApplied = false;
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        SjpConfig cfg = SjpConfig.get();
        if (!active) return;
        int dw = e.getDwheel();
        if (dw != 0) {
            double step = target > 8 ? 2.0 : 1.0;
            if (dw > 0) target += step;
            else target -= step;
            if (target < 1.5) target = 1.5;
            if (target > cfg.zoomMax) target = cfg.zoomMax;
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onFov(EntityViewRenderEvent.FOVModifier e) {
        if (current <= 1.001) return;
        float pt = (float) Minecraft.getMinecraft().getRenderPartialTicks();
        double interp = prevCurrent + (current - prevCurrent) * pt;
        if (interp < 1.001) return;
        e.setFOV((float) (e.getFOV() / interp));
    }
}
