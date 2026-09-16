package top.pinkoc.nem.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 自定义 ChestUI 持有者，用于识别我们自己的界面。
 */
public class GUIHolder implements InventoryHolder {

    public enum Type { ROOM_SELECTOR, TEAM_SELECTOR }

    private final Type type;
    private final String arenaName;
    private Inventory inventory;

    public GUIHolder(Type type) {
        this(type, null);
    }

    public GUIHolder(Type type, String arenaName) {
        this.type = type;
        this.arenaName = arenaName;
    }

    public Type getType() { return type; }

    public String getArenaName() { return arenaName; }

    public void setInventory(Inventory inv) { this.inventory = inv; }

    @Override
    public Inventory getInventory() { return inventory; }
}