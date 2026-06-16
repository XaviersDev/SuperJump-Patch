package com.allisighs.caesar.sjp.gui;

import com.allisighs.caesar.sjp.config.SjpConfig;
import com.allisighs.caesar.sjp.stats.GameStat;
import com.allisighs.caesar.sjp.stats.StatManager;
import com.allisighs.caesar.sjp.troll.TrollGui;
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

public class SjpGui extends GuiChest {

    private static final int MAIN = 0, GAMES = 1, POSITION = 2, STATS = 3;
    private static SjpInventory makeInv() { return new SjpInventory(54); }

    private final SjpConfig cfg = SjpConfig.get();
    private final SjpInventory inv;
    private int page = MAIN;

    private final Map<Integer, Integer> actions = new HashMap<Integer, Integer>();
    private final Map<Integer, String> games = new HashMap<Integer, String>();
    private final Map<Integer, String> anchorMap = new HashMap<Integer, String>();

    public SjpGui() {
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
        for (int i = 0; i < 54; i++) inv.setInventorySlotContents(i, ItemStack.EMPTY);
        actions.clear(); games.clear(); anchorMap.clear();
        if (page == GAMES) buildGames();
        else if (page == POSITION) buildPosition();
        else if (page == STATS) buildStats();
        else buildMain();
        fillEmpty();
    }

    private void fillEmpty() {
        for (int i = 0; i < 45; i++) {
            if (inv.getStackInSlot(i).isEmpty())
                inv.setInventorySlotContents(i, item(new ItemStack(Blocks.VINE), "\u00a78", false));
        }
        animateBottomRow();
    }

    private long animStart = System.currentTimeMillis();
    private static final int[] RAINBOW = {14, 1, 4, 5, 13, 3, 11, 10, 2};
    private static final int[] DUO = {11, 3, 11, 3, 9, 3, 11, 3, 11};

    private void animateBottomRow() {
        long ms = System.currentTimeMillis() - animStart;
        long frame = ms / 360L;
        int pattern = (int) ((ms / 4267L) % 4);

        for (int col = 0; col < 9; col++) {
            int slot = 45 + col;
            if (actions.containsKey(slot)) continue;
            int mirror = Math.min(col, 8 - col);
            int meta;
            boolean glow = false;

            switch (pattern) {
                case 0: {
                    int idx = (int) ((frame + mirror) % RAINBOW.length);
                    meta = RAINBOW[idx];
                    long spark = (frame % 14);
                    glow = (spark == mirror);
                    break;
                }
                case 1: {
                    int idx = (int) ((frame + col) % RAINBOW.length);
                    meta = RAINBOW[idx];
                    glow = (((frame + col) % 18) == 0);
                    break;
                }
                case 2: {
                    long pulse = frame % 6;
                    meta = DUO[col];
                    glow = (pulse == mirror);
                    break;
                }
                default: {
                    int idx = (int) ((frame / 2 + mirror) % RAINBOW.length);
                    meta = RAINBOW[idx];
                    long spark = (frame % 20);
                    glow = (spark == 0 && mirror == 4);
                    break;
                }
            }

            ItemStack pane = new ItemStack(Blocks.STAINED_GLASS_PANE, 1, meta);
            inv.setInventorySlotContents(slot, item(pane, "\u00a7r", glow));
        }
    }

    private void buildMain() {
        set(10, new ItemStack(cfg.modEnabled ? Items.EMERALD : Items.REDSTONE), 1,
                "\u00a7e\u00a7lВесь мод", cfg.modEnabled, cfg.modEnabled ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77Выключить все ну или включить");
        set(12, new ItemStack(Items.DIAMOND_AXE), 5,
                "\u00a76\u00a7lСупер-прыжок", cfg.superJump, cfg.superJump ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77ПКМ топором охотника, возвращает старую механику");
        set(14, new ItemStack(Items.COMPARATOR), 22,
                "\u00a7b\u00a7lАнти-спам", cfg.antiSpam, cfg.antiSpam ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77Повторящиюеся сообщения делаются в одну строку \u00a78(N)");
        set(16, new ItemStack(Items.WRITABLE_BOOK), 8,
                "\u00a7b\u00a7lБезлимитный чат", cfg.unlimitedChat, cfg.unlimitedChat ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77История не стирается и даже когда перезаходишь в майн");

        set(20, new ItemStack(Items.CLOCK), 23,
                "\u00a7d\u00a7lАнти-АФК", cfg.antiAfk, cfg.antiAfk ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77Реагирует на \"АФК\"");
        set(21, new ItemStack(Items.IRON_SWORD), 28,
                "\u00a7c\u00a7lЗона удара", cfg.pvpEsp,
                cfg.pvpEsp ? "\u00a7aВключена" : "\u00a7cВыключена",
                "\u00a77Подсветка игрока в зоне удара",
                "\u00a78нажми чтоб настроить");
        set(23, new ItemStack(Items.GHAST_TEAR), 26,
                "\u00a7d\u00a7lТроллинг", cfg.trollEnabled,
                cfg.trollEnabled ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77Повтор чужих сообщений  с мизерным шансом",
                "\u00a78нажми чтоб открыть профили");
        set(24, new ItemStack(Items.ENDER_EYE), 29,
                "\u00a7b\u00a7lЗум камеры", cfg.zoomEnabled,
                cfg.zoomEnabled ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77Приближение колесом + клавиша, ее можно любую сделать",
                "\u00a78нажми чтоб настроить");

        set(30, new ItemStack(Items.PAINTING), 30,
                "\u00a7a\u00a7lСтатистика", true,
                "\u00a77Все настройки статистики:",
                "\u00a77показ, размер, цвета,",
                "\u00a77позиция, мини-игры, сортировка",
                "\u00a78нажми чтоб открыть!");
        set(32, new ItemStack(Items.PRISMARINE_CRYSTALS), 31,
                "\u00a7b\u00a7lОптимизация (FPS)", cfg.cullEnabled,
                cfg.cullEnabled ? "\u00a7aВключена" : "\u00a7cВыключена",
                "\u00a77Не рисует сущности за стенами",
                "\u00a77и вне поля зрения",
                "\u00a78нажмите чтоб настроить");
        set(31, new ItemStack(Items.NAME_TAG), 32,
                "\u00a7d\u00a7lАвто-ГГ \u2764", cfg.autoGg,
                cfg.autoGg ? "\u00a7aВключен" : "\u00a7cВыключен",
                "\u00a77Сам пишет gg в конце катки на прятках",
                "\u00a77красивые фразы, очередь, символы",
                "\u00a78нажми чтоб настроить");
    }

    private void buildStats() {
        set(10, new ItemStack(cfg.hudEnabled ? Items.PAINTING : Items.ITEM_FRAME), 2,
                "\u00a7a\u00a7lПоказывать статистику", cfg.hudEnabled,
                cfg.hudEnabled ? "\u00a7aВКЛЮЧЕНО" : "\u00a7cВЫКЛЮЧЕНО",
                "\u00a77Список рейтинга на экране");
        set(12, new ItemStack(Items.GLOWSTONE_DUST), 9,
                "\u00a7e\u00a7lРазмер", false,
                "\u00a77Щас: \u00a7f" + String.format("%.1f", cfg.hudScale), "\u00a78ЛКМ +   ПКМ -");
        set(14, new ItemStack(Items.COMPASS), 7,
                "\u00a7b\u00a7lПоложение и сортировка", false,
                "\u00a77Куда поставить статистику", "\u00a77и как сортировать ?");
        set(16, new ItemStack(Blocks.CHEST), 4,
                "\u00a7e\u00a7lМини-игры", false, "\u00a77Какие режимы показывать ?");

        set(28, dyeForColor(cfg.colorTitle), 11, "\u00a7e\u00a7lЦвет ника", true, "\u00a77Нажми чтобы сменить");
        set(30, dyeForColor(cfg.colorValue), 12, "\u00a7e\u00a7lЦвет цифр", true, "\u00a77Нажми чтобы сменить");
        set(32, new ItemStack(Items.DYE, 1, 1), 3,
                "\u00a7c\u00a7lМинусовое К.О.", cfg.hideNegativeKo,
                cfg.hideNegativeKo ? "\u00a7cСкрыто" : "\u00a7aПоказывается",
                "\u00a77Прятать игры с минусом");
        set(34, new ItemStack(Items.NETHER_STAR), 13,
                "\u00a7a\u00a7lОбновить сейчас", true,
                StatManager.INST.isCapturing() ? "\u00a7eабнавляю..." : "\u00a77Рейтинг: \u00a7a" + StatManager.INST.totalKo,
                StatManager.INST.lastError != null ? "\u00a7c" + StatManager.INST.lastError : "\u00a78/clan score");

        set(49, new ItemStack(Items.ARROW), 200, "\u00a7e\u00a7l\u00ab Назад", false, "\u00a77В главное меню");
    }

    private void buildPosition() {
        set(13, new ItemStack(cfg.sortByScore ? Items.COMPARATOR : Items.REPEATER), 20,
                "\u00a7e\u00a7lСортировка по очкам", cfg.sortByScore,
                cfg.sortByScore ? "\u00a7aВключена" : "\u00a7cВыключена", "\u00a77Самый топ сверху");

        String[] anchors = {
                "TOP_LEFT", "TOP_CENTER", "TOP_RIGHT",
                "MIDDLE_LEFT", "MIDDLE_CENTER", "MIDDLE_RIGHT",
                "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT"
        };
        String[] labels = {
                "Сверху слева", "Сверху центр", "Сверху справа",
                "Слева центр", "Центр экрана", "Справа центр",
                "Снизу слева", "Снизу центр", "Снизу справа"
        };
        int[] cells = {20, 22, 24, 29, 31, 33, 38, 40, 42};
        for (int i = 0; i < 9; i++) {
            boolean active = cfg.anchor.equals(anchors[i]);
            ItemStack st = new ItemStack(active ? Blocks.SEA_LANTERN : Blocks.STONE_SLAB);
            set(cells[i], st, 100, "\u00a7e\u00a7l" + labels[i], active,
                    active ? "\u00a7aВыбрано" : "\u00a77Поставить сюды");
            anchorMap.put(cells[i], anchors[i]);
        }
        set(49, new ItemStack(Items.ARROW), 200, "\u00a7e\u00a7l\u00ab Назад", false);
    }

    private void buildGames() {
        List<String> list = gameNames();
        int n = list.size(), perRow = 7;
        int rows = (n + perRow - 1) / perRow;
        if (rows < 1) rows = 1; if (rows > 5) rows = 5;
        int idx = 0;
        for (int r = 0; r < rows && idx < n; r++) {
            int inThisRow = Math.min(perRow, n - r * perRow);
            int startCol = (9 - inThisRow) / 2;
            for (int c = 0; c < inThisRow; c++) {
                int slot = r * 9 + startCol + c;
                String g = list.get(idx++);
                boolean on = cfg.isGameOn(g);
                ItemStack st = new ItemStack(on ? Items.SLIME_BALL : Items.GUNPOWDER);
                set(slot, st, 300, (on ? "\u00a7a\u00a7l" : "\u00a7c\u00a7l") + g, on,
                        on ? "\u00a7aПоказывается" : "\u00a7cСкрыто");
                games.put(slot, g);
            }
        }
        set(49, new ItemStack(Items.ARROW), 200, "\u00a7e\u00a7l\u00ab Назад", false);
    }

    private List<String> gameNames() {
        List<String> list = new ArrayList<String>();
        if (!StatManager.INST.stats.isEmpty()) { for (GameStat gs : StatManager.INST.stats) list.add(gs.name); }
        else if (!cfg.enabledGames.isEmpty()) { for (String k : cfg.enabledGames.keySet()) list.add(k); }
        else {
            String[] def = {"БедВарс","Быстрый БедВарс","СкайВарс","ТНТРан","Прятки","ТАркада", "Крокодил","Контр-Страйк","СпидБилдерс","Овечки","Голодные игры", "Аннексия","Тайна убийства","МиксГейм"};
            for (String d : def) list.add(d);
        }
        return list;
    }

    private final int[] palette = {0x55FFFF, 0x55FF55, 0xFFFF55, 0xFFAA00, 0xFF55FF, 0xFFFFFF, 0xAAAAAA, 0xFF5555};

    private int nextColor(int cur) {
        for (int i = 0; i < palette.length; i++) if (palette[i] == cur) return palette[(i + 1) % palette.length];
        return palette[0];
    }

    private ItemStack dyeForColor(int color) {
        int[][] dyes = {{0xFFFFFF,15},{0xFFA500,14},{0xFF55FF,13},{0x55FFFF,12},{0xFFFF55,11},{0x55FF55,10},{0xFF69B4,9},{0x555555,8},{0xAAAAAA,7},{0x00AAAA,6},{0xAA00AA,5},{0x5555FF,4},{0x553311,3},{0x00AA00,2},{0xFF5555,1},{0x111111,0}};
        int best = 0, bestD = Integer.MAX_VALUE;
        for (int[] d : dyes) {
            int dr = ((d[0]>>16)&0xFF)-((color>>16)&0xFF);
            int dg = ((d[0]>>8)&0xFF)-((color>>8)&0xFF);
            int db = (d[0]&0xFF)-(color&0xFF);
            int dist = dr*dr+dg*dg+db*db;
            if (dist < bestD) { bestD = dist; best = d[1]; }
        }
        return new ItemStack(Items.DYE, 1, best);
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

    @Override
    public void updateScreen() {
        super.updateScreen();
        animateBottomRow();
        long now = System.currentTimeMillis();
        List<Pending> ready = new ArrayList<Pending>();
        java.util.Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (now >= p.fireAt) { ready.add(p); it.remove(); }
        }
        for (Pending p : ready) {
            restore(p);
            if (p.actionIdx >= 0) { if (doAction(p.actionIdx, p.btn)) return; }
        }
    }

    private boolean doAction(int idx, int btn) {
        int a = actions.get(idx);
        switch (a) {
            case 1: cfg.modEnabled = !cfg.modEnabled; break;
            case 2: cfg.hudEnabled = !cfg.hudEnabled; break;
            case 4: page = GAMES; break;
            case 5: cfg.superJump = !cfg.superJump; break;
            case 7: page = POSITION; break;
            case 8: cfg.unlimitedChat = !cfg.unlimitedChat; break;
            case 9:
                if (btn == 1) cfg.hudScale = Math.max(0.5f, Math.round((cfg.hudScale - 0.1f) * 10) / 10f);
                else cfg.hudScale = Math.min(2.5f, Math.round((cfg.hudScale + 0.1f) * 10) / 10f);
                break;
            case 11: cfg.colorTitle = nextColor(cfg.colorTitle); break;
            case 12: cfg.colorValue = nextColor(cfg.colorValue); break;
            case 13: StatManager.INST.forceRefresh(); break;
            case 3: cfg.hideNegativeKo = !cfg.hideNegativeKo; break;
            case 20: cfg.sortByScore = !cfg.sortByScore; break;
            case 22: cfg.antiSpam = !cfg.antiSpam; break;
            case 23: cfg.antiAfk = !cfg.antiAfk; break;
            case 26: cfg.save(); mc.displayGuiScreen(new TrollGui()); return true;
            case 28: cfg.save(); mc.displayGuiScreen(new PvpGui()); return true;
            case 29: cfg.save(); mc.displayGuiScreen(new ZoomGui()); return true;
            case 31: cfg.save(); mc.displayGuiScreen(new OptimizeGui()); return true;
            case 32: cfg.save(); mc.displayGuiScreen(new AutoGgGui()); return true;
            case 30: page = STATS; break;
            case 100: cfg.anchor = anchorMap.get(idx); break;
            case 200:
                if (page == POSITION || page == GAMES) page = STATS;
                else page = MAIN;
                break;
            case 300: cfg.toggleGame(games.get(idx)); break;
        }
        cfg.save();
        rebuild();
        return false;
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
        String title;
        if (page == GAMES) title = "\u00a7e\u00a7lМини-игры";
        else if (page == POSITION) title = "\u00a7e\u00a7lПоложение и сортировка";
        else if (page == STATS) title = "\u00a7e\u00a7lНастройки статы";
        else title = "\u00a7e\u00a7lНастройки /TCH";
        this.fontRenderer.drawString(title, 8, 6, 0x404000);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}