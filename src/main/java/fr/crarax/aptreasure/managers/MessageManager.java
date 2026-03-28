package fr.crarax.aptreasure.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages loading, caching, and sending messages from messages.yml.
 * Supports {@code &} color codes via Adventure's LegacyComponentSerializer
 * and PlaceholderAPI placeholders when available.
 */
public final class MessageManager {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final File messagesFile;
    private final boolean papiAvailable;

    /** Cached raw strings from messages.yml (key path -> raw string with & codes). */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private FileConfiguration messagesConfig;

    /**
     * Creates a new MessageManager and loads messages from disk.
     *
     * @param plugin the owning plugin
     */
    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        reload();
    }

    /**
     * Reloads messages.yml from disk and clears the cache.
     */
    public void reload() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        cache.clear();
        loadRecursive("", messagesConfig);
    }

    /**
     * Recursively loads all string values from the configuration into the cache.
     */
    private void loadRecursive(String parentPath, org.bukkit.configuration.ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String fullPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            if (section.isConfigurationSection(key)) {
                loadRecursive(fullPath, section.getConfigurationSection(key));
            } else if (section.isString(key)) {
                cache.put(fullPath, section.getString(key));
            }
        }
    }

    /**
     * Gets a raw message string by key, with optional placeholder replacements.
     * Returns the key itself if not found.
     *
     * @param key          the message key (e.g. "catch.success")
     * @param replacements pairs of placeholder/value (e.g. "{amount}", "5")
     * @return the raw string with replacements applied
     */
    public String getRaw(String key, String... replacements) {
        String raw = cache.getOrDefault(key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return raw;
    }

    /**
     * Gets the configured prefix.
     *
     * @return the raw prefix string
     */
    public String getPrefix() {
        return cache.getOrDefault("prefix", "&8[&6APTreasure&8] &r");
    }

    /**
     * Parses a raw message string into an Adventure Component, applying PAPI
     * placeholders if a player is provided.
     *
     * @param raw    the raw message string with {@code &} color codes
     * @param player the player for PAPI placeholders, or null
     * @return the parsed Adventure Component
     */
    public Component parse(String raw, Player player) {
        String processed = raw;
        if (papiAvailable && player != null) {
            processed = PlaceholderAPI.setPlaceholders(player, processed);
        }
        return LEGACY_SERIALIZER.deserialize(processed);
    }

    /**
     * Sends a prefixed message to a CommandSender.
     *
     * @param sender       the recipient
     * @param key          the message key
     * @param replacements pairs of placeholder/value
     */
    public void send(CommandSender sender, String key, String... replacements) {
        String raw = getPrefix() + getRaw(key, replacements);
        Player player = (sender instanceof Player p) ? p : null;
        sender.sendMessage(parse(raw, player));
    }

    /**
     * Sends a prefixed message to a Player.
     *
     * @param player       the recipient
     * @param key          the message key
     * @param replacements pairs of placeholder/value
     */
    public void send(Player player, String key, String... replacements) {
        String raw = getPrefix() + getRaw(key, replacements);
        player.sendMessage(parse(raw, player));
    }

    /**
     * Sends a message without prefix to a Player.
     *
     * @param player       the recipient
     * @param key          the message key
     * @param replacements pairs of placeholder/value
     */
    public void sendNoPrefix(Player player, String key, String... replacements) {
        String raw = getRaw(key, replacements);
        player.sendMessage(parse(raw, player));
    }
}
