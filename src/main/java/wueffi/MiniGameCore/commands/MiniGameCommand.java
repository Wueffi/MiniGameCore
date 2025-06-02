package wueffi.MiniGameCore.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.managers.GameManager;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.managers.ScoreBoardManager;
import wueffi.MiniGameCore.utils.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.bukkit.Bukkit.getLogger;

public class MiniGameCommand implements CommandExecutor {
    private final MiniGameCore plugin;

    public MiniGameCommand(MiniGameCore plugin) {
        this.plugin = plugin;
    }

    private static @NotNull HashMap<String, String> getCommandsPermissions() {
        HashMap<String, String> commands_permissions = new HashMap<>();
        commands_permissions.put("host", "mgcore.host");
        commands_permissions.put("join", "mgcore.join");
        commands_permissions.put("confirm", "mgcore.confirm");
        commands_permissions.put("ready", "mgcore.ready");
        commands_permissions.put("unready", "mgcore.unready");
        commands_permissions.put("leave", "mgcore.leave");
        commands_permissions.put("start", "mgcore.start");
        commands_permissions.put("spectate", "mgcore.spectate");
        commands_permissions.put("reload", "mgcore.admin");
        commands_permissions.put("stopall", "mgcore.admin");
        commands_permissions.put("stop", "mgcore.admin");
        commands_permissions.put("ban", "mgcore.admin");
        commands_permissions.put("unban", "mgcore.admin");
        return commands_permissions;
    }

    private static final Map<Player, Lobby> confirmations = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        LobbyManager lobbyManager = LobbyManager.getInstance();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Yo console User, only players can use this command!");
            return true;
        }
        HashMap<String, String> commands_permissions = getCommandsPermissions();

        if (args.length < 1) {
            StringBuilder availableCommands = new StringBuilder("§fUsage: §6/mg <");

            for (String command: commands_permissions.keySet()) {
                if (player.hasPermission(commands_permissions.get(command))) {
                    availableCommands.append(command).append(" | ");
                }
            }
            if (!availableCommands.isEmpty()) {
                availableCommands.setLength(availableCommands.length() - 3);
            }
            availableCommands.append(">");
            player.sendMessage(availableCommands.toString());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "host":
                if (plugin.getBannedPlayers().contains(player.getUniqueId())) {
                    player.sendMessage("§cYou were banned by an Administrator.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg host <game>");
                    return true;
                }
                if (!player.hasPermission("mgcore.host")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                if (LobbyManager.getLobbyByPlayer(player) != null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are already in another lobby!");
                    return true;
                }
                String gameName = args[1];
                if (!plugin.getAvailableGames().contains(gameName)) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cGame " + gameName + " not available!");
                    return true;
                }
                GameManager gameManager = new GameManager(plugin);
                gameManager.hostGame(gameName, sender);
                player.sendMessage("§8[§6MiniGameCore§8]§a Hosting game: " + args[1]);
                ScoreBoardManager.setPlayerStatus(player, "WAITING");
                Lobby lobby = LobbyManager.getLobbyByPlayer(player);
                lobby.setLobbyState("WAITING");
                player.sendMessage("§8[§6MiniGameCore§8]§a If you are ready use /mg ready to ready-up!");
                break;

            case "join":
                if (plugin.getBannedPlayers().contains(player.getUniqueId())) {
                    player.sendMessage("§cYou were banned by an Administrator.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg join <game>");
                    return true;
                }

                String lobbyName = args[1];
                lobby = lobbyManager.getLobby(lobbyName);

                if (LobbyManager.getLobbyByPlayer(player) != null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are already in another lobby!");
                    return true;
                }

                if (!Objects.equals(lobby.getLobbyState(), "WAITING")) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cThe game already started!");
                    return true;
                }
                if (lobby == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cLobby not found!");
                    return true;
                }

                if (lobby.isFull()) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cLobby is already full!");
                    return true;
                }

                if (!lobby.addPlayer(player)) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cCould not join the lobby.");
                    return true;
                }
                for (Player gamer : lobby.getPlayers()) {
                    gamer.sendMessage("§8[§6MiniGameCore§8]§a " + player.getName() + " joined! " + lobby.getPlayers().size() + "/" + lobby.getMaxPlayers() + " players.");
                }
                World world = Bukkit.getWorld(lobby.getWorldFolder().getName());
                if (world == null) {
                    getLogger().warning("World was null! Teleporting to Owner instead. Lobby: " + lobby.getLobbyId() + ", State: " + lobby.getLobbyState());
                    player.teleport(lobby.getOwner().getLocation());
                } else {
                    Location spawnLocation = world.getSpawnLocation();
                    player.teleport(spawnLocation);
                }
                PlayerHandler.PlayerSoftReset(player);
                player.setGameMode(GameMode.SURVIVAL);
                ScoreBoardManager.setPlayerStatus(player, "WAITING");
                player.sendMessage("§8[§6MiniGameCore§8]§a If you are ready use /mg ready to ready-up!");
                break;

            case "confirm":
                if (!player.hasPermission("mgcore.confirm")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                if (args.length >= 2) {
                    player.sendMessage("§cToo many Args! Usage: /mg confirm");
                    return true;
                }
                if (!confirmations.containsKey(player)) {
                    player.sendMessage("§cYou have nothing to confirm!");
                    return true;
                }

                Lobby confirmLobby = confirmations.remove(player);

                if (confirmLobby == null || !"WAITING".equals(confirmLobby.getLobbyState())) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cThe Lobby already started or is no longer valid!");
                    return true;
                }
                if (player != confirmLobby.getOwner()) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are not the owner of this lobby! How did you manage to do this?");
                    return true;
                }

                for (Player p : confirmLobby.getPlayers()) {
                    p.sendMessage("§8[§6MiniGameCore§8]" + confirmLobby.getOwner().getName() + " force-started the Game!");
                }
                player.sendMessage("§8[§6MiniGameCore§8] §aStarting game: " + confirmLobby.getLobbyId());
                GameManager.startGame(confirmLobby);

            case "ready":
                if (!player.hasPermission("mgcore.ready")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                if (args.length >= 2) {
                player.sendMessage("§cToo many Args! Usage: /mg ready");
                    return true;
                }

                if (LobbyManager.getLobbyByPlayer(player) == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are in no lobby!");
                    return true;
                }

                lobby = LobbyManager.getLobbyByPlayer(player);

                if (!Objects.equals(lobby.getLobbyState(), "WAITING")) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cThe game already started!");
                    return true;
                }

                if (!lobby.ready(player)) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cCould not ready!");
                    return true;
                } else {
                    final TextComponent component = ChatComponentHandler.CreateBaseComponent();
                    ChatComponentHandler.AddColorText(component, "You are now ready!", NamedTextColor.GREEN);
                    player.sendMessage(component);
                }
                break;

            case "unready":
                if (!player.hasPermission("mgcore.unready")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                if (args.length >= 2) {
                    player.sendMessage("§cToo many Args! Usage: /mg unready");
                    return true;
                }

                if (LobbyManager.getLobbyByPlayer(player) == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are in no lobby!");
                    return true;
                }

                lobby = LobbyManager.getLobbyByPlayer(player);

                if (!Objects.equals(lobby.getLobbyState(), "WAITING")) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cThe game already started!");
                    return true;
                }

                if (!lobby.unready(player)) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cCould not unready!");
                    return true;
                } else {
                    final TextComponent component = ChatComponentHandler.CreateBaseComponent();
                    ChatComponentHandler.AddColorText(component, "You are no longer ready!", NamedTextColor.RED);
                    player.sendMessage(component);
                }
                break;

            case "leave":
                if (args.length >= 2) {
                    player.sendMessage("§cToo many Args! Usage: /mg leave");
                    return true;
                }

                lobby = LobbyManager.getLobbyByPlayer(player);

                if (lobby == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are not in any lobby!");
                    return true;
                }

                if (lobby.removePlayer(player)) {
                    PlayerHandler.PlayerReset(player);
                    for (Player gamer : lobby.getPlayers()) {
                        gamer.sendMessage("§8[§6MiniGameCore§8]§a " + player.getName() + " left the Lobby! " + lobby.getPlayers().size() + "/" + lobby.getMaxPlayers() + " players.");
                    }

                    if (lobby.isOwner(player) || lobby.getPlayers().isEmpty()) {
                        player.sendMessage("§8[§6MiniGameCore§8] §cYou were the owner of this lobby. The game will now be stopped.");
                        for (Player gamer : lobby.getPlayers()) {
                            gamer.sendMessage("§8[§6MiniGameCore§8]§c Lobby Owner " + player.getName() + " left the Lobby! Resetting...");
                            PlayerHandler.PlayerReset(gamer);
                        }
                        LobbyHandler.LobbyReset(lobby);
                    }
                    ScoreBoardManager.setPlayerStatus(player, "NONE");
                } else {
                    player.sendMessage("§8[§6MiniGameCore§8] §cFailed to leave the game. Please try again.");
                }
                break;


            case "start":
                if (!player.hasPermission("mgcore.start")) {
                    player.sendMessage("§cYou have no permissions to use this command!");
                    return true;
                }

                lobby = LobbyManager.getLobbyByPlayer(player);
                if (lobby == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are not in a lobby!");
                    return true;
                }

                if (!lobby.isOwner(player)) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cOnly the lobby owner can start the game!");
                    return true;
                }
                if (!(lobby.getReadyPlayers().size() == lobby.getPlayers().size())) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cNot everyone is ready! To continue, run /mg confirm.");
                    confirmations.put(player, lobby);
                    return true;
                }
                GameManager.startGame(lobby);
                player.sendMessage("§8[§6MiniGameCore§8] §aStarting game: " + lobby.getLobbyId());
                break;

            case "spectate":
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg spectate <game|player>");
                    return true;
                }
                if (!player.hasPermission("mgcore.spectate")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                if (LobbyManager.getLobbyByPlayer(player) != null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are already in a game! Type /mg leave to leave!");
                    return true;
                }

                String target = args[1];

                Player targetPlayer = Bukkit.getPlayer(target);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    player.sendMessage("§8[§6MiniGameCore§8] §aYou are now spectating " + targetPlayer.getName() + ".");
                    player.teleport(targetPlayer);
                    player.setGameMode(GameMode.SPECTATOR);
                } else {
                    lobby = LobbyManager.getInstance().getLobby(target);
                    if (lobby != null) {
                        player.sendMessage("§8[§6MiniGameCore§8] §aYou are now spectating the lobby of " + lobby.getOwner().getName() + ".");
                        player.teleport(lobby.getOwner());
                        player.setGameMode(GameMode.SPECTATOR);
                    } else {
                        player.sendMessage("§8[§6MiniGameCore§8]§cNo player or lobby found with that name.");
                    }
                }
                break;

            case "stats":
                if (args.length == 1) {
                    player.sendMessage("§cUsage: /mg stats <Player>");
                    return true;
                }

                OfflinePlayer targetplayer = Bukkit.getOfflinePlayer(args[1]);

                player.sendMessage("§8[§6MiniGameCore§8] §6Stats for " + targetplayer.getName() + ":");

                for (String game : plugin.getAvailableGames()) {
                    int played = Stats.getPlayed(game, targetplayer);
                    int wins = Stats.getWins(game, targetplayer);
                    int losses = Stats.getLosses(game, targetplayer);
                    float winrate = 0;

                    if (played > 0 || wins > 0 || losses > 0) {
                        if (wins > 0) {
                            winrate = ((float) wins / played) * 100;
                            winrate = Math.round(winrate * 10) / 10.0f;
                        }
                        player.sendMessage("§7- §a" + game + "§7: §f" + played + " §agames played, §6" + wins + " §agames won, §c" + losses + " §alost. Win rate: §3" + winrate + "§a%");
                    }
                }
                break;

            case "reload":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                plugin.reloadConfig();
                Stats.setup();
                player.sendMessage("§8[§6MiniGameCore§8] §aPlugin reloaded!");
                break;

            case "stopall":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage("§cYou have no permissions to use this Command!");
                    return true;
                }
                if (lobbyManager.getOpenLobbies() == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cNo active Lobbies.");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cStopping all games!");
                for (Lobby lobby1 : LobbyManager.getInstance().getOpenLobbies()) {
                    for (Player gamer : lobby1.getPlayers()) {
                        gamer.sendMessage("§8[§6MiniGameCore§8]§c Administrator stopped the game! Resetting...");
                        PlayerHandler.PlayerReset(gamer);
                        LobbyHandler.LobbyReset(lobby1);
                    }
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cStopped all games.");
                break;

            case "stop":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage("§cNo permission!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg stop <game>");
                    return true;
                }
                if (lobbyManager.getLobby(args[1]) == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cNo active Lobbies.");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cStopping game: " + args[1]);
                lobby = lobbyManager.getLobby(args[1]);
                for (Player gamer : lobby.getPlayers()) {
                    gamer.sendMessage("§8[§6MiniGameCore§8]§c Administrator stopped the game! Resetting...");
                    PlayerHandler.PlayerReset(gamer);
                    LobbyHandler.LobbyReset(lobby);
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cStopped game: " + args[1]);
                break;

            case "ban":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage("§cYou don't have permissions to use this Command!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg ban <player>");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cBanning player: " + args[1]);
                plugin.banPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
                if (args.length == 2) {
                    getLogger().info(player.getName() + " banned Player: " + args[1] + ".");
                } else {
                    String[] tempReason = Arrays.copyOfRange(args, 2, args.length);
                    String reason = String.join(" ", tempReason);
                    getLogger().info(player.getName() + " banned Player: " + args[1] + "with reason: " + reason);
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cBanned player: " + args[1]);
                break;

            case "unban":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage("§cYou don't have permissions to use this Command!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg unban <player>");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cUnbanning player: " + args[1]);
                plugin.unbanPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
                getLogger().info(player.getName() + " unbanned Player: " + args[1] + ".");
                player.sendMessage("§8[§6MiniGameCore§8] §cUnbanned player: " + args[1]);
                break;

            default:
                player.sendMessage("§8[§6MiniGameCore§8] §cUnknown subcommand!");
                break;
        }

        return true;
    }
}