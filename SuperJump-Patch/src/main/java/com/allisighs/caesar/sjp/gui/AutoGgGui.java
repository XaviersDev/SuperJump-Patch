package com.allisighs.caesar.sjp.gui;

import com.allisighs.caesar.sjp.config.SjpConfig;
import com.allisighs.caesar.sjp.config.SjpConfig.GgSet;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoGgGui extends GuiScreen {

    private final SjpConfig cfg = SjpConfig.get();

    private static final int VIEW_SETS = 0, VIEW_EDIT = 1;
    private int view = VIEW_SETS;

    
    
    
    private static final int ADD_MESSAGE = 0, ADD_SET = 1;
    private int addMode = ADD_MESSAGE;

    private int editingSet = -1;     
    private int editingLine = -1;    
    private int editingSingle = -1;  

    private GuiTextField field;      

    private static final String[] SYMBOLS = {
            "\u2764", "\u2665", "\u2661", "\u2767", "\u2729", "\u2698", "\u2722",
            "\u273f", "\u273e", "\u2740", "\u2766", "\u2619", "\u266a", "\u266b",
            "\u2606", "\u2605", "\u272a", "\u2b50", "\u2727", "\u272b", "\u2728",
            "\u00bb", "\u00ab", "\u2192", "\u2190", "\u279c", "\u2756", "\u25c6",
            "\u2570", "\u256d", "\u2500", "\u00b7", "\u2022", "\u00d7", "\u221e", "\u273d"
    };
    private static final int SYMS_PER_ROW = 12;
    private static final int CELL = 18;

    
    private int iconY;
    private int[] iconX = new int[3];
    private static final int ICON_BOX = 24, ICON_GAP = 4;
    private int fieldX, fieldY, fieldW;
    private int symX, symY;
    private int listX, listY, listW;
    private static final int ROW_H = 14, ROW_GAP = 2;

    private static final int ID_ADD = 4, ID_CLEAR = 5;
    private static final int ID_MODE_MSG = 6, ID_MODE_SET = 7;

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int groupW = ICON_BOX * 3 + ICON_GAP * 2;
        int startX = width / 2 - groupW / 2;
        iconY = 22;
        for (int i = 0; i < 3; i++) iconX[i] = startX + i * (ICON_BOX + ICON_GAP);

        fieldW = 220;
        fieldX = width / 2 - fieldW / 2;
        fieldY = (view == VIEW_SETS) ? 78 : 64;
        field = new GuiTextField(0, fontRenderer, fieldX + 2, fieldY + 4, fieldW - 4, 12);
        field.setMaxStringLength(96);
        field.setEnableBackgroundDrawing(false);
        field.setFocused(true);

        this.buttonList.add(new GuiButton(ID_ADD, fieldX + fieldW + 4, fieldY, 20, 20, "+"));
        this.buttonList.add(new GuiButton(ID_CLEAR, fieldX - 24, fieldY, 20, 20, "\u2716"));

        
        if (view == VIEW_SETS) {
            int bw = 108;
            int bx = width / 2 - bw - 2;
            int by = 48;
            this.buttonList.add(new GuiButton(ID_MODE_MSG, bx, by, bw, 18,
                    (addMode == ADD_MESSAGE ? "\u00a7a\u2714 " : "\u00a77") + "Сообщение"));
            this.buttonList.add(new GuiButton(ID_MODE_SET, width / 2 + 2, by, bw, 18,
                    (addMode == ADD_SET ? "\u00a7a\u2714 " : "\u00a77") + "Набор (папка)"));
        }

        int kbW = SYMS_PER_ROW * CELL;
        symX = width / 2 - kbW / 2;
        symY = fieldY + 40;
        int rows = (int) Math.ceil(SYMBOLS.length / (double) SYMS_PER_ROW);

        listW = 300;
        listX = width / 2 - listW / 2;
        
        boolean kb = (view == VIEW_EDIT) || (view == VIEW_SETS && addMode == ADD_MESSAGE);
        listY = kb ? symY + rows * CELL + 24 : fieldY + 44;
    }

    

    private ItemStack iconStack(int i) {
        if (view == VIEW_SETS) {
            switch (i) {
                case 0: return decorate(new ItemStack(cfg.autoGg ? Items.NETHER_STAR : Items.GUNPOWDER), cfg.autoGg);
                case 1: return decorate(new ItemStack(cfg.autoGgRandom ? Items.ENDER_PEARL : Items.COMPASS), cfg.autoGgRandom);
                default: return decorate(new ItemStack(Items.BOOK), false); 
            }
        } else {
            GgSet s = curSet();
            boolean rnd = s != null && s.randomLine;
            switch (i) {
                case 0: return decorate(new ItemStack(rnd ? Items.ENDER_PEARL : Items.COMPASS), rnd);
                case 1: return decorate(new ItemStack(Items.NAME_TAG), false);  
                default: return decorate(new ItemStack(Items.ARROW), false);    
            }
        }
    }

    private String[] iconTooltip(int i) {
        if (view == VIEW_SETS) {
            switch (i) {
                case 0: return cfg.autoGg
                        ? new String[]{"\u00a7a\u00a7lАвто-ГГ: включен", "\u00a77Шлёт набор в конце катки", "\u00a78нажми чтоб выключить"}
                        : new String[]{"\u00a7c\u00a7lАвто-ГГ: выключен", "\u00a78нажми чтоб включить"};
                case 1: return cfg.autoGgRandom
                        ? new String[]{"\u00a7b\u00a7lНаборы:рандом", "\u00a77Случайный набор каждый раз", "\u00a78нажми чтоб по порядку"}
                        : new String[]{"\u00a7b\u00a7lНаборы:по порядку", "\u00a77Наборы идут по кругу", "\u00a78нажми чтоб рандом"};
                default: return new String[]{"\u00a7e\u00a7lЗакрыть", "\u00a77В главное меню"};
            }
        } else {
            GgSet s = curSet();
            boolean rnd = s != null && s.randomLine;
            switch (i) {
                case 0: return rnd
                        ? new String[]{"\u00a7b\u00a7lСтроки:рандом", "\u00a77Случайный порядок строк", "\u00a78нажми чтоб по порядку"}
                        : new String[]{"\u00a7b\u00a7lСтроки:по порядку", "\u00a77Строки шлются сверху вниз", "\u00a78нажми чтоб рандом"};
                case 1: return new String[]{"\u00a7e\u00a7lИмя набора", "\u00a77Печатай в поле и жми +", "\u00a77чтоб переименовать"};
                default: return new String[]{"\u00a7e\u00a7l\u00ab К списку наборов"};
            }
        }
    }

    private void clickIcon(int i) {
        if (view == VIEW_SETS) {
            if (i == 0) { cfg.autoGg = !cfg.autoGg; cfg.save(); }
            else if (i == 1) { cfg.autoGgRandom = !cfg.autoGgRandom; cfg.save(); }
            else { cfg.save(); mc.displayGuiScreen(new SjpGui()); }
        } else {
            GgSet s = curSet();
            if (i == 0) { if (s != null) { s.randomLine = !s.randomLine; cfg.save(); } }
            else if (i == 1) { /* rename: put set name into field */ if (s != null) { field.setText(s.name); editingLine = -2; field.setCursorPositionEnd(); field.setFocused(true); } }
            else { gotoSets(); }
        }
    }

    private ItemStack decorate(ItemStack st, boolean glow) {
        NBTTagCompound tag = st.hasTagCompound() ? st.getTagCompound() : new NBTTagCompound();
        if (glow) {
            NBTTagList ench = new NBTTagList();
            NBTTagCompound e = new NBTTagCompound();
            e.setShort("id", (short) 0); e.setShort("lvl", (short) 1);
            ench.appendTag(e); tag.setTag("ench", ench);
        }
        tag.setInteger("HideFlags", 127);
        st.setTagCompound(tag);
        return st;
    }

    

    private GgSet curSet() {
        if (editingSet < 0 || editingSet >= cfg.autoGgSets.size()) return null;
        return cfg.autoGgSets.get(editingSet);
    }

    private void gotoSets() {
        view = VIEW_SETS;
        editingSet = -1;
        editingLine = -1;
        editingSingle = -1;
        field.setText("");
        cfg.save();
        initGui();
    }

    private void gotoEdit(int idx) {
        if (idx < 0 || idx >= cfg.autoGgSets.size()) return;
        view = VIEW_EDIT;
        editingSet = idx;
        editingLine = -1;
        editingSingle = -1;
        field.setText("");
        initGui();
    }

    

    private void commitField() {
        String t = field.getText().trim();
        if (view == VIEW_SETS) {
            if (editingSingle >= 0) {
                
                if (editingSingle < cfg.autoGgSets.size()) {
                    GgSet s = cfg.autoGgSets.get(editingSingle);
                    if (t.isEmpty()) cfg.autoGgSets.remove(editingSingle);
                    else {
                        if (s.messages.isEmpty()) s.messages.add(t); else s.messages.set(0, t);
                        s.name = t; 
                    }
                    cfg.save();
                }
                editingSingle = -1;
                field.setText("");
                field.setFocused(true);
                return;
            }
            if (!t.isEmpty()) {
                if (addMode == ADD_MESSAGE) {
                    
                    GgSet s = new GgSet(t);
                    s.messages.add(t);
                    cfg.autoGgSets.add(s);
                    cfg.save();
                    field.setText("");
                    field.setFocused(true);
                } else {
                    
                    GgSet s = new GgSet(t);
                    cfg.autoGgSets.add(s);
                    cfg.save();
                    gotoEdit(cfg.autoGgSets.size() - 1);
                }
            }
        } else {
            GgSet s = curSet();
            if (s == null) return;
            if (editingLine == -2) {
                
                if (!t.isEmpty()) s.name = t;
                editingLine = -1;
            } else if (editingLine >= 0) {
                if (editingLine < s.messages.size()) {
                    if (t.isEmpty()) s.messages.remove(editingLine);
                    else s.messages.set(editingLine, t);
                }
                editingLine = -1;
            } else {
                
                if (!t.isEmpty() && s.messages.size() < SjpConfig.GG_MAX_LINES) s.messages.add(t);
            }
            field.setText("");
            field.setFocused(true);
            cfg.save();
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == ID_ADD) commitField();
        else if (b.id == ID_CLEAR) { field.setText(""); editingLine = -1; field.setFocused(true); }
        else if (b.id == ID_MODE_MSG) { addMode = ADD_MESSAGE; field.setFocused(true); initGui(); }
        else if (b.id == ID_MODE_SET) { addMode = ADD_SET; field.setFocused(true); initGui(); }
    }

    private void insertSymbol(String sym) { field.setFocused(true); field.writeText(sym); }

    

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) { 
            if (view == VIEW_EDIT) { gotoSets(); return; }
            cfg.save(); mc.displayGuiScreen(new SjpGui()); return;
        }
        if (key == 28 || key == 156) { commitField(); return; }
        if (field.textboxKeyTyped(c, key)) return;
        super.keyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        field.mouseClicked(mx, my, btn);

        for (int i = 0; i < 3; i++)
            if (mx >= iconX[i] && mx < iconX[i] + ICON_BOX && my >= iconY && my < iconY + ICON_BOX) { clickIcon(i); return; }

        boolean kbActive = (view == VIEW_EDIT) || (view == VIEW_SETS && addMode == ADD_MESSAGE);
        if (kbActive) {
            for (int i = 0; i < SYMBOLS.length; i++) {
                int col = i % SYMS_PER_ROW, row = i / SYMS_PER_ROW;
                int x = symX + col * CELL, y = symY + row * CELL;
                if (mx >= x && mx < x + CELL - 1 && my >= y && my < y + CELL - 1) { insertSymbol(SYMBOLS[i]); return; }
            }
        }

        
        List<String> rows = currentRowLabels();
        for (int i = 0; i < rows.size(); i++) {
            int y = listY + i * (ROW_H + ROW_GAP);
            int delX = listX + listW - 16;
            if (my >= y && my < y + ROW_H) {
                if (mx >= delX && mx < delX + 14) { deleteRow(i); return; }
                if (mx >= listX && mx < delX) { openRow(i); return; }
            }
        }

        super.mouseClicked(mx, my, btn);
    }

    
    
    private boolean isSingle(GgSet s) {
        return s != null && s.messages.size() == 1 && s.name != null && s.name.equals(s.messages.get(0));
    }

    
    private List<String> currentRowLabels() {
        List<String> out = new ArrayList<String>();
        if (view == VIEW_SETS) {
            for (GgSet s : cfg.autoGgSets) {
                if (isSingle(s)) {
                    out.add("\u00a7f" + s.messages.get(0));
                } else {
                    String rule = s.randomLine ? "\u00a78[рандом]" : "\u00a78[по порядку]";
                    out.add("\u00a7e\u00a7l" + s.name + " \u00a77(" + s.messages.size() + "/" + SjpConfig.GG_MAX_LINES + ") " + rule);
                }
            }
        } else {
            GgSet s = curSet();
            if (s != null) for (String m : s.messages) out.add("\u00a7f" + m);
        }
        return out;
    }

    private void openRow(int i) {
        if (view == VIEW_SETS) {
            if (i < 0 || i >= cfg.autoGgSets.size()) return;
            GgSet s = cfg.autoGgSets.get(i);
            if (isSingle(s)) {
                
                editingSingle = i;
                field.setText(s.messages.get(0));
                field.setCursorPositionEnd();
                field.setFocused(true);
            } else {
                gotoEdit(i);
            }
        }
        else {
            GgSet s = curSet();
            if (s != null && i >= 0 && i < s.messages.size()) {
                editingLine = i;
                field.setText(s.messages.get(i));
                field.setCursorPositionEnd();
                field.setFocused(true);
            }
        }
    }

    private void deleteRow(int i) {
        if (view == VIEW_SETS) {
            if (i >= 0 && i < cfg.autoGgSets.size()) { cfg.autoGgSets.remove(i); cfg.save(); }
        } else {
            GgSet s = curSet();
            if (s != null && i >= 0 && i < s.messages.size()) {
                s.messages.remove(i);
                if (editingLine == i) { editingLine = -1; field.setText(""); }
                cfg.save();
            }
        }
    }

    @Override
    public void updateScreen() { field.updateCursorCounter(); }

    

    @Override
    public void drawScreen(int mx, int my, float pt) {
        this.drawDefaultBackground();

        String title = (view == VIEW_SETS)
                ? "\u00a7d\u00a7lАвто-ГГ"
                : "\u00a7d\u00a7lНабор: \u00a7f" + (curSet() != null ? curSet().name : "?");
        drawCenteredString(fontRenderer, title, width / 2, 8, 0xFFFF55FF);

        
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < 3; i++) {
            boolean hover = mx >= iconX[i] && mx < iconX[i] + ICON_BOX && my >= iconY && my < iconY + ICON_BOX;
            drawRect(iconX[i], iconY, iconX[i] + ICON_BOX, iconY + ICON_BOX, hover ? 0xFF55556E : 0xFF2A2A33);
            this.itemRender.renderItemAndEffectIntoGUI(iconStack(i), iconX[i] + 4, iconY + 4);
        }
        RenderHelper.disableStandardItemLighting();

        
        String label;
        if (view == VIEW_SETS) {
            if (editingSingle >= 0) label = "\u00a7eРедактируешь сообщение:";
            else if (addMode == ADD_MESSAGE) label = "\u00a77Текст сообщения:";
            else label = "\u00a77Имя набора (папки):";
        }
        else if (editingLine == -2) label = "\u00a7eНовое имя набора:";
        else if (editingLine >= 0) label = "\u00a7eРедактируешь строку #" + (editingLine + 1) + ":";
        else {
            GgSet s = curSet();
            boolean full = s != null && s.messages.size() >= SjpConfig.GG_MAX_LINES;
            label = full ? "\u00a7cМаксимум 3 строки в наборе" : "\u00a77Новая строка:";
        }
        drawString(fontRenderer, label, fieldX, fieldY - 11, 0xFFAAAAAA);
        drawRect(fieldX, fieldY, fieldX + fieldW, fieldY + 20, 0xFF101014);
        drawRect(fieldX, fieldY, fieldX + fieldW, fieldY + 1, 0xFF55FF55);
        field.drawTextBox();

        
        boolean showKb = (view == VIEW_EDIT) || (view == VIEW_SETS && addMode == ADD_MESSAGE);
        if (showKb) {
            drawString(fontRenderer, "\u00a77Символы \u2014 тыкай чтоб вставить:", symX, symY - 11, 0xFFAAAAAA);
            for (int i = 0; i < SYMBOLS.length; i++) {
                int col = i % SYMS_PER_ROW, row = i / SYMS_PER_ROW;
                int x = symX + col * CELL, y = symY + row * CELL;
                boolean hover = mx >= x && mx < x + CELL - 1 && my >= y && my < y + CELL - 1;
                drawRect(x, y, x + CELL - 1, y + CELL - 1, hover ? 0xFF5A5A6E : 0xFF2A2A33);
                int sw = fontRenderer.getStringWidth(SYMBOLS[i]);
                fontRenderer.drawString(SYMBOLS[i], x + (CELL - 1 - sw) / 2, y + 5, hover ? 0xFFFFFFFF : 0xFFE0E0E0);
            }
        }

        
        String listHeader = (view == VIEW_SETS)
                ? "\u00a77Сообщения и наборы \u2014 клик открыть, \u2716 удалить:"
                : "\u00a77Строки (макс 3) \u2014 клик чтоб изменить:";
        drawString(fontRenderer, listHeader, listX, listY - 12, 0xFFAAAAAA);

        List<String> rows = currentRowLabels();
        for (int i = 0; i < rows.size(); i++) {
            int y = listY + i * (ROW_H + ROW_GAP);
            boolean editing = (view == VIEW_EDIT && i == editingLine);
            boolean rowHover = mx >= listX && mx < listX + listW - 16 && my >= y && my < y + ROW_H;
            int bg = editing ? 0x803030A0 : (rowHover ? 0x40FFFFFF : 0x40000000);
            drawRect(listX, y, listX + listW, y + ROW_H, bg);
            String prefix = (view == VIEW_EDIT)
                    ? (curSet() != null && curSet().randomLine ? "\u00a78\u2022 " : "\u00a78" + (i + 1) + ". ")
                    : "\u00a78\u25b6 ";
            fontRenderer.drawString(prefix + rows.get(i), listX + 4, y + 3, 0xFFFFFFFF);
            int delX = listX + listW - 16;
            boolean delHover = mx >= delX && mx < delX + 14 && my >= y && my < y + ROW_H;
            drawRect(delX, y, delX + 14, y + ROW_H, delHover ? 0xFFAA3333 : 0xFF552222);
            drawCenteredString(fontRenderer, "\u00a7f\u2716", delX + 7, y + 3, 0xFFFFFFFF);
        }
        if (rows.isEmpty()) {
            String empty = (view == VIEW_SETS) ? "\u00a78(нет наборов \u2014 добавь выше)" : "\u00a78(пусто \u2014 добавь строку выше)";
            drawString(fontRenderer, empty, listX + 4, listY + 3, 0xFF888888);
        }

        super.drawScreen(mx, my, pt);

        for (int i = 0; i < 3; i++)
            if (mx >= iconX[i] && mx < iconX[i] + ICON_BOX && my >= iconY && my < iconY + ICON_BOX)
                drawHoveringText(new ArrayList<String>(Arrays.asList(iconTooltip(i))), mx, my);
    }
}
