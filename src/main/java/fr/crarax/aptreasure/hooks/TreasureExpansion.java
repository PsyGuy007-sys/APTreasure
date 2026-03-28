package fr.crarax.aptreasure.hooks;

import fr.crarax.aptreasure.APTreasure;
import fr.crarax.aptreasure.managers.TreasureManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for APTreasure.
 * Provides the following placeholders:
 * <ul>
 *   <li>{@code %aptreasure_total_caught%} - Total treasures caught by the player</li>
 *   <li>{@code %aptreasure_enabled%} - Whether the player has treasures enabled</li>
 *   <li>{@code %aptreasure_active_count%} - Current number of alive treasures server-wide</li>
 * </ul>
 */
public final class TreasureExpansion extends PlaceholderExpansion {

    private final APTreasure plugin;

    /**
     * Creates a new TreasureExpansion.
     *
     * @param plugin the owning plugin
     */
    public TreasureExpansion(APTreasure plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "aptreasure";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getPluginMeta().getAuthors().stream()
                .findFirst()
                .orElse("Crarax");
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        TreasureManager manager = plugin.getTreasureManager();

        return switch (params.toLowerCase()) {
            case "total_caught" -> {
                if (player == null) yield "0";
                yield String.valueOf(manager.getPlayerCaughtCount(player.getUniqueId()));
            }
            case "enabled" -> {
                if (player == null) yield "true";
                yield String.valueOf(!manager.isDisabled(player.getUniqueId()));
            }
            case "active_count" -> String.valueOf(manager.getActiveCount());
            default -> null;
        };
    }
}
