package com.allisighs.caesar.sjp.stats;

import net.minecraft.client.Minecraft;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatManager {

    public static final StatManager INST = new StatManager();

    private static final long REQUEST_MS = 60000L, CAPTURE_MS = 5000L, COOLDOWN_MS = 11000L, JOIN_GRACE_MS = 60000L;

    public List<GameStat> stats = new ArrayList<GameStat>();
    public int totalKo = 0;
    public boolean capturing = false;
    public String lastError = null;
    public int rank = 0;
    public String playerName = "";

    private long lastRequest = 0L, captureUntil = 0L, nextAllowed = 0L, joinTime = 0L;
    private List<GameStat> building = null;
    private boolean sawHeader = false;
    private boolean rankParsed = false;
    private final Map<String, String> nameMap = new LinkedHashMap<String, String>();

    private StatManager() {
        nameMap.put("бедварс", "БедВарс");
        nameMap.put("быстрыйбедварс", "Быстрый БедВарс");
        nameMap.put("таркада", "ТАркада");
        nameMap.put("скайварс", "СкайВарс");
        nameMap.put("голодныеигры", "Голодные игры");
        nameMap.put("крокодил", "Крокодил");
        nameMap.put("прятки", "Прятки");
        nameMap.put("спидбилдерс", "СпидБилдерс");
        nameMap.put("овечки", "Овечки");
        nameMap.put("тнтран", "ТНТРан");
        nameMap.put("аннексия", "Аннексия");
        nameMap.put("тайнаубийства", "Тайна убийства");
        nameMap.put("контрстрайк", "Контр-Страйк");
        nameMap.put("битвастроителей", "Битва строителей");
        nameMap.put("миксгейм", "МиксГейм");
        nameMap.put("дезран", "ДезРан");
        loadCache();
    }

    public void onJoin() {
        joinTime = System.currentTimeMillis();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) playerName = mc.player.getName();
    }

    public void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();
        if (joinTime == 0L) joinTime = now;
        if (capturing && now > captureUntil) {
            capturing = false;
            if (stats.isEmpty() && lastError == null) lastError = "нет ответа";
        }
        if (now - joinTime < JOIN_GRACE_MS) return;
        if (now - lastRequest > REQUEST_MS && now >= nextAllowed && !capturing) sendRequest(mc);
    }

    public void forceRefresh() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now < nextAllowed) { lastError = "подожди " + ((nextAllowed - now) / 1000 + 1) + "с"; return; }
        sendRequest(mc);
    }

    private void sendRequest(Minecraft mc) {
        long now = System.currentTimeMillis();
        lastRequest = now; nextAllowed = now + COOLDOWN_MS; captureUntil = now + CAPTURE_MS;
        capturing = true; building = null; sawHeader = false; rankParsed = false; lastError = null;
        try { mc.player.sendChatMessage("/clan score"); }
        catch (Throwable t) { capturing = false; lastError = "ошибка"; }
    }

    public boolean isCapturing() { return capturing && System.currentTimeMillis() <= captureUntil; }

    public boolean handleLine(String raw) {
        if (!isCapturing()) return false;
        String text = stripColors(raw);
        if (text.trim().isEmpty()) return false;
        boolean relevant = false;
        if (text.contains("клановых очк") || text.contains("не чаще") || text.contains("раз в 10 секунд")) {
            nextAllowed = System.currentTimeMillis() + COOLDOWN_MS;
            lastError = "подожди 10с"; capturing = false; return true;
        }
        Matcher head = Pattern.compile("Рейтинг игрока\\s+(\\S+)\\s*:\\s*(-?\\d+)").matcher(text);
        if (head.find()) {
            playerName = head.group(1); rank = Integer.parseInt(head.group(2));
            rankParsed = true;
            building = new ArrayList<GameStat>(); sawHeader = true; relevant = true;
        }
        Matcher gm = Pattern.compile(">\\s*([^:>]+?):\\s*(-?\\d+)\\s*очк").matcher(text);
        while (gm.find()) {
            if (building == null) { building = new ArrayList<GameStat>(); sawHeader = true; }
            String rawName = gm.group(1).trim();
            int score = Integer.parseInt(gm.group(2));
            String key = rawName.toLowerCase().replaceAll("[^а-яёa-z]", "");
            String name = nameMap.containsKey(key) ? nameMap.get(key) : rawName;
            GameStat g = new GameStat(name); g.ko = score; building.add(g); relevant = true;
        }
        if (text.contains("---") && (sawHeader || (building != null && !building.isEmpty()))) relevant = true;
        if (relevant && building != null && !building.isEmpty()) commit();
        return relevant;
    }

    private void commit() {
        int total = 0;
        for (GameStat g : building) total += g.ko;
        stats = building; totalKo = rankParsed ? rank : total;
        lastError = null; capturing = false; saveCache();
    }

    private String stripColors(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)\u00a7[0-9A-FK-OR]", "");
    }

    private File cacheFile() { return new File(Minecraft.getMinecraft().mcDataDir, "config/sjp_stats_cache.txt"); }

    private void saveCache() {
        try {
            java.io.PrintWriter w = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(cacheFile(), false), "UTF-8"));
            w.println("nick=" + playerName); w.println("rank=" + rank);
            for (GameStat g : stats) w.println(g.name + "=" + g.ko);
            w.close();
        } catch (Throwable ignored) {}
    }

    private void loadCache() {
        try {
            File f = cacheFile();
            if (!f.exists()) return;
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(
                    new java.io.FileInputStream(f), "UTF-8"));
            List<GameStat> cached = new ArrayList<GameStat>();
            String line;
            while ((line = r.readLine()) != null) {
                int eq = line.lastIndexOf('=');
                if (eq < 0) continue;
                String k = line.substring(0, eq), v = line.substring(eq + 1);
                if (k.equals("nick")) playerName = v;
                else if (k.equals("rank")) { try { rank = Integer.parseInt(v); } catch (Exception ignored) {} }
                else { try { GameStat g = new GameStat(k); g.ko = Integer.parseInt(v); cached.add(g); } catch (Exception ignored) {} }
            }
            r.close();
            if (!cached.isEmpty()) { stats = cached; totalKo = rank; }
        } catch (Throwable ignored) {}
    }
}
