package top.pinkoc.nem;

import org.bukkit.plugin.java.JavaPlugin;
import top.pinkoc.nem.arena.ArenaManager;
import top.pinkoc.nem.command.EscapeCommand;
import top.pinkoc.nem.config.Lang;
import top.pinkoc.nem.game.RoomManager;
import top.pinkoc.nem.listener.GameListener;
import top.pinkoc.nem.listener.GUIListener;

public final class NotchEscapeMini extends JavaPlugin {

    private static NotchEscapeMini instance;

    private Lang lang;
    private ArenaManager arenaManager;
    private RoomManager roomManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.lang = new Lang(this);
        this.lang.load();

        this.arenaManager = new ArenaManager(this);
        this.arenaManager.loadAll();

        this.roomManager = new RoomManager(this);
        this.roomManager.loadAll();

        EscapeCommand cmd = new EscapeCommand(this);
        var c = getCommand("notchescape");
        if (c != null) {
            c.setExecutor(cmd);
            c.setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        getLogger().info("Notch_Escape_Mini Alpha-0.0.1 加载完成！竞技场: "
                + arenaManager.getArenas().size());
    }

    @Override
    public void onDisable() {
        if (roomManager != null) roomManager.shutdown();
        if (arenaManager != null) arenaManager.unloadAll();
    }

    public static NotchEscapeMini get() { return instance; }
    public Lang lang() { return lang; }
    public ArenaManager arenas() { return arenaManager; }
    public RoomManager rooms() { return roomManager; }
}