package top.pinkoc.nem.arena;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理 plugins/Notch_Escape_Mini/arenas/ 下所有竞技场（地图）。
 *
 * <p>目录结构：
 * <pre>
 * plugins/Notch_Escape_Mini/
 *   └── arenas/
 *       ├── house1/
 *       │   └── arena.yml
 *       └── house2/
 *           └── arena.yml
 * </pre>
 *
 * <p>对应的世界文件夹保存在服务器根目录：
 * <pre>
 * &lt;server&gt;/ne_house1/
 * &lt;server&gt;/ne_house2/
 * </pre>
 */
public class ArenaManager {

    private final JavaPlugin plugin;
    private final File arenasFolder;
    /** key = 小写地图名 */
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenasFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            arenasFolder.mkdirs();
        }
    }

    /**
     * 从磁盘重新加载全部竞技场。
     * 已在使用的对局不会被打断（由 RoomManager 负责）。
     */
    public void loadAll() {
        arenas.clear();

        File[] files = arenasFolder.listFiles();
        if (files == null) {
            plugin.getLogger().info("已加载 0 张地图。");
            return;
        }

        for (File f : files) {
            if (!f.isDirectory()) continue;
            File cfg = new File(f, "arena.yml");
            if (!cfg.exists()) continue;

            Arena arena = new Arena(plugin, f.getName());
            arena.load();
            arenas.put(f.getName().toLowerCase(), arena);

            // 已启用的地图，服务器启动时自动载入世界
            if (arena.isEnabled()) {
                arena.loadOrCreateWorld();
            }
        }

        plugin.getLogger().info("已加载 " + arenas.size() + " 张地图。");
    }

    /**
     * 创建一张新地图（只会创建插件目录，不会立刻加载世界）。
     *
     * @return 成功返回新地图，如果同名已存在返回 {@code null}
     */
    public Arena create(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.toLowerCase();
        if (arenas.containsKey(key)) return null;

        Arena arena = new Arena(plugin, name);
        arena.save(); // 立刻落盘一个空配置
        arenas.put(key, arena);
        return arena;
    }

    /** 按名称获取地图，大小写不敏感。 */
    public Arena get(String name) {
        if (name == null) return null;
        return arenas.get(name.toLowerCase());
    }

    /** 判断地图是否存在。 */
    public boolean exists(String name) {
        return name != null && arenas.containsKey(name.toLowerCase());
    }

    /**
     * 删除地图：
     * <ol>
     *   <li>卸载世界</li>
     *   <li>删除 plugins/Notch_Escape_Mini/arenas/&lt;name&gt;/</li>
     *   <li>删除服务器根目录下的世界文件夹 ne_&lt;name&gt;/</li>
     * </ol>
     *
     * @return 是否成功删除
     */
    public boolean delete(String name) {
        Arena arena = arenas.remove(name.toLowerCase());
        if (arena == null) return false;

        arena.unloadWorld();

        // 删除插件配置
        deleteRecursive(arena.getFolder());

        // 删除世界文件夹
        File worldFolder = new File(plugin.getServer().getWorldContainer(), arena.getWorldName());
        deleteRecursive(worldFolder);

        return true;
    }

    /** 返回全部地图（保持加载顺序）。 */
    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    /** 插件卸载时调用：把所有已加载的世界卸载。 */
    public void unloadAll() {
        for (Arena arena : arenas.values()) {
            if (arena.isWorldLoaded()) {
                arena.unloadWorld();
            }
        }
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursive(c);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}