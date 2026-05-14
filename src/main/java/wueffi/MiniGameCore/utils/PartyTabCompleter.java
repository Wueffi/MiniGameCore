package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.commands.PartyCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PartyTabCompleter implements TabCompleter {
    private final MiniGameCore plugin;
    private static final Map<String, String> commandsPermissions = PartyCommand.getCommandsPermissions();
    public PartyTabCompleter(MiniGameCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return null;
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions = commandsPermissions.entrySet().stream()
                    .filter(e -> player.hasPermission(e.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();
        } else if (args.length == 2) {
            String subcmd = args[0].toLowerCase();

            if (!(commandsPermissions.containsKey(subcmd) && player.hasPermission(commandsPermissions.get(subcmd)))) {
                return completions;
            }

            switch (subcmd) {
                case "join", "invite", "deny":
                    if (!plugin.getBannedPlayers().contains(player.getUniqueId())) {
                        completions = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                    }
                    break;
            } // well that was easy
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }
}