package top.pinkoc.nem.arena;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.pinkoc.nem.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Arena {

    private final JavaPlugin plugin;
    private final String name;
    private final File folder;
    private final File configFile;

    private String worldName;
    private boolean enabled;

    // ---------- 点位 ----------
    private Location waitLobby;
    private Location playerSpawn;
    private Location underKey;
    private Location underDoor;
    private Location toiletKey;
    private Location toiletDoor;
    private Location bedroomKey;
    private Location bedroomDoor;
    private Location bedroomSleep;
    private Location frontDoorKey;
    private Location frontDoor;
    private Location hideHouse;
    private Location police;
    private Location guSpawn;
    private final List<Location> patrolPoints = new ArrayList<>();

    // ---------- 参数 ----------
    private double pickupRadius = 2.0;
    private double doorRadius = 3.0;
    private int doorHoldSeconds = 3;
    private double captureDistance = 1.6;

    // 阶段 4/5/6 参数
    private int escapeHouseSeconds = 5;         // 冲出大门的时限
    private double houseEscapeRadius = 15.0;    // 判定"出大门"的距离
    private int hideArrivalSeconds = 30;        // 抵达 hideHouse 的时限
    private int hideStaySeconds = 30;           // hideHouse 内静止时长
    private int guBlindnessSeconds = 40;        // 古振兴黑暗效果时长
    private int noiseGlowSeconds = 5;           // 移动后发光持续时长

    public Arena(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.folder = new File(plugin.getDataFolder(), "arenas/" + name);
        this.configFile = new File(folder, "arena.yml");
        this.worldName = "ne_" + name;
    }

    // ------------------------------------------------------------------
    // I/O
    // ------------------------------------------------------------------

    public void load() {
        if (!configFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(configFile);

        worldName = y.getString("world", "ne_" + name);
        enabled = y.getBoolean("enabled", false);

        waitLobby    = Util.parse(y.getString("locations.wait-lobby"));
        playerSpawn  = Util.parse(y.getString("locations.player-spawn"));
        underKey     = Util.parse(y.getString("locations.under-key"));
        underDoor    = Util.parse(y.getString("locations.under-door"));
        toiletKey    = Util.parse(y.getString("locations.toilet-key"));
        toiletDoor   = Util.parse(y.getString("locations.toilet-door"));
        bedroomKey   = Util.parse(y.getString("locations.bedroom-key"));
        bedroomDoor  = Util.parse(y.getString("locations.bedroom-door"));
        bedroomSleep = Util.parse(y.getString("locations.bedroom-sleep"));
        frontDoorKey = Util.parse(y.getString("locations.front-door-key"));
        frontDoor    = Util.parse(y.getString("locations.front-door"));
        hideHouse    = Util.parse(y.getString("locations.hide-house"));
        police       = Util.parse(y.getString("locations.police"));
        guSpawn      = Util.parse(y.getString("locations.gu-spawn"));

        patrolPoints.clear();
        for (String s : y.getStringList("locations.patrol")) {
            Location l = Util.parse(s);
            if (l != null) patrolPoints.add(l);
        }

        pickupRadius    = y.getDouble("game.pickup-radius", 2.0);
        doorRadius      = y.getDouble("game.door-radius", 3.0);
        doorHoldSeconds = y.getInt("game.door-hold-seconds", 3);
        captureDistance = y.getDouble("game.capture-distance", 1.6);

        escapeHouseSeconds = y.getInt("game.escape-house-seconds", 5);
        houseEscapeRadius  = y.getDouble("game.house-escape-radius", 15.0);
        hideArrivalSeconds = y.getInt("game.hide-arrival-seconds", 30);
        hideStaySeconds    = y.getInt("game.hide-stay-seconds", 30);
        guBlindnessSeconds = y.getInt("game.gu-blindness-seconds", 40);
        noiseGlowSeconds   = y.getInt("game.noise-glow-seconds", 5);
    }

    public void save() {
        folder.mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("world", worldName);
        y.set("enabled", enabled);

        y.set("locations.wait-lobby",      Util.serialize(waitLobby));
        y.set("locations.player-spawn",    Util.serialize(playerSpawn));
        y.set("locations.under-key",       Util.serialize(underKey));
        y.set("locations.under-door",      Util.serialize(underDoor));
        y.set("locations.toilet-key",      Util.serialize(toiletKey));
        y.set("locations.toilet-door",     Util.serialize(toiletDoor));
        y.set("locations.bedroom-key",     Util.serialize(bedroomKey));
        y.set("locations.bedroom-door",    Util.serialize(bedroomDoor));
        y.set("locations.bedroom-sleep",   Util.serialize(bedroomSleep));
        y.set("locations.front-door-key",  Util.serialize(frontDoorKey));
        y.set("locations.front-door",      Util.serialize(frontDoor));
        y.set("locations.hide-house",      Util.serialize(hideHouse));
        y.set("locations.police",          Util.serialize(police));
        y.set("locations.gu-spawn",        Util.serialize(guSpawn));

        List<String> patrol = new ArrayList<>();
        for (Location l : patrolPoints) {
            String s = Util.serialize(l);
            if (s != null) patrol.add(s);
        }
        y.set("locations.patrol", patrol);

        y.set("game.pickup-radius", pickupRadius);
        y.set("game.door-radius", doorRadius);
        y.set("game.door-hold-seconds", doorHoldSeconds);
        y.set("game.capture-distance", captureDistance);

        y.set("game.escape-house-seconds", escapeHouseSeconds);
        y.set("game.house-escape-radius", houseEscapeRadius);
        y.set("game.hide-arrival-seconds", hideArrivalSeconds);
        y.set("game.hide-stay-seconds", hideStaySeconds);
        y.set("game.gu-blindness-seconds", guBlindnessSeconds);
        y.set("game.noise-glow-seconds", noiseGlowSeconds);

        try {
            y.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存 arena " + name + " 失败: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 世界
    // ------------------------------------------------------------------

    public World loadOrCreateWorld() {
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) return existing;
        WorldCreator creator = new WorldCreator(worldName);
        creator.generateStructures(false);
        creator.keepSpawnInMemory(false);
        World w = creator.createWorld();
        if (w != null) {
            w.setAutoSave(true);
            w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            w.setGameRule(GameRule.DO_FIRE_TICK, false);
            w.setGameRule(GameRule.KEEP_INVENTORY, true);
            w.setGameRule(GameRule.FALL_DAMAGE, false);
            w.setGameRule(GameRule.NATURAL_REGENERATION, false);
        }
        return w;
    }

    public void unloadWorld() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return;
        for (var p : new ArrayList<>(w.getPlayers())) {
            World fallback = Bukkit.getWorlds().get(0);
            if (fallback != null && !fallback.equals(w)) p.teleport(fallback.getSpawnLocation());
        }
        w.save();
        Bukkit.unloadWorld(w, true);
    }

    public boolean isWorldLoaded() {
        return Bukkit.getWorld(worldName) != null;
    }

    public List<String> missingPoints() {
        List<String> missing = new ArrayList<>();
        if (waitLobby == null)    missing.add("waitLobby");
        if (playerSpawn == null)  missing.add("playerSpawn");
        if (underKey == null)     missing.add("underKey");
        if (underDoor == null)    missing.add("underDoor");
        if (toiletKey == null)    missing.add("toiletKey");
        if (toiletDoor == null)   missing.add("toiletDoor");
        if (bedroomKey == null)   missing.add("bedroomKey");
        if (bedroomDoor == null)  missing.add("bedroomDoor");
        if (bedroomSleep == null) missing.add("bedroomSleep");
        if (frontDoorKey == null) missing.add("frontDoorKey");
        if (frontDoor == null)    missing.add("frontDoor");
        if (hideHouse == null)    missing.add("hideHouse");
        if (police == null)       missing.add("police");
        if (guSpawn == null)      missing.add("guSpawn");
        return missing;
    }

    public boolean isReady() {
        return missingPoints().isEmpty();
    }

    // ------------------------------------------------------------------
    // Getter / Setter
    // ------------------------------------------------------------------

    public String getName() { return name; }
    public File getFolder() { return folder; }
    public File getConfigFile() { return configFile; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String w) { this.worldName = w; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; }

    public Location getWaitLobby() { return waitLobby; }
    public void setWaitLobby(Location l) { this.waitLobby = l; }

    public Location getPlayerSpawn() { return playerSpawn; }
    public void setPlayerSpawn(Location l) { this.playerSpawn = l; }

    public Location getUnderKey() { return underKey; }
    public void setUnderKey(Location l) { this.underKey = l; }

    public Location getUnderDoor() { return underDoor; }
    public void setUnderDoor(Location l) { this.underDoor = l; }

    public Location getToiletKey() { return toiletKey; }
    public void setToiletKey(Location l) { this.toiletKey = l; }

    public Location getToiletDoor() { return toiletDoor; }
    public void setToiletDoor(Location l) { this.toiletDoor = l; }

    public Location getBedroomKey() { return bedroomKey; }
    public void setBedroomKey(Location l) { this.bedroomKey = l; }

    public Location getBedroomDoor() { return bedroomDoor; }
    public void setBedroomDoor(Location l) { this.bedroomDoor = l; }

    public Location getBedroomSleep() { return bedroomSleep; }
    public void setBedroomSleep(Location l) { this.bedroomSleep = l; }

    public Location getFrontDoorKey() { return frontDoorKey; }
    public void setFrontDoorKey(Location l) { this.frontDoorKey = l; }

    public Location getFrontDoor() { return frontDoor; }
    public void setFrontDoor(Location l) { this.frontDoor = l; }

    public Location getHideHouse() { return hideHouse; }
    public void setHideHouse(Location l) { this.hideHouse = l; }

    public Location getPolice() { return police; }
    public void setPolice(Location l) { this.police = l; }

    public Location getGuSpawn() { return guSpawn; }
    public void setGuSpawn(Location l) { this.guSpawn = l; }

    public List<Location> getPatrolPoints() { return patrolPoints; }

    public double getPickupRadius() { return pickupRadius; }
    public double getDoorRadius() { return doorRadius; }
    public int getDoorHoldSeconds() { return doorHoldSeconds; }
    public double getCaptureDistance() { return captureDistance; }

    public int getEscapeHouseSeconds() { return escapeHouseSeconds; }
    public double getHouseEscapeRadius() { return houseEscapeRadius; }
    public int getHideArrivalSeconds() { return hideArrivalSeconds; }
    public int getHideStaySeconds() { return hideStaySeconds; }
    public int getGuBlindnessSeconds() { return guBlindnessSeconds; }
    public int getNoiseGlowSeconds() { return noiseGlowSeconds; }
}