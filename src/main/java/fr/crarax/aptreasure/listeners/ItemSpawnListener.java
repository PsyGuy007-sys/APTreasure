package fr.crarax.aptreasure.listeners;

import fr.crarax.aptreasure.APTreasure;
import fr.crarax.aptreasure.managers.TreasureManager;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Listens for item spawns and pickup events to handle treasure creation and catching.
 */
public final class ItemSpawnListener implements Listener {

    /** Maximum distance (squared) to detect the player who dropped an item. */
    private static final double DROP_DETECT_RADIUS_SQ = 3.0 * 3.0;

    private final APTreasure plugin;

    /**
     * Creates a new ItemSpawnListener.
     *
     * @param plugin the owning plugin
     */
    public ItemSpawnListener(APTreasure plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles item spawn events. Rolls a chance to turn the item into a treasure
     * if it was dropped by a player who is not immune and has the feature enabled.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        TreasureManager manager = plugin.getTreasureManager();

        // Check max active limit
        int maxActive = plugin.getConfig().getInt("max-active", 20);
        if (manager.getActiveCount() >= maxActive) return;

        // Check blacklist
        Material material = item.getItemStack().getType();
        Set<Material> blacklist = plugin.getConfig().getStringList("blacklist").stream()
                .map(name -> {
                    try { return Material.valueOf(name.toUpperCase()); }
                    catch (IllegalArgumentException e) { return null; }
                })
                .filter(m -> m != null)
                .collect(Collectors.toSet());
        if (blacklist.contains(material)) return;

        // Find a nearby player who likely dropped the item
        Player dropper = findDropper(item);
        if (dropper == null) return;

        // Check immunity permission
        if (dropper.hasPermission("aptreasure.immune")) return;

        // Check if player has toggled off
        if (manager.isDisabled(dropper.getUniqueId())) return;

        // Roll the chance
        int chance = Math.clamp(plugin.getConfig().getInt("chance", 10), 1, 30);
        if (ThreadLocalRandom.current().nextInt(100) >= chance) return;

        // Register as treasure
        manager.registerTreasure(item);

        // Make the item non-despawnable while it's a treasure
        item.setUnlimitedLifetime(true);

        // Alert nearby players
        for (Entity entity : item.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Player nearby) {
                plugin.getMessageManager().send(nearby, "spawn.alert");
            }
        }
    }

    /**
     * Handles item pickup events. Awards bonus if the item is an active treasure.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Item item = event.getItem();
        TreasureManager manager = plugin.getTreasureManager();

        if (!manager.isActiveTreasure(item.getUniqueId())) return;

        // Award the bonus (this modifies the item stack before pickup completes)
        manager.awardBonus(player, item);
    }

    /**
     * Finds the player who most likely dropped the item by checking for
     * the nearest player within a 3-block radius.
     *
     * @param item the spawned item entity
     * @return the nearest player, or null if none found
     */
    private Player findDropper(Item item) {
        List<Entity> nearby = item.getNearbyEntities(3, 3, 3);
        Player closest = null;
        double closestDistSq = DROP_DETECT_RADIUS_SQ;

        for (Entity entity : nearby) {
            if (entity instanceof Player player) {
                double distSq = player.getLocation().distanceSquared(item.getLocation());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = player;
                }
            }
        }
        return closest;
    }
}
