package com.allisighs.caesar.sjp.account;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

public class AccountsGui extends GuiScreen {

    private final GuiScreen parent;
    private GuiTextField nameField;
    private int scroll = 0;
    private static final int PER_PAGE = 6;

    public AccountsGui(GuiScreen parent) { this.parent = parent; }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private int maxScroll() {
        int n = AccountManager.list().size();
        return Math.max(0, n - PER_PAGE);
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        if (scroll > maxScroll()) scroll = maxScroll();
        if (scroll < 0) scroll = 0;

        nameField = new GuiTextField(0, fontRenderer, width / 2 - 150, 40, 220, 20);
        nameField.setMaxStringLength(16);
        this.buttonList.add(new GuiButton(1, width / 2 + 75, 40, 75, 20, "Добавить"));

        List<String> accs = AccountManager.list();
        int y = 75;
        int shown = 0;
        for (int i = scroll; i < accs.size() && shown < PER_PAGE; i++, shown++) {
            String nick = accs.get(i);
            boolean current = nick.equalsIgnoreCase(AccountManager.currentName());
            this.buttonList.add(new GuiButton(1000 + i, width / 2 - 150, y, 175, 20, (current ? "\u00a7a" : "") + nick));
            this.buttonList.add(new GuiButton(2000 + i, width / 2 + 30, y, 120, 20, "Войти"));
            this.buttonList.add(new GuiButton(3000 + i, width / 2 + 155, y, 20, 20, "x"));
            y += 24;
        }

        if (scroll > 0)
            this.buttonList.add(new GuiButton(5, width / 2 + 158, 75, 16, 20, "\u25b2"));
        if (scroll < maxScroll())
            this.buttonList.add(new GuiButton(6, width / 2 + 158, 75 + (PER_PAGE - 1) * 24, 16, 20, "\u25bc"));

        this.buttonList.add(new GuiButton(9, width / 2 - 100, height - 30, 200, 20, "Готово"));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 1) { AccountManager.add(nameField.getText()); nameField.setText(""); scroll = maxScroll(); initGui(); }
        else if (b.id == 9) { mc.displayGuiScreen(parent); }
        else if (b.id == 5) { scroll = Math.max(0, scroll - 1); initGui(); }
        else if (b.id == 6) { scroll = Math.min(maxScroll(), scroll + 1); initGui(); }
        else if (b.id >= 2000 && b.id < 3000) { AccountManager.switchTo(AccountManager.list().get(b.id - 2000)); initGui(); }
        else if (b.id >= 3000 && b.id < 4000) { AccountManager.remove(AccountManager.list().get(b.id - 3000)); initGui(); }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (nameField.textboxKeyTyped(c, key)) return;
        super.keyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        nameField.mouseClicked(mx, my, btn);
        super.mouseClicked(mx, my, btn);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dw = Mouse.getEventDWheel();
        if (dw != 0) {
            if (dw > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(maxScroll(), scroll + 1);
            initGui();
        }
    }

    @Override
    public void updateScreen() { nameField.updateCursorCounter(); }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        this.drawDefaultBackground();
        drawCenteredString(fontRenderer, "\u00a7e\u00a7lАккаунты (оффлайн)", width / 2, 18, 0xFFFF55);
        nameField.drawTextBox();
        if (AccountManager.list().isEmpty())
            drawCenteredString(fontRenderer, "\u00a77Добавь ник выше", width / 2, 80, 0xAAAAAA);
        int n = AccountManager.list().size();
        if (n > PER_PAGE)
            drawCenteredString(fontRenderer, "\u00a78" + (scroll + 1) + "-"
                    + Math.min(n, scroll + PER_PAGE) + " из " + n, width / 2, height - 48, 0x888888);
        drawString(fontRenderer, "\u00a77Текущий: \u00a7f" + AccountManager.currentName(), width / 2 - 150, height - 50, 0xFFFFFF);
        super.drawScreen(mx, my, pt);
    }
}
