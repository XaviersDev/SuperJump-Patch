package com.allisighs.caesar.sjp.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UnlimitedChat extends GuiNewChat {

    private final Minecraft mc;
    private Field fChatLines, fDrawnLines;
    private boolean ok = false;
    private final List<String> persistent = new ArrayList<String>();
    private long lastSave = 0;
    private static final long MAX_BYTES = 3L * 1024 * 1024;
    private long currentBytes = 0L;

    public UnlimitedChat(Minecraft mcIn) {
        super(mcIn);
        this.mc = mcIn;
        try {
            fChatLines = findField("chatLines", "field_146252_h");
            fDrawnLines = findField("drawnChatLines", "field_146253_i");
            fChatLines.setAccessible(true);
            fDrawnLines.setAccessible(true);
            ok = true;
        } catch (Throwable t) { ok = false; }
        loadFile();
    }

    private Field findField(String... names) throws NoSuchFieldException {
        for (String n : names) { try { return GuiNewChat.class.getDeclaredField(n); } catch (NoSuchFieldException ignored) {} }
        throw new NoSuchFieldException(names[0]);
    }

    @Override
    public void printChatMessageWithOptionalDeletion(ITextComponent comp, int chatLineId) {
        if (!ok) { super.printChatMessageWithOptionalDeletion(comp, chatLineId); return; }
        addLine(comp, chatLineId, this.mc.ingameGUI.getUpdateCounter(), false);
    }

    @SuppressWarnings("unchecked")
    private void addLine(ITextComponent comp, int chatLineId, int updateCounter, boolean displayOnly) {
        try {
            List<ChatLine> drawn = (List<ChatLine>) fDrawnLines.get(this);
            List<ChatLine> chatLines = (List<ChatLine>) fChatLines.get(this);
            int width = MathHelper.floor((float) getChatWidth() / getChatScale());
            List<ITextComponent> split = GuiUtilRenderComponents.splitText(comp, width, this.mc.fontRenderer, false, false);
            for (ITextComponent part : split) drawn.add(0, new ChatLine(updateCounter, part, chatLineId));
            if (!displayOnly) chatLines.add(0, new ChatLine(updateCounter, comp, chatLineId));
        } catch (Throwable t) { super.printChatMessageWithOptionalDeletion(comp, chatLineId); }
    }

    public void remember(String formatted) {
        persistent.add(formatted);
        currentBytes += byteLen(formatted);
        while (currentBytes > MAX_BYTES && persistent.size() > 1) {
            currentBytes -= byteLen(persistent.remove(0));
        }
        long now = System.currentTimeMillis();
        if (now - lastSave > 3000) { lastSave = now; save(); }
    }

    private long byteLen(String s) {
        try { return s.getBytes("UTF-8").length + 1; } catch (Exception e) { return s.length() + 1; }
    }

    private File file() { return new File(mc.mcDataDir, "config/sjp_chat_history.txt"); }

    public void save() {
        try {
            java.io.PrintWriter w = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(file(), false), "UTF-8"));
            for (int i = 0; i < persistent.size(); i++)
                w.println(persistent.get(i).replace("\\", "\\\\").replace("\n", "\\n").replace("\r", ""));
            w.close();
        } catch (Throwable ignored) {}
    }

    private void loadFile() {
        try {
            File f = file();
            if (!f.exists()) return;
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(
                    new java.io.FileInputStream(f), "UTF-8"));
            String s;
            while ((s = r.readLine()) != null) {
                String line = unescape(s);
                persistent.add(line);
                currentBytes += byteLen(line);
            }
            r.close();
            while (currentBytes > MAX_BYTES && persistent.size() > 1) {
                currentBytes -= byteLen(persistent.remove(0));
            }
        } catch (Throwable ignored) {}
    }

    private String unescape(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'n') { out.append('\n'); i++; continue; }
                if (n == '\\') { out.append('\\'); i++; continue; }
            }
            out.append(c);
        }
        return out.toString();
    }

    public void restore() {
        try {
            List<String> copy = new ArrayList<String>(persistent);
            for (String line : copy) printChatMessage(new TextComponentString(line));
        } catch (Throwable ignored) {}
    }
}
