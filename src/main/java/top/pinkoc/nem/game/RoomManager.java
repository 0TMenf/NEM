package top.pinkoc.nem.game;

import org.bukkit.entity.Player;
import top.pinkoc.nem.NotchEscapeMini;
import top.pinkoc.nem.arena.Arena;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 管理所有正在运行的房间。
 *
 * <p>每张启用的地图对应一个 {@link GameRoom}。
 * 玩家的归属关系用一个 {@code UUID -> 地图名} 的映射来维护，
 * 避免每次都要遍历所有房间去查玩家。
 */
public class RoomManager {

    private final NotchEscapeMini plugin;

    /** key = 小写地图名 */
    private final Map<String, GameRoom> rooms = new LinkedHashMap<>();
    /** 玩家 UUID -> 所在房间（小写地图名） */
    private final Map<UUID, String> playerRoom = new HashMap<>();

    public RoomManager(NotchEscapeMini plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    /**
     * 为每张已启用的地图创建一个房间。
     * 通常在插件启动时以及地图启停、reload 后调用。
     */
    public void loadAll() {
        rooms.clear();
        playerRoom.clear();

        for (Arena arena : plugin.arenas().getArenas()) {
            if (arena.isEnabled()) {
                rooms.put(arena.getName().toLowerCase(), new GameRoom(plugin, arena));
            }
        }

        plugin.getLogger().info("已为 " + rooms.size() + " 张地图创建房间。");
    }

    /** 插件卸载时调用，关闭所有房间并踢出玩家。 */
    public void shutdown() {
        for (GameRoom room : rooms.values()) {
            room.shutdown();
        }
        rooms.clear();
        playerRoom.clear();
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /** 按地图名获取房间，大小写不敏感。 */
    public GameRoom get(String arenaName) {
        if (arenaName == null) return null;
        return rooms.get(arenaName.toLowerCase());
    }

    /** 全部房间。 */
    public Collection<GameRoom> getRooms() {
        return rooms.values();
    }

    /**
     * 查询玩家当前所在的房间。
     *
     * @return 若玩家不在任何房间，返回 {@code null}
     */
    public GameRoom getByPlayer(Player p) {
        if (p == null) return null;
        String name = playerRoom.get(p.getUniqueId());
        return name == null ? null : rooms.get(name);
    }

    /** 与 {@link #getByPlayer(Player)} 等价的 UUID 版本。 */
    public GameRoom getByPlayer(UUID uuid) {
        if (uuid == null) return null;
        String name = playerRoom.get(uuid);
        return name == null ? null : rooms.get(name);
    }

    /** 玩家是否已经处于某个房间中。 */
    public boolean isInRoom(Player p) {
        return p != null && playerRoom.containsKey(p.getUniqueId());
    }

    // ------------------------------------------------------------------
    // 加入 / 离开
    // ------------------------------------------------------------------

    /**
     * 尝试把玩家加入到指定地图的房间，并选择阵营。
     *
     * @return 成功返回 {@code true}；房间不存在、已在进行中、玩家已
     *         在其它房间、阵营已满等情况返回 {@code false}
     */
    public boolean joinRoom(Player p, String arenaName, Team team) {
        if (p == null || arenaName == null || team == null) return false;

        // 已经在某个房间
        if (playerRoom.containsKey(p.getUniqueId())) return false;

        GameRoom room = get(arenaName);
        if (room == null) return false;

        if (!room.join(p, team)) return false;

        playerRoom.put(p.getUniqueId(), arenaName.toLowerCase());
        return true;
    }

    /** 让玩家离开当前房间。若玩家不在任何房间，则什么也不做。 */
    public void leaveRoom(Player p) {
        if (p == null) return;

        String name = playerRoom.remove(p.getUniqueId());
        if (name == null) return;

        GameRoom room = rooms.get(name);
        if (room != null) {
            room.leave(p);
        }
    }

    /**
     * 玩家掉线时调用。等价于 {@link #leaveRoom(Player)}，但额外接受
     * 一个离线玩家对象（Paper 里 PlayerQuitEvent 依然会给 Player 实例）。
     */
    public void onPlayerQuit(Player p) {
        leaveRoom(p);
    }
}