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
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZoomGui extends GuiChest {

    private static SjpInventory makeInv() { return new SjpInventory(27); }
    private final SjpConfig cfg = SjpConfig.get();
    private final SjpInventory inv;
    private final Map<Integer, Integer> actions = new HashMap<Integer, Integer>();
    private boolean bindMode = false;

    public ZoomGui() {
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

        set(10, cfg.zoomEnabled ? new ItemStack(Items.ENDER_EYE) : new ItemStack(Items.ENDER_PEARL), 1,
                "\u00a7b\u00a7lЗум", cfg.zoomEnabled,
                cfg.zoomEnabled ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77Зажми клавишу для приближения");

        set(12, new ItemStack(Items.SUGAR), 2,
                "\u00a7e\u00a7lСкорость: \u00a7f" + String.format("%.1f", cfg.zoomSpeed), false,
                "\u00a77Как быстро приближать /отдалять",
                "\u00a78ЛКМ +0.5     ПКМ -0.5");

        set(13, new ItemStack(Items.SPIDER_EYE), 4,
                "\u00a7e\u00a7lМакс. приближение: \u00a7f" + (int) cfg.zoomMax + "x", false,
                "\u00a78ЛКМ +5     ПКМ -5");

        set(14, cfg.zoomCinematic ? new ItemStack(Items.FILLED_MAP) : new ItemStack(Items.MAP), 3,
                "\u00a7d\u00a7lКино-камера", cfg.zoomCinematic,
                cfg.zoomCinematic ? "\u00a7aВключена" : "\u00a7cВыключена",
                "\u00a77Плавные движения камеры при зуме или тебе это ненадо?");

        String keyName = bindMode ? "\u00a7eнажми клавишу..." : "\u00a7f" + Keyboard.getKeyName(cfg.zoomKey);
        set(16, new ItemStack(Items.NAME_TAG), 5,
                "\u00a7a\u00a7lКлавиша: " + keyName, bindMode,
                "\u00a77Зажми её в игре для зума",
                "\u00a78нажми чтоб поменять бинд для зума");

        set(22, new ItemStack(Items.ARROW), 200, "\u00a7e\u00a7l\u00ab Назад", false,
                "\u00a77В главное меню");

        for (int i = 0; i < 27; i++) {
            if (inv.getStackInSlot(i).isEmpty())
                inv.setInventorySlotContents(i, item(new ItemStack(Blocks.VINE), "\u00a78", false));
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (bindMode) {
            if (key != Keyboard.KEY_ESCAPE) { cfg.zoomKey = key; cfg.save(); }
            bindMode = false;
            rebuild();
            return;
        }
        try { super.keyTyped(c, key); } catch (Throwable ignored) {}
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (!pending.isEmpty()) return;
        if (bindMode) return;
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
        if (a == 1) cfg.zoomEnabled = !cfg.zoomEnabled;
        else if (a == 2) {
            if (btn == 1) cfg.zoomSpeed = Math.max(0.5, Math.round((cfg.zoomSpeed - 0.5) * 10) / 10.0);
            else cfg.zoomSpeed = Math.min(10.0, Math.round((cfg.zoomSpeed + 0.5) * 10) / 10.0);
        } else if (a == 3) cfg.zoomCinematic = !cfg.zoomCinematic;
        else if (a == 4) {
            if (btn == 1) cfg.zoomMax = Math.max(5, cfg.zoomMax - 5);
            else cfg.zoomMax = Math.min(100, cfg.zoomMax + 5);
        } else if (a == 5) { bindMode = true; rebuild(); return false; }
        else return false;
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
        this.fontRenderer.drawString("\u00a7b\u00a7lНастры зума", 8, 6, 0x404000);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
