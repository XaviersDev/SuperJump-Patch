package com.allisighs.caesar.sjp.troll;

import com.allisighs.caesar.sjp.config.SjpConfig;
import com.allisighs.caesar.sjp.config.SjpConfig.TrollProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class TrollRenameGui extends GuiScreen {

    private static final ResourceLocation SIGN_TEX = new ResourceLocation("textures/entity/sign.png");

    private final TrollProfile profile;
    private GuiTextField field;

    public TrollRenameGui(TrollProfile profile) { this.profile = profile; }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int fw = 180;
        field = new GuiTextField(0, fontRenderer, width / 2 - fw / 2, height / 2 - 4, fw, 12);
        field.setMaxStringLength(24);
        field.setEnableBackgroundDrawing(false);
        field.setText(profile.name == null ? "" : profile.name);
        field.setFocused(true);
        field.setCursorPositionEnd();
        this.buttonList.add(new GuiButton(1, width / 2 - 100, height / 2 + 40, 200, 20, "Готово"));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 1) save();
    }

    private void save() {
        String t = field.getText().trim();
        if (!t.isEmpty()) profile.name = t;
        SjpConfig.get().save();
        mc.displayGuiScreen(new TrollGui());
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) { mc.displayGuiScreen(new TrollGui()); return; }
        if (key == 28 || key == 156) { save(); return; }
        if (field.textboxKeyTyped(c, key)) return;
        super.keyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        field.mouseClicked(mx, my, btn);
        super.mouseClicked(mx, my, btn);
    }

    @Override
    public void updateScreen() { field.updateCursorCounter(); }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        this.drawDefaultBackground();
        drawCenteredString(fontRenderer, "\u00a7e\u00a7lНазываем твой профиль :)", width / 2, height / 2 - 60, 0xFFFF55);
        drawSignPlate();
        String shown = field.getText();
        boolean blink = field.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0;
        drawCenteredString(fontRenderer, "\u00a7f" + shown + (blink ? "_" : ""), width / 2, height / 2 - 4, 0xFFFFFF);
        drawCenteredString(fontRenderer, "\u00a78Если дописал, нажимай энтер или кнопку, а esc - отмена", width / 2, height / 2 + 66, 0x888888);
        super.drawScreen(mx, my, pt);
    }

    private void drawSignPlate() {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            mc.getTextureManager().bindTexture(SIGN_TEX);
            GlStateManager.color(1f, 1f, 1f, 1f);
            int w = 220, h = 64;
            int x = width / 2 - w / 2;
            int y = height / 2 - 26;
            drawScaledCustomSizeModalRect(x, y, 0, 0, 52, 28, w, h, 64, 32);
        } catch (Throwable ignored) {}
    }
}
