package com.allisighs.caesar.sjp.stats;

import com.allisighs.caesar.sjp.config.SjpConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class StatChatHandler {
    private int counter = 0;
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (Minecraft.getMinecraft().player == null) return;
        if (!SjpConfig.get().modEnabled) return;
        counter++;
        if (counter % 20 != 0) return;
        StatManager.INST.tick();
    }
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (!StatManager.INST.isCapturing()) return;
        if (e.getType() == ChatType.GAME_INFO) return;
        if (e.getMessage() == null) return;
        String full = e.getMessage().getUnformattedText();
        if (full == null || full.isEmpty()) full = e.getMessage().getFormattedText();
        if (full == null || full.isEmpty()) return;
        if (StatManager.INST.handleLine(full)) e.setCanceled(true);
    }
}
