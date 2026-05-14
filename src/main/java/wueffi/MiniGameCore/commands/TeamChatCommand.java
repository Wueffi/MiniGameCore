package wueffi.MiniGameCore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.utils.Lobby;
import wueffi.MiniGameCore.utils.Team;

public final class TeamChatCommand implements CommandExecutor {

    public TeamChatCommand() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (!(player.hasPermission("mgcore.teamchat"))) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§fUsage: §6/teamchat <message>");
            return true;
        }

        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby == null) {
            player.sendMessage("§cYou are not in a Lobby!");
            return true;
        }
        Team team = lobby.getTeamByPlayer(player);
        if (team == null) {
            player.sendMessage("§cYou are not in a Team!");
            return true;
        }

        for (Player player1 : team.getPlayers()) {
            player1.sendMessage("§7[§6TEAM§7] " + team.getColorCode() + player.getName() + "§7: §f" + String.join(" ", args));
        }

        return true;
    }
}