package top.pinkoc.nem.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.pinkoc.nem.NotchEscapeMini;
import top.pinkoc.nem.game.RoomState;
import top.pinkoc.nem.game.Team;
import top.pinkoc.nem.gui.GUIHolder;
import top.pinkoc.nem.gui.RoomGUI;

/**
 * 处理 ChestUI 的点击事件：
 *   1. 房间选择界面 → 打开该房间的阵营选择界面
 *   2. 阵营选择界面 → 加入对应阵营
 */
public class GUIListener implements Listener {

    private final NotchEscapeMini plugin;

    public GUIListener(NotchEscapeMini plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        // 只处理我们自己的 GUI
        if (!(e.getInventory().getHolder() instanceof GUIHolder holder)) return;

        // 阻止玩家把界面里的物品拿出来
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (holder.getType()) {
            case ROOM_SELECTOR -> handleRoomClick(p, clicked);
            case TEAM_SELECTOR -> handleTeamClick(p, holder, clicked);
        }
    }

    // ------------------------------------------------------------------
    // 房间选择
    // ------------------------------------------------------------------

    private void handleRoomClick(Player p, ItemStack clicked) {
        // 从物品的显示名反推地图名
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.displayName() == null) return;

        String arenaName = PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName())
                .replace("§e", "")  // 去掉颜色字符
                .trim();

        p.closeInventory();

        plugin.rooms().getRooms().stream()
                .filter(r -> r.getArena().getName().equals(arenaName))
                .findFirst()
                .ifPresentOrElse(room -> {
                    RoomState state = room.getPublicState();
                    if (state == RoomState.WAITING) {
                        // 玩家已在别的房间 → 不允许再进
                        if (plugin.rooms().getByPlayer(p) != null) {
                            p.sendMessage(Component.text(
                                    plugin.lang().get("command.already-in-room")));
                            return;
                        }
                        new RoomGUI(plugin).openTeamSelector(p, arenaName);
                    } else if (state == RoomState.LOADING) {
                        p.sendMessage(Component.text(
                                plugin.lang().get("command.arena-loading", "name", arenaName)));
                    } else if (state == RoomState.FULL) {
                        p.sendMessage(Component.text(
                                plugin.lang().get("command.arena-full")));
                    } else {
                        p.sendMessage(Component.text(
                                plugin.lang().get("gui.room-running")));
                    }
                }, () -> p.sendMessage(Component.text(
                        plugin.lang().get("command.arena-not-found", "name", arenaName))));
    }

    // ------------------------------------------------------------------
    // 阵营选择
    // ------------------------------------------------------------------

    private void handleTeamClick(Player p, GUIHolder holder, ItemStack clicked) {
        String arenaName = holder.getArenaName();
        if (arenaName == null) return;

        Team team = switch (clicked.getType()) {
            case NETHERITE_SWORD -> Team.GU;
            case DIAMOND_SWORD   -> Team.NOTCH;
            default -> null;
        };
        if (team == null) return;

        p.closeInventory();

        // 已经在别的房间
        if (plugin.rooms().getByPlayer(p) != null) {
            p.sendMessage(Component.text(plugin.lang().get("command.already-in-room")));
            return;
        }

        boolean ok = plugin.rooms().joinRoom(p, arenaName, team);
        if (!ok) {
            p.sendMessage(Component.text(plugin.lang().get("command.arena-full")));
        }
    }
}