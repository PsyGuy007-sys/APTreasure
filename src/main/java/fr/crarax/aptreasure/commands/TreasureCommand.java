package fr.crarax.aptreasure.commands;

import fr.crarax.aptreasure.APTreasure;
import fr.crarax.aptreasure.managers.MessageManager;
import fr.crarax.aptreasure.managers.TreasureManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Handles the /aptreasure command with subcommands: reload, toggle, stats.
 */
public final class TreasureCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("reload", "toggle", "stats");

    private final APTreasure plugin;

    /**
     * Creates a new TreasureCommand handler.
     *
     * @param plugin the owning plugin
     */
    public TreasureCommand(APTreasure plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        MessageManager messages = plugin.getMessageManager();

        if (args.length == 0) {
            messages.send(sender, "command.usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender, messages);
            case "toggle" -> handleToggle(sender, messages);
            case "stats" -> handleStats(sender, messages);
            default -> messages.send(sender, "command.unknown");
        }

        return true;
    }

    /**
     * Handles the reload subcommand.
     */
    private void handleReload(CommandSender sender, MessageManager messages) {
        if (!sender.hasPermission("aptreasure.reload")) {
            messages.send(sender, "reload.no-permission");
            return;
        }
        plugin.reloadConfig();
        messages.reload();
        messages.send(sender, "reload.success");
    }

    /**
     * Handles the toggle subcommand (player-only).
     */
    private void handleToggle(CommandSender sender, MessageManager messages) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.player-only");
            return;
        }
        if (!player.hasPermission("aptreasure.toggle")) {
            messages.send(player, "toggle.no-permission");
            return;
        }

        TreasureManager manager = plugin.getTreasureManager();
        boolean enabled = manager.toggle(player.getUniqueId());
        messages.send(player, enabled ? "toggle.enabled" : "toggle.disabled");
    }

    /**
     * Handles the stats subcommand (player-only).
     */
    private void handleStats(CommandSender sender, MessageManager messages) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.player-only");
            return;
        }
        if (!player.hasPermission("aptreasure.stats")) {
            messages.send(player, "stats.no-permission");
            return;
        }

        TreasureManager manager = plugin.getTreasureManager();
        int count = manager.getPlayerCaughtCount(player.getUniqueId());
        messages.sendNoPrefix(player, "stats.header");
        messages.sendNoPrefix(player, "stats.total-caught", "{count}", String.valueOf(count));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.startsWith(input))
                    .filter(sub -> switch (sub) {
                        case "reload" -> sender.hasPermission("aptreasure.reload");
                        case "toggle" -> sender.hasPermission("aptreasure.toggle");
                        case "stats" -> sender.hasPermission("aptreasure.stats");
                        default -> false;
                    })
                    .toList();
        }
        return List.of();
    }
}
