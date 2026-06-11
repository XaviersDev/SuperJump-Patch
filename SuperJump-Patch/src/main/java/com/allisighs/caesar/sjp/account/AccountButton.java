package com.allisighs.caesar.sjp.account;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AccountButton {
    private static final int ID_ACC = 777001;
    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e) {
        if (!(e.getGui() instanceof GuiMultiplayer)) return;
        int w = e.getGui().width;
        e.getButtonList().add(new GuiButton(ID_ACC, w - 90, 6, 80, 20, "Аккаунты"));
    }
    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre e) {
        if (!(e.getGui() instanceof GuiMultiplayer)) return;
        if (e.getButton().id == ID_ACC)
            Minecraft.getMinecraft().displayGuiScreen(new AccountsGui(e.getGui()));
    }
}
