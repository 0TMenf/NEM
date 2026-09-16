package top.pinkoc.nem.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import top.pinkoc.nem.NotchEscapeMini;
import top.pinkoc.nem.arena.Arena;
import top.pinkoc.nem.util.Util;

import java.time.Duration;
import java.util.*;

public class GameRoom {

    public enum State { WAITING, COUNTDOWN, PLAYING, ENDING, LOADING }

    private final NotchEscapeMini plugin;
    private final Arena arena;
    private final Map<UUID, GamePlayer> players = new LinkedHashMap<>();

    private State state = State.WAITING;
    private BukkitTask tickTask;
    private int countdownTaskId = -1;
    private long countdownEndsAt;
    private int tickCounter;

    // 全局阶段控制
    private boolean frontDoorOpened = false;
    private long frontDoorOpenedAt = 0;
    private boolean guAwake = false;
    private int guAwakeBlindnessId = -1;

    public GameRoom(NotchEscapeMini plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    public Arena getArena() { return arena; }
    public State getState() { return state; }
    public Map<UUID, GamePlayer> getPlayers() { return players; }
    public int size() { return players.size(); }

    public int countTeam(Team t) {
        int n = 0;
        for (GamePlayer p : players.values()) if (p.getTeam() == t) n++;
        return n;
    }

    public boolean contains(UUID id) { return players.containsKey(id); }
    public GamePlayer get(UUID id) { return players.get(id); }

    public RoomState getPublicState() {
        if (state == State.LOADING) return RoomState.LOADING;
        if (!arena.isEnabled()) return RoomState.DISABLED;
        if (state == State.PLAYING || state == State.ENDING) return RoomState.RUNNING;
        if (players.size() >= plugin.getConfig().getInt("game.max-players", 24)) return RoomState.FULL;
        return RoomState.WAITING;
    }

    public boolean isReadyPublic() {
        return arena.isReady()
                && countTeam(Team.GU) >= 1
                && countTeam(Team.NOTCH) >= 2;
    }

    // ------------------------------------------------------------------
    // 加入 / 离开
    // ------------------------------------------------------------------

    public boolean join(Player p, Team team) {
        if (state == State.PLAYING || state == State.ENDING || state == State.LOADING) return false;
        if (players.containsKey(p.getUniqueId())) return false;

        int max = plugin.getConfig().getInt("game.max-players", 24);
        if (players.size() >= max) return false;

        int maxGu = plugin.getConfig().getInt("game.max-gu", 6);
        int maxNotch = plugin.getConfig().getInt("game.max-notch", 18);
        if (team == Team.GU && countTeam(Team.GU) >= maxGu) return false;
        if (team == Team.NOTCH && countTeam(Team.NOTCH) >= maxNotch) return false;

        GamePlayer gp = new GamePlayer(p.getUniqueId());
        gp.setTeam(team);
        players.put(p.getUniqueId(), gp);

        teleportToLobby(p);
        broadcast(plugin.lang().get("join.joined-room", "arena", arena.getName()));
        broadcast(plugin.lang().get("join.joined-team",
                "team", team == Team.GU ? "古振兴" : "Notch"));

        checkStartConditions();
        return true;
    }

    public void leave(Player p) {
        GamePlayer gp = players.remove(p.getUniqueId());
        if (gp == null) return;
        broadcast(plugin.lang().get("join.left-room"));
        Location fb = Bukkit.getWorlds().get(0).getSpawnLocation();
        p.teleport(fb);
        checkStartConditions();
    }

    public void kickAll() {
        for (UUID id : new ArrayList<>(players.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                for (PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) {
                    p.removePotionEffect(pe.getType());
                }
            }
        }
    }

    private void teleportToLobby(Player p) {
        World w = arena.loadOrCreateWorld();
        if (w == null) return;
        Location lobby = arena.getWaitLobby();
        if (lobby == null) lobby = w.getSpawnLocation();
        p.teleport(lobby);
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setFireTicks(0);
        for (PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) {
            p.removePotionEffect(pe.getType());
        }
    }

    // ------------------------------------------------------------------
    // 开始
    // ------------------------------------------------------------------

    private void checkStartConditions() {
        int min = plugin.getConfig().getInt("game.min-players", 3);

        if (players.size() < min || !isReadyPublic()) {
            if (countdownTaskId != -1) {
                Bukkit.getScheduler().cancelTask(countdownTaskId);
                countdownTaskId = -1;
                broadcast(plugin.lang().get("join.countdown-cancelled"));
            }
            state = State.WAITING;
            return;
        }

        if (state == State.WAITING) startCountdown();
    }

    private void startCountdown() {
        state = State.COUNTDOWN;
        int seconds = plugin.getConfig().getInt("game.start-countdown", 30);
        countdownEndsAt = System.currentTimeMillis() + seconds * 1000L;

        if (countdownTaskId != -1) Bukkit.getScheduler().cancelTask(countdownTaskId);
        countdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (state != State.COUNTDOWN) return;
            long remain = countdownEndsAt - System.currentTimeMillis();
            int sec = (int) Math.ceil(remain / 1000.0);
            if (sec <= 0) {
                Bukkit.getScheduler().cancelTask(countdownTaskId);
                countdownTaskId = -1;
                beginGame();
                return;
            }
            if (sec <= 5 || sec % 5 == 0) {
                broadcast(plugin.lang().get("join.countdown", "seconds", String.valueOf(sec)));
            }
        }, 0L, 20L);
    }

    public void forceStart() {
        if (players.isEmpty()) return;
        this.state = State.WAITING;
        beginGame();
    }

    private void beginGame() {
        state = State.PLAYING;
        tickCounter = 0;
        frontDoorOpened = false;
        guAwake = false;

        for (GamePlayer gp : players.values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p == null || !p.isOnline()) continue;
            resetPlayerEffects(p);

            if (gp.isNotch()) {
                p.teleport(arena.getPlayerSpawn());
                p.setGameMode(GameMode.ADVENTURE);
                p.getInventory().clear();
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
                p.showTitle(Title.title(
                        Component.text("你醒了过来", NamedTextColor.DARK_RED),
                        Component.text("这里是古振兴的地下室……", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(3), Duration.ofSeconds(1))
                ));
                p.sendMessage(Component.text(plugin.lang().get("game.welcome")));
            } else {
                // 古振兴玩家
                Location spawn = arena.getGuSpawn() != null
                        ? arena.getGuSpawn()
                        : arena.getBedroomSleep();
                p.teleport(spawn);
                p.setGameMode(GameMode.ADVENTURE);
                p.getInventory().clear();
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);

                // 给一把钻石剑
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta meta = sword.getItemMeta();
                meta.displayName(Component.text("§c古振兴的猎刀"));
                meta.lore(List.of(Component.text("§7砍 3 刀即可杀死 Notch")));
                sword.setItemMeta(meta);
                p.getInventory().addItem(sword);

                // 躺床上假睡
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        Integer.MAX_VALUE, 10, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,
                        Integer.MAX_VALUE, 128, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false));
                p.sendMessage(Component.text(plugin.lang().get("game.gu-sword-received")));
                p.sendMessage(Component.text("§7你躺到床上假装睡着……等他们上钩。"));
            }
        }

        broadcast(plugin.lang().get("command.game-start"));
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void resetPlayerEffects(Player p) {
        for (PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) {
            p.removePotionEffect(pe.getType());
        }
    }

    // ------------------------------------------------------------------
    // 主循环
    // ------------------------------------------------------------------

    private void tick() {
        if (state != State.PLAYING) return;
        tickCounter++;

        boolean anyNotchAlive = false;

        for (UUID id : new ArrayList<>(players.keySet())) {
            GamePlayer gp = players.get(id);
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            if (!gp.isNotch() || !gp.isAlive()) continue;

            anyNotchAlive = true;
            tickNotch(p, gp);
        }

        if (!anyNotchAlive) {
            gameOver(false, plugin.lang().get("game.gu-victory"));
            return;
        }

        for (GamePlayer gp : players.values()) {
            if (gp.isNotch() && gp.isWon()) {
                gameOver(true, plugin.lang().get("game.notch-win"));
                return;
            }
        }
    }

    private void tickNotch(Player p, GamePlayer gp) {
        // ---- 阶段 1：地下室钥匙 ----
        if (!gp.hasUnderKey()) {
            if (near(p, arena.getUnderKey(), arena.getPickupRadius())) {
                gp.setHasUnderKey(true);
                p.sendMessage(Component.text(plugin.lang().get("game.pick-under-key")));
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.3f);
                giveKeyItem(p, "地下室钥匙");
            }
            return;
        }

        // ---- 阶段 2：厕所钥匙 ----
        if (!gp.hasToiletKey()) {
            if (near(p, arena.getToiletKey(), arena.getPickupRadius())) {
                gp.setHasToiletKey(true);
                p.sendMessage(Component.text(plugin.lang().get("game.pick-toilet-key")));
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.3f);
                giveKeyItem(p, "厕所钥匙");
            }
            return;
        }

        // ---- 阶段 2.5：卧室钥匙 ----
        if (!gp.hasBedroomKey()) {
            if (near(p, arena.getBedroomKey(), arena.getPickupRadius())) {
                gp.setHasBedroomKey(true);
                p.sendMessage(Component.text(plugin.lang().get("game.pick-bedroom-key")));
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.3f);
                giveKeyItem(p, "卧室钥匙");
            }
            return;
        }

        // ---- 阶段 3：卧室，必须潜行拿大门钥匙 ----
        if (!gp.hasFrontKey()) {
            boolean inBedroom = near(p, arena.getBedroomSleep(), 12.0);
            if (inBedroom && !p.isSneaking()) {
                p.sendActionBar(Component.text(plugin.lang().get("actionbar.sleep-warning")));
                if (!guAwake && near(p, arena.getFrontDoorKey(), arena.getPickupRadius() + 3)) {
                    wakeGu("noise");
                }
                return;
            }
            if (near(p, arena.getFrontDoorKey(), arena.getPickupRadius())) {
                if (!p.isSneaking()) {
                    if (!guAwake) wakeGu("noise");
                    p.sendActionBar(Component.text(plugin.lang().get("actionbar.noise-warning")));
                    return;
                }
                gp.setHasFrontKey(true);
                p.sendMessage(Component.text(plugin.lang().get("game.pick-front-key")));
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.3f);
                giveKeyItem(p, "大门钥匙");
            }
            return;
        }

        // ---- 阶段 4：打开大门 ----
        if (!frontDoorOpened) {
            if (near(p, arena.getFrontDoor(), arena.getDoorRadius())) {
                frontDoorOpened = true;
                frontDoorOpenedAt = System.currentTimeMillis();

                // 通知所有 Notch
                for (GamePlayer other : players.values()) {
                    if (!other.isNotch()) continue;
                    Player op = Bukkit.getPlayer(other.getUuid());
                    if (op == null || !op.isOnline()) continue;
                    op.sendMessage(Component.text(plugin.lang().get("game.front-door-open")));
                    op.playSound(op.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 2f, 0.5f);
                }
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 2f, 0.5f);
            }
            return;
        }

        // ---- 阶段 5：冲出大门（5 秒内） ----
        long timeSinceOpen = System.currentTimeMillis() - frontDoorOpenedAt;
        long escapeMs = arena.getEscapeHouseSeconds() * 1000L;

        if (!gp.isHouseExited()) {
            if (timeSinceOpen >= escapeMs) {
                eliminate(p, gp, plugin.lang().get("game.escape-house-timeout"));
                return;
            }
            if (near(p, arena.getFrontDoor(), arena.getHouseEscapeRadius())) {
                int sec = (int) Math.ceil((escapeMs - timeSinceOpen) / 1000.0);
                p.sendActionBar(Component.text(plugin.lang().get("actionbar.house-exit-timer",
                        "seconds", String.valueOf(sec))));
                return;
            }
            gp.setHouseExited(true);
            p.sendMessage(Component.text(plugin.lang().get("game.house-exited")));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }

        // ---- 阶段 5.5：30 秒内抵达 hideHouse ----
        if (!gp.isAtHideHouse()) {
            long totalMs = escapeMs + arena.getHideArrivalSeconds() * 1000L;
            long remain = totalMs - timeSinceOpen;
            if (remain <= 0) {
                eliminate(p, gp, plugin.lang().get("game.hide-arrival-timeout"));
                return;
            }
            int sec = (int) Math.ceil(remain / 1000.0);
            p.sendActionBar(Component.text(plugin.lang().get("actionbar.arrival-timer",
                    "seconds", String.valueOf(sec))));

            if (near(p, arena.getHideHouse(), arena.getDoorRadius() + 2)) {
                gp.setAtHideHouse(true);
                gp.setHideStartAt(System.currentTimeMillis());
                gp.setStaticPos(p.getLocation().clone());
                p.sendMessage(Component.text(plugin.lang().get("game.hide-arrived")));
                p.playSound(p.getLocation(), Sound.BLOCK_WOOL_PLACE, 0.8f, 1.2f);
            }
            return;
        }

        // ---- 阶段 6：静止不动 30 秒 ----
        if (!gp.isHidden()) {
            // 必须待在里面
            if (!near(p, arena.getHideHouse(), arena.getDoorRadius() + 4)) {
                eliminate(p, gp, plugin.lang().get("game.hide-arrival-timeout"));
                return;
            }

            long elapsed = System.currentTimeMillis() - gp.getHideStartAt();
            int remain = arena.getHideStaySeconds() - (int) (elapsed / 1000);

            if (remain <= 0) {
                gp.setHidden(true);
                p.sendMessage(Component.text(plugin.lang().get("game.hide-stay-done")));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            } else {
                p.sendActionBar(Component.text(plugin.lang().get("actionbar.hide-timer",
                        "seconds", String.valueOf(remain))));

                // 静止检查：与基准位置偏差 > 0.5 格
                Location base = gp.getStaticPos();
                if (base != null && p.getLocation().distanceSquared(base) > 0.25) {
                    // 检测到移动
                    gp.setStaticPos(p.getLocation().clone());
                    onPlayerMoved(p, gp);
                }
            }
            return;
        }

        // ---- 阶段 7：报警 ----
        if (near(p, arena.getPolice(), arena.getDoorRadius() + 2)) {
            gp.setWon(true);
            p.showTitle(Title.title(
                    Component.text("报警成功", NamedTextColor.GREEN),
                    Component.text("古振兴被带走了", NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
        }
    }

    /**
     * Notch 在躲藏阶段移动 → 全身发光 + 广播位置给所有古振兴。
     * 不会立即死亡，古振兴需要亲自去砍 3 刀。
     */
    private void onPlayerMoved(Player p, GamePlayer gp) {
        // 白色描边效果
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,
                arena.getNoiseGlowSeconds() * 20, 0, false, false));

        // 通知自己
        p.sendActionBar(Component.text(plugin.lang().get("game.hide-moved-self")));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);

        if (!gp.tryShowNoiseWarning()) return;

        // 广播给所有古振兴
        Location loc = p.getLocation();
        String msg = plugin.lang().get("game.hide-moved-broadcast",
                "player", p.getName(),
                "x", String.valueOf((int) loc.getX()),
                "y", String.valueOf((int) loc.getY()),
                "z", String.valueOf((int) loc.getZ()));

        for (GamePlayer gu : players.values()) {
            if (!gu.isGu()) continue;
            Player gup = Bukkit.getPlayer(gu.getUuid());
            if (gup == null || !gup.isOnline()) continue;
            gup.sendMessage(Component.text(msg));
            gup.playSound(gup.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.6f);
        }
    }

    // ------------------------------------------------------------------
    // 古振兴
    // ------------------------------------------------------------------

    private void wakeGu(String reason) {
        if (guAwake) return;
        guAwake = true;

        for (GamePlayer gp : players.values()) {
            if (!gp.isGu()) continue;
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p == null || !p.isOnline()) continue;

            p.removePotionEffect(PotionEffectType.SLOWNESS);
            p.removePotionEffect(PotionEffectType.JUMP_BOOST);
            p.removePotionEffect(PotionEffectType.BLINDNESS);

            // 40 秒黑暗
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                    arena.getGuBlindnessSeconds() * 20, 0, false, false));

            p.sendMessage(Component.text(plugin.lang().get("game.gu-awake")));
            p.sendMessage(Component.text(plugin.lang().get("game.gu-blinded")));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
        }

        // 40 秒后清除黑暗提示
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (GamePlayer gp : players.values()) {
                if (!gp.isGu()) continue;
                Player p = Bukkit.getPlayer(gp.getUuid());
                if (p == null || !p.isOnline()) continue;
                p.sendMessage(Component.text(plugin.lang().get("game.gu-blindness-cleared")));
            }
        }, arena.getGuBlindnessSeconds() * 20L);

        broadcast("§4古振兴醒了……");
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private void eliminate(Player p, GamePlayer gp, String reason) {
        gp.setAlive(false);
        p.setGameMode(GameMode.SPECTATOR);
        p.showTitle(Title.title(
                Component.text("你被抓住了", NamedTextColor.DARK_RED),
                Component.text(reason, NamedTextColor.RED),
                Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);
        broadcast(reason);
    }

    private boolean near(Player p, Location loc, double radius) {
        if (loc == null || !Util.sameWorld(p.getLocation(), loc)) return false;
        return p.getLocation().distanceSquared(loc) <= radius * radius;
    }

    private void giveKeyItem(Player p, String name) {
        ItemStack stack = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("§6" + name));
        stack.setItemMeta(meta);
        p.getInventory().addItem(stack);
    }

    // ------------------------------------------------------------------
    // 结束 / 重置
    // ------------------------------------------------------------------

    private void gameOver(boolean notchWin, String reason) {
        if (state == State.ENDING) return;
        state = State.ENDING;
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }

        broadcast(reason);

        for (GamePlayer gp : players.values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p == null || !p.isOnline()) continue;

            if (notchWin && gp.isNotch()) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                p.showTitle(Title.title(
                        Component.text("逃出生天", NamedTextColor.GREEN),
                        Component.text("你成功了", NamedTextColor.WHITE),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofSeconds(1))
                ));
            } else if (!notchWin && gp.isGu()) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                p.showTitle(Title.title(
                        Component.text("狩猎成功", NamedTextColor.DARK_RED),
                        Component.text("他们都留下了", NamedTextColor.RED),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofSeconds(1))
                ));
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::reset, 100L);
    }

    public void reset() {
        state = State.LOADING;
        players.clear();
        guAwake = false;
        frontDoorOpened = false;

        Bukkit.getScheduler().runTask(plugin, () -> {
            arena.unloadWorld();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                arena.loadOrCreateWorld();
                state = State.WAITING;
                plugin.getLogger().info("房间 " + arena.getName() + " 已重置。");
            }, 40L);
        });
    }

    public void shutdown() {
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
        kickAll();
    }

    private void broadcast(String msg) {
        Component c = Component.text(msg);
        for (UUID id : players.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) p.sendMessage(c);
        }
    }
}