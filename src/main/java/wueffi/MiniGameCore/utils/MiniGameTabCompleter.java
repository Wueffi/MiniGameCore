package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.commands.MiniGameCommand;
import wueffi.MiniGameCore.managers.GameManager;
import wueffi.MiniGameCore.managers.LobbyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MiniGameTabCompleter implements TabCompleter {
    private final MiniGameCore plugin;
    private static final Map<String, String> commandsPermissions = MiniGameCommand.getCommandsPermissions();

    public MiniGameTabCompleter(MiniGameCore plugin) {
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
                case "host":
                    if (!plugin.getBannedPlayers().contains(player.getUniqueId())) {
                        completions = plugin.getAvailableGames();
                    }
                    break;
                case "join":
                    if (!plugin.getBannedPlayers().contains(player.getUniqueId())) {
                        completions = LobbyManager.getOpenLobbies().stream()
                                .filter(l -> player.hasPermission(GameManager.getConfig(l).getJoinPerm()))
                                .map(Lobby::getLobbyId)
                                .toList();
                    }
                    break;
                case "spectate":
                    completions = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    for (Lobby lobby : LobbyManager.getOpenLobbies()) {
                        String lobbyId = lobby.getLobbyId();
                        completions.add(lobbyId);
                    }
                    break;
                case "stats", "ban", "unban": // yes that is correct!
                    completions = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    break;
                case "stop":
                    completions = new ArrayList<>();
                    for (Lobby lobby : LobbyManager.getOpenLobbies()) {
                        String lobbyId = lobby.getLobbyId();
                        completions.add(lobbyId);
                    }
                    break;
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}