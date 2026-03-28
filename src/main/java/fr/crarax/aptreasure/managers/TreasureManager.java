package fr.crarax.aptreasure.managers;

import fr.crarax.aptreasure.APTreasure;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active treasure items and player statistics.
 * All collections are thread-safe via ConcurrentHashMap.
 */
public final class TreasureManager {

    private final APTreasure plugin;

    /** Active treasure items: entity UUID -> Item entity. */
    private final Map<UUID, Item> activeTreasures = new ConcurrentHashMap<>();

    /** Tick at which each treasure was registered (for lifetime calculation). */
    private final Map<UUID, Long> spawnTicks = new ConcurrentHashMap<>();

    /** Total treasures caught per player UUID. */
    private final Map<UUID, Integer> playerStats = new ConcurrentHashMap<>();

    /** Players who have toggled treasure off for themselves. */
    private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new TreasureManager.
     *
     * @param plugin the owning plugin instance
     */
    public TreasureManager(APTreasure plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers an item entity as an active treasure.
     *
     * @param item the item entity to track
     */
    public void registerTreasure(Item item) {
        activeTreasures.put(item.getUniqueId(), item);
        spawnTicks.put(item.getUniqueId(), (long) plugin.getServer().getCurrentTick());
    }

    /**
     * Unregisters a treasure by its entity UUID.
     *
     * @param entityId the UUID of the item entity
     */
    public void unregisterTreasure(UUID entityId) {
        activeTreasures.remove(entityId);
        spawnTicks.remove(entityId);
    }

    /**
     * Checks whether the given entity UUID is an active treasure.
     *
     * @param entityId the entity UUID to check
     * @return true if the entity is a tracked treasure
     */
    public boolean isActiveTreasure(UUID entityId) {
        return activeTreasures.containsKey(entityId);
    }

    /**
     * Returns an unmodifiable view of all active treasures.
     *
     * @return map of entity UUID to Item
     */
    public Map<UUID, Item> getActiveTreasures() {
        return Collections.unmodifiableMap(activeTreasures);
    }

    /**
     * Gets the server tick at which a treasure was registered.
     *
     * @param entityId the entity UUID
     * @return the spawn tick, or -1 if not found
     */
    public long getSpawnTick(UUID entityId) {
        return spawnTicks.getOrDefault(entityId, -1L);
    }

    /**
     * Returns the current number of active treasures.
     *
     * @return active treasure count
     */
    public int getActiveCount() {
        return activeTreasures.size();
    }

    /**
     * Awards the catch bonus to a player for picking up a treasure item.
     * Doubles the stack (or applies configured multiplier), sends a message,
     * plays effects, and tracks stats. All on the main thread (caller must ensure this).
     *
     * @param player the player who caught the treasure
     * @param item   the treasure item entity
     */
    public void awardBonus(Player player, Item item) {
        UUID entityId = item.getUniqueId();
        if (!isActiveTreasure(entityId)) return;

        int multiplier = plugin.getConfig().getInt("bonus-multiplier", 2);
        ItemStack stack = item.getItemStack();
        int originalAmount = stack.getAmount();
        int bonusAmount = originalAmount * (multiplier - 1);

        // Increase the stack size on the dropped item before pickup completes
        stack.setAmount(originalAmount * multiplier);
        item.setItemStack(stack);

        // Track stats
        playerStats.merge(player.getUniqueId(), 1, Integer::sum);

        // Send congratulatory message
        plugin.getMessageManager().send(player, "catch.success",
                "{amount}", String.valueOf(bonusAmount));

        // Play success sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Spawn happy particles
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);

        // Unregister
        unregisterTreasure(entityId);
    }

    /**
     * Gets the total treasures caught by a player.
     *
     * @param playerId the player UUID
     * @return the total count
     */
    public int getPlayerCaughtCount(UUID playerId) {
        return playerStats.getOrDefault(playerId, 0);
    }

    /**
     * Checks if a player has disabled treasures for themselves.
     *
     * @param playerId the player UUID
     * @return true if disabled
     */
    public boolean isDisabled(UUID playerId) {
        return disabledPlayers.contains(playerId);
    }

    /**
     * Toggles the treasure feature for a player.
     *
     * @param playerId the player UUID
     * @return true if now enabled, false if now disabled
     */
    public boolean toggle(UUID playerId) {
        if (disabledPlayers.remove(playerId)) {
            return true; // Was disabled, now enabled
        }
        disabledPlayers.add(playerId);
        return false; // Now disabled
    }

    /**
     * Cleans up all tracked treasures and resets state.
     * Called on plugin disable.
     */
    public void cleanup() {
        activeTreasures.clear();
        spawnTicks.clear();
    }
}
