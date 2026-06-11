package com.allisighs.caesar.sjp.gui;

import com.allisighs.caesar.sjp.config.SjpConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PvpGui extends GuiChest {

    private static SjpInventory makeInv() { return new SjpInventory(27); }
    private final SjpConfig cfg = SjpConfig.get();
    private final SjpInventory inv;
    private final Map<Integer, Integer> actions = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> colorSlot = new HashMap<Integer, Integer>();

    private static final int[] COLORS = {0xFF5555, 0x55FF55, 0x55FFFF, 0xFFFF55, 0xFF55FF, 0xFFFFFF, 0xFFAA00, 0x5555FF};
    private static final String[] NAMES = {"Красный", "Зелёный", "Голубой", "Жёлтый", "Розовый", "Белый", "Оранжевый", "Синий"};
    private static final int[] DYE = {1, 10, 12, 11, 13, 15, 14, 4};

    public PvpGui() {
        super(Minecraft.getMinecraft().player.inventory, makeInv());
        this.inv = (SjpInventory) this.inventorySlots.getSlot(0).inventory;
        rebuild();
    }

    private ItemStack item(ItemStack st, String name, boolean shine, String... lore) {
        NBTTagCompound tag = st.hasTagCompound() ? st.getTagCompound() : new NBTTagCompound();
        NBTTagCompound display = tag.getCompoundTag("display");
        if (name != null) display.setString("Name", name);
        if (lore.length > 0) {
            NBTTagList list = new NBTTagList();
            for (String l : lore) list.appendTag(new net.minecraft.nbt.NBTTagString(l));
            display.setTag("Lore", list);
        }
        tag.setTag("display", display);
        if (shine) {
            NBTTagList ench = new NBTTagList();
            NBTTagCompound e = new NBTTagCompound();
            e.setShort("id", (short) 0); e.setShort("lvl", (short) 1);
            ench.appendTag(e); tag.setTag("ench", ench);
        }
        tag.setInteger("HideFlags", 127);
        st.setTagCompound(tag);
        return st;
    }

    private void set(int slot, ItemStack st, int action, String name, boolean shine, String... lore) {
        inv.setInventorySlotContents(slot, item(st, name, shine, lore));
        actions.put(slot, action);
    }

    private void rebuild() {
        for (int i = 0; i < 27; i++) inv.setInventorySlotContents(i, ItemStack.EMPTY);
        actions.clear();
        colorSlot.clear();

        set(10, new ItemStack(Items.IRON_SWORD), 1,
                "\u00a7c\u00a7lПодсветка в зоне удара", cfg.pvpEsp,
                cfg.pvpEsp ? "\u00a7aВключена" : "\u00a7cВыключена",
                "\u00a77Подсвечивает игрока,",
                "\u00a77до которого достаешь рукой!");

        set(12, cfg.pvpHitbox ? new ItemStack(Blocks.GLASS) : new ItemStack(Items.SPECTRAL_ARROW), 2,
                cfg.pvpHitbox ? "\u00a7e\u00a7lРежим: рамка хитбокса" : "\u00a7e\u00a7lРежим: свечение",
                false,
                cfg.pvpHitbox ? "\u00a77Цветная рамка вокруг игрока" : "\u00a77Контур-свечение (без цвета)",
                "\u00a78нажми чтоб переключить");

        if (cfg.pvpHitbox) {
            set(14, new ItemStack(Items.DYE, 1, dyeOf(cfg.pvpColor)), 3,
                    "\u00a7e\u00a7lЩас цвет рамки: \u00a7f" + nameOf(cfg.pvpColor), true,
                    "\u00a77Активный цвет хитбокса",
                    "\u00a78нажми чтобы сменить");
            int[] cells = {19, 20, 21, 22, 23, 24, 25};
            for (int i = 0; i < COLORS.length && i < 7; i++) {
                boolean active = cfg.pvpColor == COLORS[i];
                set(cells[i], new ItemStack(Items.DYE, 1, DYE[i]), 100 + i,
                        (active ? "\u00a7a\u00a7l" : "\u00a77") + NAMES[i], active,
                        active ? "\u00a7aвыбран" : "\u00a78нажми чтоб выбрать");
                colorSlot.put(cells[i], i);
            }
        } else {
            set(14, new ItemStack(Blocks.BARRIER), 0,
                    "\u00a78Цвет токо для хитбокса", false,
                    "\u00a77Переключи режим на хитбокс,",
                    "\u00a77чтоб выбрать цвет");
        }

        set(22, new ItemStack(Items.ARROW), 200, "\u00a7e\u00a7l\u00ab Назад", false);

        for (int i = 0; i < 27; i++) {
            if (inv.getStackInSlot(i).isEmpty())
                inv.setInventorySlotContents(i, item(new ItemStack(Blocks.VINE), "\u00a78", false));
        }
    }

    private int dyeOf(int color) { for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == color) return DYE[i]; return 1; }
    private String nameOf(int color) { for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == color) return NAMES[i]; return "своё"; }
    private int nextColor(int cur) {
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == cur) return COLORS[(i + 1) % COLORS.length];
        return COLORS[0];
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (!pending.isEmpty()) return;
        int actionId = -1;
        Object[] snap = snapshot();
        if (slot != null) {
            if (slot.inventory == inv && actions.containsKey(slot.getSlotIndex())) actionId = slot.getSlotIndex();
            fakeClick(slot, mouseButton);
        }
        long delay = 60 + (long) (Math.random() * 60);
        pending.add(new Pending(System.currentTimeMillis() + delay, snap, actionId, mouseButton));
    }

    private boolean apply(int idx, int btn) {
        if (!actions.containsKey(idx)) return false;
        int a = actions.get(idx);
        if (a == 200) { cfg.save(); mc.displayGuiScreen(new SjpGui()); return true; }
        if (a == 1) cfg.pvpEsp = !cfg.pvpEsp;
        else if (a == 2) cfg.pvpHitbox = !cfg.pvpHitbox;
        else if (a == 3) cfg.pvpColor = nextColor(cfg.pvpColor);
        else if (a >= 100) {
            Integer ci = colorSlot.get(idx);
            if (ci != null) cfg.pvpColor = COLORS[ci];
        } else return false;
        cfg.save();
        rebuild();
        return false;
    }

    private void fakeClick(Slot slot, int mouseButton) {
        ItemStack cursor = mc.player.inventory.getItemStack();
        ItemStack inSlot = slot.getStack();
        if (cursor.isEmpty()) {
            if (!inSlot.isEmpty()) {
                ItemStack taken = inSlot.copy();
                if (mouseButton == 1) {
                    int half = (taken.getCount() + 1) / 2;
                    ItemStack pick = taken.copy(); pick.setCount(half);
                    ItemStack left = taken.copy(); left.setCount(taken.getCount() - half);
                    mc.player.inventory.setItemStack(pick);
                    slot.putStack(left.getCount() > 0 ? left : ItemStack.EMPTY);
                } else { mc.player.inventory.setItemStack(taken); slot.putStack(ItemStack.EMPTY); }
            }
        } else {
            if (inSlot.isEmpty()) {
                if (mouseButton == 1) {
                    ItemStack one = cursor.copy(); one.setCount(1);
                    slot.putStack(one); cursor.shrink(1);
                    mc.player.inventory.setItemStack(cursor.getCount() > 0 ? cursor : ItemStack.EMPTY);
                } else { slot.putStack(cursor.copy()); mc.player.inventory.setItemStack(ItemStack.EMPTY); }
            } else {
                ItemStack tmp = inSlot.copy();
                slot.putStack(cursor.copy());
                mc.player.inventory.setItemStack(tmp);
            }
        }
    }

    private static class Pending {
        long fireAt; ItemStack[] menuSnap; ItemStack cursorSnap; ItemStack[] playerSnap; int actionIdx; int btn;
        Pending(long fireAt, Object[] snap, int actionIdx, int btn) {
            this.fireAt = fireAt; this.menuSnap = (ItemStack[]) snap[0]; this.cursorSnap = (ItemStack) snap[1];
            this.playerSnap = (ItemStack[]) snap[2]; this.actionIdx = actionIdx; this.btn = btn;
        }
    }

    private final List<Pending> pending = new ArrayList<Pending>();

    private Object[] snapshot() {
        ItemStack[] menu = new ItemStack[27];
        for (int i = 0; i < 27; i++) menu[i] = inv.getStackInSlot(i).copy();
        ItemStack cursor = mc.player.inventory.getItemStack().copy();
        ItemStack[] pl = new ItemStack[mc.player.inventory.getSizeInventory()];
        for (int i = 0; i < pl.length; i++) pl[i] = mc.player.inventory.getStackInSlot(i).copy();
        return new Object[]{menu, cursor, pl};
    }

    private void restore(Pending p) {
        for (int i = 0; i < 27; i++) inv.setInventorySlotContents(i, p.menuSnap[i]);
        mc.player.inventory.setItemStack(p.cursorSnap);
        for (int i = 0; i < p.playerSnap.length && i < mc.player.inventory.getSizeInventory(); i++)
            mc.player.inventory.setInventorySlotContents(i, p.playerSnap[i]);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        long now = System.currentTimeMillis();
        List<Pending> ready = new ArrayList<Pending>();
        java.util.Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (now >= p.fireAt) { ready.add(p); it.remove(); }
        }
        for (Pending p : ready) {
            restore(p);
            if (p.actionIdx >= 0) { if (apply(p.actionIdx, p.btn)) return; }
        }
    }

    @Override
    public void onGuiClosed() {
        for (Pending p : pending) restore(p);
        pending.clear();
        mc.player.inventory.setItemStack(ItemStack.EMPTY);
        super.onGuiClosed();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        this.fontRenderer.drawString("\u00a7c\u00a7lЗона удара - подсветка", 8, 6, 0x404000);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
