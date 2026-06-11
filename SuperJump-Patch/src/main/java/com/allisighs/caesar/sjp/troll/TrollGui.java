package com.allisighs.caesar.sjp.troll;

import com.allisighs.caesar.sjp.config.SjpConfig;
import com.allisighs.caesar.sjp.config.SjpConfig.TrollProfile;
import com.allisighs.caesar.sjp.gui.SjpGui;
import com.allisighs.caesar.sjp.gui.SjpInventory;
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

public class TrollGui extends GuiChest {

    private static SjpInventory makeInv() { return new SjpInventory(54); }

    private final SjpConfig cfg = SjpConfig.get();
    private final SjpInventory inv;

    private final Map<Integer, Integer> actions = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> profileSlot = new HashMap<Integer, Integer>();

    public TrollGui() {
        super(Minecraft.getMinecraft().player.inventory, makeInv());
        this.inv = (SjpInventory) this.inventorySlots.getSlot(0).inventory;
        rebuild();
    }

    private ItemStack item(ItemStack st, String name, boolean shine, String... lore) {
        NBTTagCompound tag = st.hasTagCompound() ? st.getTagCompound() : new NBTTagCompound();
        NBTTagCompound display = tag.getCompoundTag("display");
        if (name != null && !name.isEmpty()) display.setString("Name", name);
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
            ench.appendTag(e);
            tag.setTag("ench", ench);
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
        for (int i = 0; i < 54; i++) inv.setInventorySlotContents(i, ItemStack.EMPTY);
        actions.clear();
        profileSlot.clear();

        set(4, new ItemStack(cfg.trollEnabled ? Items.NETHER_STAR : Items.COAL), 1,
                "\u00a7d\u00a7lТроллинг", cfg.trollEnabled,
                cfg.trollEnabled ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77Иногда повторяет чужие сообщения");

        set(2, new ItemStack(Items.REDSTONE), 2,
                "\u00a7e\u00a7lЧастота: \u00a7f" + cfg.trollChance + "%", false,
                "\u00a77Шанс повтора сообщения, но рекомендую 1%",
                "\u00a78ЛКМ +5     ПКМ -5");

        set(6, new ItemStack(Items.WRITABLE_BOOK), 3,
                "\u00a7a\u00a7lНовый профиль", true,
                "\u00a77Добавить пустой профиль");

        List<TrollProfile> profs = cfg.trollProfiles;
        int[] cells = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        TrollProfile learning = TrollHandler.get() != null ? TrollHandler.get().learnTarget : null;
        for (int i = 0; i < profs.size() && i < cells.length; i++) {
            TrollProfile pr = profs.get(i);
            boolean isLearn = pr == learning;
            ItemStack icon = new ItemStack(isLearn ? Items.CLOCK : (pr.enabled ? Items.SLIME_BALL : Items.GUNPOWDER));
            String trained = (pr.sep == null || pr.sep.isEmpty()) ? "\u00a7cне обучен" : "\u00a7aобучен";
            set(cells[i], icon, 100 + i,
                    (pr.enabled ? "\u00a7a\u00a7l" : "\u00a77\u00a7l") + pr.name, pr.enabled || isLearn,
                    isLearn ? "\u00a7eабучэние..." : "\u00a77Статус: " + trained,
                    pr.sep == null || pr.sep.isEmpty() ? "\u00a78ещё не обучен" : "\u00a78ловит после: \u00a7f\"" + pr.sep + "\"",
                    "\u00a78ЛКМ: обучить",
                    "\u00a78ПКМ: вкл/выкл",
                    "\u00a78Ср.кнопка мыши: переименовать",
                    "\u00a78Shift+ЛКМ: удалить");
            profileSlot.put(cells[i], i);
        }

        set(49, new ItemStack(Items.ARROW), 200, "\u00a7e\u00a7l\u00ab Назад", false,
                "\u00a77В главнео меню мода");

        for (int i = 0; i < 54; i++) {
            if (inv.getStackInSlot(i).isEmpty()) {
                ItemStack v = new ItemStack(Blocks.VINE);
                NBTTagCompound tag = new NBTTagCompound();
                NBTTagCompound d = new NBTTagCompound();
                d.setString("Name", "\u00a78");
                tag.setTag("display", d);
                tag.setInteger("HideFlags", 127);
                v.setTagCompound(tag);
                inv.setInventorySlotContents(i, v);
            }
        }
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (!pending.isEmpty()) return;
        int actionId = -1;
        boolean shift = type == ClickType.QUICK_MOVE;
        boolean middle = type == ClickType.CLONE;
        Object[] snap = snapshot();
        if (slot != null && slot.inventory == inv && actions.containsKey(slot.getSlotIndex()))
            actionId = slot.getSlotIndex();
        if (slot != null) fakeClick(slot, mouseButton);
        long delay = 60 + (long) (Math.random() * 60);
        pending.add(new Pending(System.currentTimeMillis() + delay, snap, actionId, mouseButton, shift, middle));
    }

    private boolean apply(int idx, int mouseButton, boolean shift, boolean middle) {
        int a = actions.get(idx);
        if (a == 1) cfg.trollEnabled = !cfg.trollEnabled;
        else if (a == 2) {
            if (mouseButton == 1) cfg.trollChance = Math.max(1, cfg.trollChance - 5);
            else cfg.trollChance = Math.min(100, cfg.trollChance + 5);
        } else if (a == 3) {
            cfg.trollProfiles.add(new TrollProfile("Профиль " + (cfg.trollProfiles.size() + 1)));
        } else if (a == 200) {
            cfg.save();
            mc.displayGuiScreen(new SjpGui());
            return true;
        } else if (a >= 100) {
            int pi = profileSlot.get(idx);
            if (pi >= cfg.trollProfiles.size()) return false;
            TrollProfile pr = cfg.trollProfiles.get(pi);
            if (shift) {
                cfg.trollProfiles.remove(pi);
            } else if (middle) {
                cfg.save();
                mc.displayGuiScreen(new TrollRenameGui(pr));
                return true;
            } else if (mouseButton == 1) {
                pr.enabled = !pr.enabled;
            } else {
                TrollHandler h = TrollHandler.get();
                if (h != null) h.startLearn(pr);
                cfg.save();
            }
        }
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
        long fireAt; ItemStack[] menuSnap; ItemStack cursorSnap; ItemStack[] playerSnap;
        int actionIdx, btn; boolean shift, middle;
        Pending(long fireAt, Object[] snap, int actionIdx, int btn, boolean shift, boolean middle) {
            this.fireAt = fireAt; this.menuSnap = (ItemStack[]) snap[0]; this.cursorSnap = (ItemStack) snap[1];
            this.playerSnap = (ItemStack[]) snap[2]; this.actionIdx = actionIdx; this.btn = btn;
            this.shift = shift; this.middle = middle;
        }
    }

    private final List<Pending> pending = new ArrayList<Pending>();

    private Object[] snapshot() {
        ItemStack[] menu = new ItemStack[54];
        for (int i = 0; i < 54; i++) menu[i] = inv.getStackInSlot(i).copy();
        ItemStack cursor = mc.player.inventory.getItemStack().copy();
        ItemStack[] pl = new ItemStack[mc.player.inventory.getSizeInventory()];
        for (int i = 0; i < pl.length; i++) pl[i] = mc.player.inventory.getStackInSlot(i).copy();
        return new Object[]{menu, cursor, pl};
    }

    private void restore(Pending p) {
        for (int i = 0; i < 54; i++) inv.setInventorySlotContents(i, p.menuSnap[i]);
        mc.player.inventory.setItemStack(p.cursorSnap);
        for (int i = 0; i < p.playerSnap.length && i < mc.player.inventory.getSizeInventory(); i++)
            mc.player.inventory.setInventorySlotContents(i, p.playerSnap[i]);
    }

    private long lastRefresh = 0;

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
            if (p.actionIdx >= 0) { if (apply(p.actionIdx, p.btn, p.shift, p.middle)) return; }
        }
        if (now - lastRefresh > 400) { lastRefresh = now; rebuild(); }
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
        this.fontRenderer.drawString("\u00a7d\u00a7lТроллинг - профили", 8, 6, 0x404000);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
