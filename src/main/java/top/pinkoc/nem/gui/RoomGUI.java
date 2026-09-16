package top.pinkoc.nem.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.pinkoc.nem.NotchEscapeMini;
import top.pinkoc.nem.arena.Arena;
import top.pinkoc.nem.game.GameRoom;
import top.pinkoc.nem.game.RoomState;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间选择（/ne gui）+ 阵营选择两个 ChestUI。
 */
public class RoomGUI {

    private final NotchEscapeMini plugin;

    public RoomGUI(NotchEscapeMini plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // 房间列表
    // ------------------------------------------------------------------

    public void openRoomSelector(Player p) {
        GUIHolder holder = new GUIHolder(GUIHolder.Type.ROOM_SELECTOR);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text(plugin.lang().get("gui.room-title")));
        holder.setInventory(inv);

        int slot = 10;
        for (GameRoom room : plugin.rooms().getRooms()) {
            if (slot >= 17) break; // 只显示一行最多 7 个
            inv.setItem(slot++, buildRoomIcon(room));
        }

        p.openInventory(inv);
    }

    private ItemStack buildRoomIcon(GameRoom room) {
        Arena arena = room.getArena();
        RoomState state = room.getPublicState();

        Material icon = switch (state) {
            case LOADING -> Material.REDSTONE_BLOCK;
            case WAITING -> Material.GREEN_WOOL;
            case RUNNING, FULL -> Material.RED_CONCRETE;
            case DISABLED -> Material.BARRIER;
        };

        String stateName = switch (state) {
            case LOADING -> plugin.lang().get("gui.room-loading");
            case WAITING -> plugin.lang().get("gui.room-waiting",
                    "current", String.valueOf(room.size()),
                    "max", String.valueOf(plugin.getConfig().getInt("game.max-players", 24)));
            case RUNNING -> plugin.lang().get("gui.room-running");
            case FULL -> plugin.lang().get("gui.room-full");
            case DISABLED -> "§c未启用";
        };

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e" + arena.getName()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(plugin.lang().get("gui.room-lore", "name", arena.getName())));
        lore.add(Component.text(plugin.lang().get("gui.room-lore-state", "state", stateName)));
        lore.add(Component.text(plugin.lang().get("gui.room-lore-players",
                "current", String.valueOf(room.size()),
                "max", String.valueOf(plugin.getConfig().getInt("game.max-players", 24)))));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------------------------------------------
    // 阵营选择
    // ------------------------------------------------------------------

    public void openTeamSelector(Player p, String arenaName) {
        GUIHolder holder = new GUIHolder(GUIHolder.Type.TEAM_SELECTOR, arenaName);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text(plugin.lang().get("gui.team-title", "arena", arenaName)));
        holder.setInventory(inv);

        GameRoom room = plugin.rooms().get(arenaName);
        int guCount = room == null ? 0 : room.countTeam(top.pinkoc.nem.game.Team.GU);
        int notchCount = room == null ? 0 : room.countTeam(top.pinkoc.nem.game.Team.NOTCH);
        int maxGu = plugin.getConfig().getInt("game.max-gu", 6);
        int maxNotch = plugin.getConfig().getInt("game.max-notch", 18);

        inv.setItem(11, buildTeamIcon(Material.NETHERITE_SWORD,
                plugin.lang().get("gui.team-gu"),
                guCount < maxGu ? plugin.lang().get("gui.team-gu-lore")
                        : plugin.lang().get("gui.team-gu-full")));

        inv.setItem(15, buildTeamIcon(Material.DIAMOND_SWORD,
                plugin.lang().get("gui.team-notch"),
                notchCount < maxNotch ? plugin.lang().get("gui.team-notch-lore")
                        : plugin.lang().get("gui.team-notch-full")));

        p.openInventory(inv);
    }

    private ItemStack buildTeamIcon(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(List.of(Component.text(lore)));
        item.setItemMeta(meta);
        return item;
    }
}