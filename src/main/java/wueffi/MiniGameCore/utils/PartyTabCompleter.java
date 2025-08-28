package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import wueffi.MiniGameCore.MiniGameCore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PartyTabCompleter implements TabCompleter {
    private final MiniGameCore plugin;

    public PartyTabCompleter(MiniGameCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return null;
        }

        List<String> completions = new ArrayList<>();

        String[] commands = {"create", "leave", "join", "invite", "deny", "list"};
        String[] permissions = {
                "mgcore.party.create", "mgcore.party.join", "mgcore.party.join", "mgcore.party.invite", "mgcore.party.invite",
                "mgcore.party.list"
        };

        if (args.length == 1) {
            for (int i = 0; i < commands.length; i++) {
                if (player.hasPermission(permissions[i])) {
                    completions.add(commands[i]);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join":
                    if (player.hasPermission("mgcore.party.join")) {
                        if (!plugin.getBannedPlayers().contains(player.getUniqueId())) {
                            completions = new ArrayList<>();
                            for (Player player1 : Bukkit.getOnlinePlayers()) {
                                String Name = player1.getName();
                                completions.add(Name);
                            }
                        }
                    }
                    break;
                case "invite", "deny":
                    if (player.hasPermission("mgcore.party.invite")) {
                        if (!plugin.getBannedPlayers().contains(player.getUniqueId())) {
                            completions = new ArrayList<>();
                            for (Player player1 : Bukkit.getOnlinePlayers()) {
                                String Name = player1.getName();
                                completions.add(Name);
                            }
                        }
                    }
                    break;
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}