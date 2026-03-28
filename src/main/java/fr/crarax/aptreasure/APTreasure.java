package fr.crarax.aptreasure;

import fr.crarax.aptreasure.commands.TreasureCommand;
import fr.crarax.aptreasure.hooks.TreasureExpansion;
import fr.crarax.aptreasure.listeners.ItemSpawnListener;
import fr.crarax.aptreasure.managers.MessageManager;
import fr.crarax.aptreasure.managers.TreasureManager;
import fr.crarax.aptreasure.tasks.TreasureAnimationTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * APTreasure - Dropped items come alive and run away from players!
 * When a player drops an item, there is a configurable chance it starts
 * moving on its own, playing sounds and spawning particles. If the player
 * catches it, they receive a bonus.
 *
 * @author Crarax
 */
public final class APTreasure extends JavaPlugin {

    private MessageManager messageManager;
    private TreasureManager treasureManager;
    private BukkitTask animationTask;

    @Override
    public void onEnable() {
        // Save default configs
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Initialize managers
        messageManager = new MessageManager(this);
        treasureManager = new TreasureManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new ItemSpawnListener(this), this);

        // Register command
        PluginCommand command = getCommand("aptreasure");
        if (command != null) {
            TreasureCommand executor = new TreasureCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        // Start animation task
        startAnimationTask();

        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TreasureExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("APTreasure v" + getPluginMeta().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel animation task
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }

        // Clean up tracked treasures
        if (treasureManager != null) {
            treasureManager.cleanup();
        }

        getLogger().info("APTreasure disabled.");
    }

    /**
     * Starts (or restarts) the animation task with the configured tick interval.
     */
    public void startAnimationTask() {
        if (animationTask != null) {
            animationTask.cancel();
        }
        int tickInterval = Math.clamp(getConfig().getInt("tick-interval", 5), 1, 40);
        TreasureAnimationTask task = new TreasureAnimationTask(this);
        animationTask = task.runTaskTimer(this, tickInterval, tickInterval);
    }

    /**
     * Gets the message manager for accessing localized messages.
     *
     * @return the MessageManager instance
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Gets the treasure manager for tracking active treasures and stats.
     *
     * @return the TreasureManager instance
     */
    public TreasureManager getTreasureManager() {
        return treasureManager;
    }
}
