package com.allisighs.caesar.sjp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SjpConfig {

    private static SjpConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File file;

    public boolean modEnabled = true;
    public boolean hudEnabled = true;
    public boolean superJump = true;

    public boolean unlimitedChat = true;
    public boolean antiSpam = true;
    public boolean antiAfk = false;

    public boolean pvpEsp = false;
    public boolean pvpHitbox = false;
    public int pvpColor = 0xFF5555;

    public boolean trollEnabled = false;
    public int trollChance = 5;
    public List<TrollProfile> trollProfiles = new ArrayList<TrollProfile>();

    public boolean zoomEnabled = false;
    public int zoomKey = 46;
    public double zoomSpeed = 2.0;
    public double zoomMax = 30.0;
    public boolean zoomCinematic = true;

    public boolean cullEnabled = false;
    public boolean cullWalls = true;
    public boolean cullFov = true;
    public boolean cullTileEntities = false;

    public String anchor = "TOP_LEFT";
    public int offsetX = 4;
    public int offsetY = 4;
    public float hudScale = 1.0f;

    public boolean hideNegativeKo = false;

    public int colorTitle = 0x55FFFF;
    public int colorGame = 0xAAAAAA;
    public int colorValue = 0x55FF55;
    public int colorNegative = 0xFF5555;

    public boolean sortByScore = true;
    public boolean colorTiers = true;
    public int tierHigh = 1000;
    public int tierMid = 100;

    public List<String> accounts = new ArrayList<String>();
    public Map<String, Boolean> enabledGames = new LinkedHashMap<String, Boolean>();

    public static class TrollProfile {
        public String name = "Профиль";
        public String prefix = "";
        public String sep = "";
        public boolean enabled = true;
        public TrollProfile() {}
        public TrollProfile(String name) { this.name = name; }
    }

    public static SjpConfig get() {
        if (instance == null) load();
        return instance;
    }

    private static File getFile() {
        if (file == null) {
            File dir = new File(Minecraft.getMinecraft().mcDataDir, "config");
            if (!dir.exists()) dir.mkdirs();
            file = new File(dir, "sjp_settings.json");
        }
        return file;
    }

    public static void load() {
        try {
            File f = getFile();
            if (f.exists()) {
                FileReader r = new FileReader(f);
                instance = GSON.fromJson(r, SjpConfig.class);
                r.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
        if (instance == null) instance = new SjpConfig();
        instance.fix();
    }

    private void fix() {
        if (enabledGames == null) enabledGames = new LinkedHashMap<String, Boolean>();
        if (accounts == null) accounts = new ArrayList<String>();
        if (trollProfiles == null) trollProfiles = new ArrayList<TrollProfile>();
        if (anchor == null) anchor = "TOP_LEFT";
        if (zoomSpeed <= 0) zoomSpeed = 2.0;
        if (zoomMax < 2) zoomMax = 30.0;
    }

    public boolean isGameOn(String name) {
        Boolean b = enabledGames.get(name);
        if (b == null) { enabledGames.put(name, true); save(); return true; }
        return b;
    }

    public void toggleGame(String name) {
        enabledGames.put(name, !isGameOn(name));
        save();
    }

    public int[] resolveHud(int screenW, int screenH, int contentW, int contentH) {
        int x = offsetX, y = offsetY;
        boolean right = anchor.endsWith("RIGHT");
        boolean center = anchor.endsWith("CENTER");
        boolean bottom = anchor.startsWith("BOTTOM");
        boolean mid = anchor.startsWith("MIDDLE");
        if (right) x = screenW - contentW - offsetX;
        else if (center) x = (screenW - contentW) / 2;
        if (bottom) y = screenH - contentH - offsetY;
        else if (mid) y = (screenH - contentH) / 2;
        return new int[]{x, y};
    }

    public void save() {
        try {
            FileWriter w = new FileWriter(getFile());
            GSON.toJson(this, w);
            w.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
