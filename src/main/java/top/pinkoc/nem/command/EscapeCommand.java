package top.pinkoc.nem.command;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.pinkoc.nem.NotchEscapeMini;
import top.pinkoc.nem.arena.Arena;
import top.pinkoc.nem.game.GameRoom;
import top.pinkoc.nem.gui.RoomGUI;
import top.pinkoc.nem.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public class EscapeCommand implements CommandExecutor, TabCompleter {

    private final NotchEscapeMini plugin;
    private final Map<UUID, String> editing = new HashMap<>();

    public EscapeCommand(NotchEscapeMini plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender, label); return true; }

        String sub = args[0].toLowerCase();

        // ==================================================
        //  玩家命令（默认所有玩家可用）
        // ==================================================
        if (sub.equals("gui")) {
            if (!sender.hasPermission("notchescape.player")) {
                sender.sendMessage(Component.text(plugin.lang().get("command.no-permission")));
                return true;
            }
            gui(sender);
            return true;
        }
        if (sub.equals("leave")) {
            if (!sender.hasPermission("notchescape.player")) {
                sender.sendMessage(Component.text(plugin.lang().get("command.no-permission")));
                return true;
            }
            leave(sender);
            return true;
        }
        if (sub.equals("help")) {
            help(sender, label);
            return true;
        }

        // ==================================================
        //  管理员命令（默认仅 OP）
        // ==================================================
        if (!sender.hasPermission("notchescape.admin")) {
            sender.sendMessage(Component.text(plugin.lang().get("command.no-permission")));
            return true;
        }

        switch (sub) {
            case "create"        -> create(sender, args);
            case "delete"        -> delete(sender, args);
            case "edit"          -> edit(sender, args);
            case "list"          -> list(sender);
            case "save"          -> save(sender);
            case "enablesave", "enable" -> enableSave(sender, args);
            case "disable"       -> disable(sender, args);

            case "waitlobby", "setwaitlobby" -> setPoint(sender, PointType.WAIT_LOBBY);
            case "playerspawn"               -> setPoint(sender, PointType.PLAYER_SPAWN);
            case "setunderkey"               -> setPoint(sender, PointType.UNDER_KEY);
            case "setunderdoor"              -> setPoint(sender, PointType.UNDER_DOOR);
            case "settoiletkey"              -> setPoint(sender, PointType.TOILET_KEY);
            case "settoiletdoor"             -> setPoint(sender, PointType.TOILET_DOOR);
            case "setbedroomkey"             -> setPoint(sender, PointType.BEDROOM_KEY);
            case "setbedroomdoor"            -> setPoint(sender, PointType.BEDROOM_DOOR);
            case "setbedroomsleep"           -> setPoint(sender, PointType.BEDROOM_SLEEP);
            case "setfrontdoorkey"           -> setPoint(sender, PointType.FRONT_DOOR_KEY);
            case "setfrontdoor"              -> setPoint(sender, PointType.FRONT_DOOR);
            case "sethidehouse"              -> setPoint(sender, PointType.HIDE_HOUSE);
            case "setpolice"                 -> setPoint(sender, PointType.POLICE);
            case "setguspawn"                -> setPoint(sender, PointType.GU_SPAWN);
            case "addpatrol"                 -> addPatrol(sender);
            case "clearpatrol"               -> clearPatrol(sender);

            case "start"         -> start(sender, args);
            case "stop"          -> stop(sender, args);
            case "status"        -> status(sender, args);
            case "reload"        -> reload(sender);
            default              -> help(sender, label);
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 玩家命令
    // ------------------------------------------------------------------

    private void gui(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text(plugin.lang().get("command.only-player")));
            return;
        }
        new RoomGUI(plugin).openRoomSelector(p);
    }

    private void leave(CommandSender sender) {
        if (!(sender instanceof Player p)) return;
        if (plugin.rooms().getByPlayer(p) == null) {
            p.sendMessage(Component.text(plugin.lang().get("command.not-in-room")));
            return;
        }
        plugin.rooms().leaveRoom(p);
    }

    // ------------------------------------------------------------------
    // 地图管理
    // ------------------------------------------------------------------

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("§c用法: /ne create <名称>")); return; }
        if (plugin.arenas().exists(args[1])) {
            sender.sendMessage(Component.text(plugin.lang().get("command.arena-exists", "name", args[1])));
            return;
        }
        plugin.arenas().create(args[1]);
        sender.sendMessage(Component.text(plugin.lang().get("command.arena-created", "name", args[1])));
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("§c用法: /ne delete <名称>")); return; }
        if (!plugin.arenas().delete(args[1])) {
            sender.sendMessage(Component.text(plugin.lang().get("command.arena-not-found", "name", args[1])));
            return;
        }
        plugin.rooms().loadAll();
        sender.sendMessage(Component.text(plugin.lang().get("command.arena-deleted", "name", args[1])));
    }

    private void edit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text(plugin.lang().get("command.only-player")));
            return;
        }
        if (args.length < 2) { sender.sendMessage(Component.text("§c用法: /ne edit <名称>")); return; }
        Arena a = plugin.arenas().get(args[1]);
        if (a == null) {
            sender.sendMessage(Component.text(plugin.lang().get("command.arena-not-found", "name", args[1])));
            return;
        }
        editing.put(p.getUniqueId(), a.getName());
        World w = a.loadOrCreateWorld();
        if (w == null) { p.sendMessage(Component.text("§c世界加载失败。")); return; }

        Location target = a.getWaitLobby() != null ? a.getWaitLobby() : w.getSpawnLocation();
        p.teleport(target);
        p.setGameMode(GameMode.CREATIVE);

        p.sendMessage(Component.text("§a===== 编辑 " + a.getName() + " ====="));
        p.sendMessage(Component.text("§e/ne waitLobby §7- 等待大厅"));
        p.sendMessage(Component.text("§e/ne playerSpawn §7- 醒来点"));
        p.sendMessage(Component.text("§e/ne setUnderKey §7- 地下室钥匙"));
        p.sendMessage(Component.text("§e/ne setUnderDoor §7- 地下室门"));
        p.sendMessage(Component.text("§e/ne setToiletKey §7- 厕所钥匙"));
        p.sendMessage(Component.text("§e/ne setToiletDoor §7- 厕所门"));
        p.sendMessage(Component.text("§e/ne setBedroomKey §7- 卧室钥匙"));
        p.sendMessage(Component.text("§e/ne setBedroomDoor §7- 卧室门"));
        p.sendMessage(Component.text("§e/ne setBedroomSleep §7- 古振兴睡觉点"));
        p.sendMessage(Component.text("§e/ne setFrontDoorKey §7- 大门钥匙"));
        p.sendMessage(Component.text("§e/ne setFrontDoor §7- 大门"));
        p.sendMessage(Component.text("§e/ne setHideHouse §7- 废弃屋子"));
        p.sendMessage(Component.text("§e/ne setPolice §7- 警察局"));
        p.sendMessage(Component.text("§e/ne setGuSpawn §7- 古振兴出生点"));
        p.sendMessage(Component.text("§e/ne addPatrol §7- 添加巡逻点"));
        p.sendMessage(Component.text("§e/ne save §7- 保存"));
        p.sendMessage(Component.text("§e/ne enableSave " + a.getName() + " §7- 启用"));
    }

    private void list(CommandSender sender) {
        var arenas = plugin.arenas().getArenas();
        if (arenas.isEmpty()) {
            sender.sendMessage(Component.text("§7还没有任何地图。§e/ne create <名称>"));
            return;
        }
        sender.sendMessage(Component.text("§6===== 地图列表 (" + arenas.size() + ") ====="));
        for (Arena a : arenas) {
            GameRoom room = plugin.rooms().get(a.getName());
            String state = room == null ? "§7未加载" : room.getPublicState().name();
            String ready = a.isReady() ? "§a就绪" : "§e缺: " + String.join(", ", a.missingPoints());
            sender.sendMessage(Component.text(" §e" + a.getName() + " §8| "
                    + (a.isEnabled() ? "§a启用" : "§c禁用") + " §8| " + state + " §8| " + ready));
        }
    }

    // ------------------------------------------------------------------
    // 编辑点位
    // ------------------------------------------------------------------

    private enum PointType {
        WAIT_LOBBY("waitLobby", "等待大厅"),
        PLAYER_SPAWN("playerSpawn", "醒来点"),
        UNDER_KEY("underKey", "地下室钥匙"),
        UNDER_DOOR("underDoor", "地下室门"),
        TOILET_KEY("toiletKey", "厕所钥匙"),
        TOILET_DOOR("toiletDoor", "厕所门"),
        BEDROOM_KEY("bedroomKey", "卧室钥匙"),
        BEDROOM_DOOR("bedroomDoor", "卧室门"),
        BEDROOM_SLEEP("bedroomSleep", "古振兴睡觉点"),
        FRONT_DOOR_KEY("frontDoorKey", "大门钥匙"),
        FRONT_DOOR("frontDoor", "大门"),
        HIDE_HOUSE("hideHouse", "废弃屋子"),
        POLICE("police", "警察局"),
        GU_SPAWN("guSpawn", "古振兴出生点");

        final String label;
        final String desc;
        PointType(String l, String d) { this.label = l; this.desc = d; }
    }

    private void setPoint(CommandSender sender, PointType type) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text(plugin.lang().get("command.only-player")));
            return;
        }
        Arena a = getEditing(p);
        if (a == null) {
            p.sendMessage(Component.text(plugin.lang().get("command.edit-not-editing")));
            return;
        }

        Location loc = p.getLocation();
        switch (type) {
            case WAIT_LOBBY    -> a.setWaitLobby(loc);
            case PLAYER_SPAWN  -> a.setPlayerSpawn(loc);
            case UNDER_KEY     -> a.setUnderKey(loc);
            case UNDER_DOOR    -> a.setUnderDoor(loc);
            case TOILET_KEY    -> a.setToiletKey(loc);
            case TOILET_DOOR   -> a.setToiletDoor(loc);
            case BEDROOM_KEY   -> a.setBedroomKey(loc);
            case BEDROOM_DOOR  -> a.setBedroomDoor(loc);
            case BEDROOM_SLEEP -> a.setBedroomSleep(loc);
            case FRONT_DOOR_KEY-> a.setFrontDoorKey(loc);
            case FRONT_DOOR    -> a.setFrontDoor(loc);
            case HIDE_HOUSE    -> a.setHideHouse(loc);
            case POLICE        -> a.setPolice(loc);
            case GU_SPAWN      -> a.setGuSpawn(loc);
        }
        p.sendMessage(Component.text(plugin.lang().get("command.point-set",
                "point", type.desc, "location", Util.serialize(loc))));
    }

    private void addPatrol(CommandSender sender) {
        if (!(sender instanceof Player p)) return;
        Arena a = getEditing(p);
        if (a == null) { p.sendMessage(Component.text(plugin.lang().get("command.edit-not-editing"))); return; }
        a.getPatrolPoints().add(p.getLocation());
        p.sendMessage(Component.text(plugin.lang().get("command.patrol-added",
                "index", String.valueOf(a.getPatrolPoints().size()),
                "location", Util.serialize(p.getLocation()))));
    }

    private void clearPatrol(CommandSender sender) {
        if (!(sender instanceof Player p)) return;
        Arena a = getEditing(p);
        if (a == null) return;
        a.getPatrolPoints().clear();
        p.sendMessage(Component.text(plugin.lang().get("command.patrol-cleared")));
    }

    // ------------------------------------------------------------------
    // 保存 / 启用 / 禁用
    // ------------------------------------------------------------------

    private void save(CommandSender sender) {
        if (!(sender instanceof Player p)) return;
        Arena a = getEditing(p);
        if (a == null) { p.sendMessage(Component.text(plugin.lang().get("command.edit-not-editing"))); return; }
        a.save();
        p.sendMessage(Component.text(plugin.lang().get("command.arena-saved", "name", a.getName())));
        List<String> missing = a.missingPoints();
        if (!missing.isEmpty()) p.sendMessage(Component.text("§e还缺少: " + String.join(", ", missing)));
    }

    private void enableSave(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("§c用法: /ne enableSave <地图>")); return; }
        Arena a = plugin.arenas().get(args[1]);
        if (a == null) {
            sender.sendMessage(Component.text(plugin.lang().get("command.arena-not-found", "name", args[1])));
            return;
        }
        List<String> missing = a.missingPoints();
        if (!missing.isEmpty()) {
            sender.sendMessage(Component.text(plugin.lang().get("command.arena-not-ready",
                    "missing", String.join(", ", missing))));
            return;
        }
        a.setEnabled(true);
        a.save();
        a.loadOrCreateWorld();
        plugin.rooms().loadAll();
        sender.sendMessage(Component.text(plugin.lang().get("command.arena-enabled", "name", a.getName())));
    }

    private void disable(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("§c用法: /ne disable <地图>")); return; }
        Arena a = plugin.arenas().get(args[1]);
        if (a == null) {
            sender.sendMessage(Component.text(plugin.lang().get("command.arena-not-found", "name", args[1])));
            return;
        }
        GameRoom room = plugin.rooms().get(a.getName());
        if (room != null && room.getState() == GameRoom.State.PLAYING) {
            sender.sendMessage(Component.text("§c该地图正在游戏中，先 /ne stop"));
            return;
        }
        a.setEnabled(false);
        a.save();
        a.unloadWorld();
        plugin.rooms().loadAll();
        sender.sendMessage(Component.text(plugin.lang().get("command.arena-disabled", "name", a.getName())));
    }

    // ------------------------------------------------------------------
    // 游戏控制
    // ------------------------------------------------------------------

    private void start(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("§c用法: /ne start <地图>")); return; }
        Arena a = plugin.arenas().get(args[1]);
        if (a == null) {
            sender.sendMessage(Component.text(plugin.lang().get("command.arena-not-found", "name", args[1])));
            return;
        }
        GameRoom room = plugin.rooms().get(a.getName());
        if (room == null) {
            sender.sendMessage(Component.text("§c该地图未启用，请先 /ne enableSave"));
            return;
        }
        if (room.getState() == GameRoom.State.PLAYING) {
            sender.sendMessage(Component.text("§c该地图已经在对局中。"));
            return;
        }
        if (!room.isReadyPublic()) {
            sender.sendMessage(Component.text(plugin.lang().get("command.arena-not-ready",
                    "missing", String.join(", ", a.missingPoints()))));
            return;
        }
        room.forceStart();
        sender.sendMessage(Component.text(plugin.lang().get("command.game-start")));
    }

    private void stop(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Component.text("§c用法: /ne stop <地图>")); return; }
        GameRoom room = plugin.rooms().get(args[1]);
        if (room == null) {
            sender.sendMessage(Component.text(plugin.lang().get("command.not-in-game")));
            return;
        }
        room.reset();
        sender.sendMessage(Component.text(plugin.lang().get("command.game-stopped", "name", args[1])));
    }

    private void status(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Arena a = plugin.arenas().get(args[1]);
            if (a == null) {
                sender.sendMessage(Component.text(plugin.lang().get("command.arena-not-found", "name", args[1])));
                return;
            }
            printArena(sender, a);
            return;
        }
        sender.sendMessage(Component.text("§6===== Notch_Escape_Mini ====="));
        for (Arena a : plugin.arenas().getArenas()) printArena(sender, a);
    }

    private void printArena(CommandSender sender, Arena a) {
        GameRoom room = plugin.rooms().get(a.getName());
        sender.sendMessage(Component.text("§6--- " + a.getName() + " ---"));
        sender.sendMessage(Component.text("§7启用: §f" + a.isEnabled() + " §7世界已载入: §f" + a.isWorldLoaded()));
        sender.sendMessage(Component.text("§7房间状态: §f" + (room == null ? "未创建" : room.getPublicState())));
        if (room != null) {
            sender.sendMessage(Component.text("§7玩家: §f" + room.size()
                    + " §7(GU: §c" + room.countTeam(top.pinkoc.nem.game.Team.GU)
                    + "§7, Notch: §a" + room.countTeam(top.pinkoc.nem.game.Team.NOTCH) + "§7)"));
        }
    }

    private void reload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.lang().load();
        plugin.arenas().loadAll();
        plugin.rooms().loadAll();
        sender.sendMessage(Component.text(plugin.lang().get("command.reloaded")));
    }

    private Arena getEditing(Player p) {
        String name = editing.get(p.getUniqueId());
        return name == null ? null : plugin.arenas().get(name);
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(Component.text("§6===== Notch_Escape_Mini ====="));
        sender.sendMessage(Component.text("§e/" + label + " gui §7- 打开房间选择"));
        sender.sendMessage(Component.text("§e/" + label + " leave §7- 离开房间"));
        if (sender.hasPermission("notchescape.admin")) {
            sender.sendMessage(Component.text("§e/" + label + " create/delete/edit <名称>"));
            sender.sendMessage(Component.text("§e/" + label + " waitLobby / playerSpawn"));
            sender.sendMessage(Component.text("§e/" + label + " setUnderKey / setUnderDoor"));
            sender.sendMessage(Component.text("§e/" + label + " setToiletKey / setToiletDoor"));
            sender.sendMessage(Component.text("§e/" + label + " setBedroomKey / setBedroomDoor / setBedroomSleep"));
            sender.sendMessage(Component.text("§e/" + label + " setFrontDoorKey / setFrontDoor"));
            sender.sendMessage(Component.text("§e/" + label + " setHideHouse / setPolice / setGuSpawn"));
            sender.sendMessage(Component.text("§e/" + label + " addPatrol / clearPatrol"));
            sender.sendMessage(Component.text("§e/" + label + " save / enableSave <地图> / disable <地图>"));
            sender.sendMessage(Component.text("§e/" + label + " start / stop / status / list / reload"));
        }
    }

    // ------------------------------------------------------------------
    // Tab 补全
    // ------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> roots = new ArrayList<>(Arrays.asList("gui", "leave", "help"));
            if (sender.hasPermission("notchescape.admin")) {
                roots.addAll(Arrays.asList(
                        "create", "delete", "edit", "list", "save",
                        "enableSave", "enable", "disable",
                        "waitLobby", "playerSpawn",
                        "setUnderKey", "setUnderDoor",
                        "setToiletKey", "setToiletDoor",
                        "setBedroomKey", "setBedroomDoor", "setBedroomSleep",
                        "setFrontDoorKey", "setFrontDoor",
                        "setHideHouse", "setPolice", "setGuSpawn",
                        "addPatrol", "clearPatrol",
                        "start", "stop", "status", "reload"));
            }
            return filter(roots, args[0]);
        }
        if (!sender.hasPermission("notchescape.admin")) return List.of();

        String sub = args[0].toLowerCase();
        boolean needsArena = switch (sub) {
            case "edit", "delete", "enablesave", "enable", "disable",
                 "start", "stop", "status" -> true;
            default -> false;
        };
        if (needsArena && args.length == 2) {
            return filter(plugin.arenas().getArenas().stream()
                    .map(Arena::getName).collect(Collectors.toList()), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase();
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(p))
                .collect(Collectors.toList());
    }
}