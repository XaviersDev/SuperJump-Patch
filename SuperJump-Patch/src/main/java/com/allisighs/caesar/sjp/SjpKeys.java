package com.allisighs.caesar.sjp;

import com.allisighs.caesar.sjp.gui.SjpGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class SjpKeys {

    public static KeyBinding openMenu;
    public static boolean openRequested = false;

    public static void register() {
        openMenu = new KeyBinding("Открыть меню Teslive", Keyboard.KEY_RSHIFT, "Teslive Community Hub");
        ClientRegistry.registerKeyBinding(openMenu);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (openMenu != null && openMenu.isPressed()) openRequested = true;
    }

    @SubscribeEvent
    public void onChatSend(ClientChatEvent e) {
        String msg = e.getMessage();
        if (msg == null) return;
        String low = msg.trim().toLowerCase();
        if (low.equals("/tch") || low.equals("/еср") || low.equals("/мод") || low.equals("/теслив") || low.equals("/тесливе") || low.equals("/teslive")) {
            e.setCanceled(true);
            openRequested = true;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!openRequested) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        if (mc.currentScreen != null) return;
        openRequested = false;
        try { mc.displayGuiScreen(new SjpGui()); } catch (Throwable t) { t.printStackTrace(); }
    }
}
