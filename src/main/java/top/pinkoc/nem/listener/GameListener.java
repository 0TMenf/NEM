package top.pinkoc.nem.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import top.pinkoc.nem.NotchEscapeMini;
import top.pinkoc.nem.game.GamePlayer;
import top.pinkoc.nem.game.GameRoom;

import java.time.Duration;

/**
 * 游戏过程中的事件监听。
 */
public class GameListener implements Listener {

    private final NotchEscapeMini plugin;

    public GameListener(NotchEscapeMini plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.rooms().leaveRoom(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        if (inGame(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (inGame(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && inGame(p)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (inGame(e.getPlayer())) e.setCancelled(true);
    }

    // ------------------------------------------------------------------
    // 伤害控制
    // ------------------------------------------------------------------

    /**
     * 只允许古振兴的钻石剑伤害 Notch，其它伤害一律取消。
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        GameRoom room = plugin.rooms().getByPlayer(victim);
        if (room == null) return;

        boolean allowed = false;

        if (e instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Player attacker) {
                GamePlayer vg = room.get(victim.getUniqueId());
                GamePlayer ag = room.get(attacker.getUniqueId());
                if (vg != null && ag != null && ag.isGu() && vg.isNotch()) {
                    ItemStack hand = attacker.getInventory().getItemInMainHand();
                    if (hand != null && hand.getType() == Material.DIAMOND_SWORD) {
                        allowed = true;

                        // 击杀判定：如果这次伤害会把 Notch 血量打到 0 以下
                        if (victim.getHealth() - e.getFinalDamage() <= 0) {
                            vg.setAlive(false);
                            victim.setGameMode(GameMode.SPECTATOR);

                            attacker.sendMessage(Component.text(
                                    plugin.lang().get("game.gu-killed",
                                            "player", victim.getName())));

                            victim.showTitle(Title.title(
                                    Component.text("你被砍倒了", NamedTextColor.DARK_RED),
                                    Component.text("古振兴获胜了", NamedTextColor.RED),
                                    Title.Times.times(
                                            Duration.ofMillis(400),
                                            Duration.ofSeconds(3),
                                            Duration.ofSeconds(1))
                            ));
                            victim.playSound(victim.getLocation(),
                                    Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);

                            plugin.getLogger().info("古振兴 " + attacker.getName()
                                    + " 砍死了 " + victim.getName());
                        }
                    }
                }
            }
        }

        if (!allowed) e.setCancelled(true);
    }

    // ------------------------------------------------------------------
    // 古振兴睡觉期间禁止移动
    // ------------------------------------------------------------------

    /**
     * 古振兴刚开局时被施以 SLOWNESS 10（相当于完全定身）。
     * 这里额外做一个“位置锁定”，避免他被推动或挤动。
     */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        GameRoom room = plugin.rooms().getByPlayer(p);
        if (room == null) return;

        GamePlayer gp = room.get(p.getUniqueId());
        if (gp == null || !gp.isGu()) return;
        if (room.getState() != GameRoom.State.PLAYING) return;

        if (p.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            var pe = p.getPotionEffect(PotionEffectType.SLOWNESS);
            if (pe != null && pe.getAmplifier() >= 10) {
                if (e.getFrom().distanceSquared(e.getTo()) > 0.0001) {
                    e.setTo(e.getFrom());
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private boolean inGame(Player p) {
        return plugin.rooms().getByPlayer(p) != null;
    }
}