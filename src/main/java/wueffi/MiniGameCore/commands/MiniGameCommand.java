package wueffi.MiniGameCore.commands;

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
            sender.sendMessage(new ComponentFactory("Yo console User, only players can use this command!").toComponent());
            return true;
        }
        HashMap<String, String> commands_permissions = getCommandsPermissions();

        if (args.length < 1) {
            ComponentFactory availableCommands = new ComponentFactory("Usage: ");
            availableCommands.addColorText("/mg <", NamedTextColor.GOLD);

            for (String command : commands_permissions.keySet()) {
                if (player.hasPermission(commands_permissions.get(command))) {
                    availableCommands.addText(command).addText(" | ");
                }
            }
            availableCommands.addText(">");
            player.sendMessage(availableCommands.toComponent());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "host":
                if (plugin.getBannedPlayers().contains(player.getUniqueId())) {
                    player.sendMessage(new ComponentFactory("You were banned by an Administrator.", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(new ComponentFactory("Missing Args! Usage: /mg host <game>", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (!player.hasPermission("mgcore.host")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this Command!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (LobbyManager.getLobbyByPlayer(player) != null) {
                    player.sendMessage(new ComponentFactory("You are already in another lobby!", NamedTextColor.RED).toComponent());
                    return true;
                }
                String gameName = args[1];
                if (!plugin.getAvailableGames().contains(gameName)) {
                    player.sendMessage(new ComponentFactory("Game " + gameName + " not available!", NamedTextColor.RED).toComponent());
                    return true;
                }
                GameManager gameManager = new GameManager(plugin);
                gameManager.hostGame(gameName, sender);
                player.sendMessage(new ComponentFactory("Hosting game: " + args[1], NamedTextColor.GREEN).toComponent());
                ScoreBoardManager.setPlayerStatus(player, "WAITING");
                Lobby lobby = LobbyManager.getLobbyByPlayer(player);
                lobby.setLobbyState("WAITING");
                player.sendMessage(new ComponentFactory("If you are ready use /mg ready to ready-up!", NamedTextColor.GREEN).toComponent());
                break;

            case "join":
                if (plugin.getBannedPlayers().contains(player.getUniqueId())) {
                    player.sendMessage(new ComponentFactory("You were banned by an Administrator.", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(new ComponentFactory("Missing Args! Usage: /mg join <game>", NamedTextColor.RED).toComponent());
                    return true;
                }

                String lobbyName = args[1];
                lobby = lobbyManager.getLobby(lobbyName);

                if (LobbyManager.getLobbyByPlayer(player) != null) {
                    player.sendMessage(new ComponentFactory("You are already in another lobby!", NamedTextColor.RED).toComponent());
                    return true;
                }

                if (!Objects.equals(lobby.getLobbyState(), "WAITING")) {
                    player.sendMessage(new ComponentFactory("The game already started!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (lobby == null) {
                    player.sendMessage(new ComponentFactory("Lobby not found!", NamedTextColor.RED).toComponent());
                    return true;
                }

                if (lobby.isFull()) {
                    player.sendMessage(new ComponentFactory("Lobby is already full!", NamedTextColor.RED).toComponent());
                    return true;
                }

                if (!lobby.addPlayer(player)) {
                    player.sendMessage(new ComponentFactory("Could not join the lobby.", NamedTextColor.RED).toComponent());
                    return true;
                }
                for (Player gamer : lobby.getPlayers()) {
                    gamer.sendMessage(new ComponentFactory(player.getName() + " joined! " + lobby.getPlayers().size() + "/" + lobby.getMaxPlayers() + " players.", NamedTextColor.GREEN).toComponent());
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
                player.sendMessage(new ComponentFactory("If you are ready use /mg ready to ready-up!", NamedTextColor.GREEN).toComponent());
                break;

            case "confirm":
                if (!player.hasPermission("mgcore.confirm")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this Command!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (args.length >= 2) {
                    player.sendMessage(new ComponentFactory("Too many Args! Usage: /mg confirm", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (!confirmations.containsKey(player)) {
                    player.sendMessage(new ComponentFactory("You have nothing to confirm!", NamedTextColor.RED).toComponent());
                    return true;
                }

                Lobby confirmLobby = confirmations.remove(player);

                if (confirmLobby == null || !"WAITING".equals(confirmLobby.getLobbyState())) {
                    player.sendMessage(new ComponentFactory("The Lobby already started or is no longer valid!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (player != confirmLobby.getOwner()) {
                    player.sendMessage(new ComponentFactory("You are not the owner of this lobby! How did you manage to do this?", NamedTextColor.RED).toComponent());
                    return true;
                }

                for (Player p : confirmLobby.getPlayers()) {
                    p.sendMessage(new ComponentFactory(confirmLobby.getOwner().getName() + " force-started the Game!"
                            , NamedTextColor.GOLD).toComponent());
                }
                player.sendMessage(new ComponentFactory("Starting game: " + confirmLobby.getLobbyId(),
                        NamedTextColor.GREEN).toComponent());
                GameManager.startGame(confirmLobby);

            case "ready":
                if (!player.hasPermission("mgcore.ready")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this Command!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (args.length >= 2) {
                    player.sendMessage(new ComponentFactory("Too many Args! Usage: /mg ready", NamedTextColor.RED).toComponent());
                    return true;
                }

                if (LobbyManager.getLobbyByPlayer(player) == null) {
                    player.sendMessage(new ComponentFactory("You are not in a lobby!", NamedTextColor.RED).toComponent());
                    return true;
                }

                lobby = LobbyManager.getLobbyByPlayer(player);

                if (!Objects.equals(lobby.getLobbyState(), "WAITING")) {
                    player.sendMessage(new ComponentFactory("The game already started!", NamedTextColor.RED).toComponent());
                    return true;
                }

                if (!lobby.ready(player)) {
                    player.sendMessage(new ComponentFactory("Could not ready!", NamedTextColor.RED).toComponent());
                    return true;
                } else {
                    player.sendMessage(new ComponentFactory("You are now ready!", NamedTextColor.GREEN).toComponent());
                    return true;
                }

            case "unready":
                if (!player.hasPermission("mgcore.unready")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this Command!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (args.length >= 2) {
                    player.sendMessage(new ComponentFactory("Too many Args! Usage: /mg unready", NamedTextColor.RED).toComponent());
                    return true;
                }

                if (LobbyManager.getLobbyByPlayer(player) == null) {
                    player.sendMessage(new ComponentFactory("You are in no lobby!", NamedTextColor.RED).toComponent());
                    return true;
                }

                lobby = LobbyManager.getLobbyByPlayer(player);

                if (!Objects.equals(lobby.getLobbyState(), "WAITING")) {
                    player.sendMessage(new ComponentFactory("The game already started!", NamedTextColor.RED).toComponent());
                    return true;
                }

                if (!lobby.unready(player)) {
                    player.sendMessage(new ComponentFactory("Could not unready!", NamedTextColor.RED).toComponent());
                    return true;
                } else {
                    player.sendMessage(new ComponentFactory("You are no longer ready!", NamedTextColor.RED).toComponent());
                    return true;
                }

            case "leave":
                if (args.length >= 2) {
                    player.sendMessage(new ComponentFactory("Too many Args! Usage: /mg leave", NamedTextColor.RED).toComponent());
                    return true;
                }

                lobby = LobbyManager.getLobbyByPlayer(player);

                if (lobby == null) {
                    player.sendMessage(new ComponentFactory("You are not in any lobby!", NamedTextColor.RED).toComponent());
                    return true;
                }

                if (lobby.removePlayer(player)) {
                    PlayerHandler.PlayerReset(player);
                    for (Player gamer : lobby.getPlayers()) {
                        gamer.sendMessage(new ComponentFactory(player.getName() + " left the Lobby! " + lobby.getPlayers().size() + "/" + lobby.getMaxPlayers() + " players.", NamedTextColor.GREEN).toComponent());
                    }

                    if (lobby.isOwner(player) || lobby.getPlayers().isEmpty()) {
                        player.sendMessage(new ComponentFactory("You were the owner of this lobby. The game will now " +
                                "be stopped.", NamedTextColor.RED).toComponent());
                        for (Player gamer : lobby.getPlayers()) {
                            gamer.sendMessage(new ComponentFactory("Lobby Owner " + player.getName() + " left the " +
                                    "Lobby! Resetting...", NamedTextColor.RED).toComponent());
                            PlayerHandler.PlayerReset(gamer);
                        }
                        LobbyHandler.LobbyReset(lobby);
                    }
                    ScoreBoardManager.setPlayerStatus(player, "NONE");
                } else {
                    player.sendMessage(new ComponentFactory("Failed to leave the game. Please try again.", NamedTextColor.RED).toComponent());
                }
                break;


            case "start":
                if (!player.hasPermission("mgcore.start")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this command!", NamedTextColor.RED).toComponent());
                    return true;
                }

                lobby = LobbyManager.getLobbyByPlayer(player);
                if (lobby == null) {
                    player.sendMessage(new ComponentFactory("You are not in a lobby!", NamedTextColor.RED).toComponent());
                    return true;
                }

                if (!lobby.isOwner(player)) {
                    player.sendMessage(new ComponentFactory("Only the lobby owner can start the game!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (!(lobby.getReadyPlayers().size() == lobby.getPlayers().size())) {
                    player.sendMessage(new ComponentFactory("Not everyone is ready! To continue, run /mg confirm.", NamedTextColor.RED).toComponent());
                    confirmations.put(player, lobby);
                    return true;
                }
                GameManager.startGame(lobby);
                player.sendMessage(new ComponentFactory("Starting game: " + lobby.getLobbyId(), NamedTextColor.GREEN).toComponent());
                break;

            case "spectate":
                if (args.length < 2) {
                    player.sendMessage(new ComponentFactory("Missing Args! Usage: /mg spectate <game|player>", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (!player.hasPermission("mgcore.spectate")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this Command!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (LobbyManager.getLobbyByPlayer(player) != null) {
                    player.sendMessage(new ComponentFactory("You are already in a game! Type /mg leave to leave!", NamedTextColor.RED).toComponent());
                    return true;
                }

                String target = args[1];

                Player targetPlayer = Bukkit.getPlayer(target);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    player.sendMessage(new ComponentFactory("You are now spectating " + targetPlayer.getName() + ".",
                            NamedTextColor.GREEN).toComponent());
                    player.teleport(targetPlayer);
                    player.setGameMode(GameMode.SPECTATOR);
                } else {
                    lobby = LobbyManager.getInstance().getLobby(target);
                    if (lobby != null) {
                        player.sendMessage(new ComponentFactory("You are now spectating the lobby of " + lobby.getOwner().getName() + ".", NamedTextColor.GREEN).toComponent());
                        player.teleport(lobby.getOwner());
                        player.setGameMode(GameMode.SPECTATOR);
                    } else {
                        player.sendMessage(new ComponentFactory("No player or lobby found with that name.", NamedTextColor.RED).toComponent());
                    }
                }
                break;

            case "stats":
                if (args.length == 1) {
                    player.sendMessage(new ComponentFactory("Usage: /mg stats <Player>", NamedTextColor.RED).toComponent());
                    return true;
                }

                OfflinePlayer targetplayer = Bukkit.getOfflinePlayer(args[1]);

                player.sendMessage(new ComponentFactory("Stats for " + targetplayer.getName() + ":", NamedTextColor.GOLD).toComponent());

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
                        player.sendMessage(new ComponentFactory("- ", NamedTextColor.GRAY)
                                .addColorText(game.toString(), NamedTextColor.GREEN)
                                .addColorText(": ", NamedTextColor.GRAY)
                                .addColorText(Integer.toString(played), NamedTextColor.WHITE)
                                .addColorText(" games played, ", NamedTextColor.GREEN)
                                .addColorText(Integer.toString(wins), NamedTextColor.GOLD)
                                .addColorText(" games won, ", NamedTextColor.GREEN)
                                .addColorText(Integer.toString(losses), NamedTextColor.RED)
                                .addColorText(" lost. Win rate: ", NamedTextColor.GREEN)
                                .addColorText(Float.toString(winrate), NamedTextColor.DARK_AQUA)
                                .addColorText("%", NamedTextColor.GREEN)
                                .toComponent());
                    }
                }
                break;

            case "reload":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this Command!", NamedTextColor.RED).toComponent());
                    return true;
                }
                plugin.reloadConfig();
                Stats.setup();
                player.sendMessage(new ComponentFactory("Plugin reloaded!", NamedTextColor.GREEN).toComponent());
                break;

            case "stopall":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage(new ComponentFactory("", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (lobbyManager.getOpenLobbies() == null) {
                    player.sendMessage(new ComponentFactory("No active Lobbies.", NamedTextColor.RED).toComponent());
                    return true;
                }
                player.sendMessage(new ComponentFactory("Stopping all games!", NamedTextColor.RED).toComponent());
                for (Lobby lobby1 : LobbyManager.getInstance().getOpenLobbies()) {
                    for (Player gamer : lobby1.getPlayers()) {
                        gamer.sendMessage(new ComponentFactory("Administrator stopped the game! Resetting...", NamedTextColor.RED).toComponent());
                        PlayerHandler.PlayerReset(gamer);
                        LobbyHandler.LobbyReset(lobby1);
                    }
                }
                player.sendMessage(new ComponentFactory("Stopped all games.", NamedTextColor.RED).toComponent());
                break;

            case "stop":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this Command!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(new ComponentFactory("Missing Args! Usage: /mg stop <game>", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (lobbyManager.getLobby(args[1]) == null) {
                    player.sendMessage(new ComponentFactory("No active Lobbies.", NamedTextColor.RED).toComponent());
                    return true;
                }
                player.sendMessage(new ComponentFactory("Stopping game: " + args[1], NamedTextColor.RED).toComponent());
                lobby = lobbyManager.getLobby(args[1]);
                for (Player gamer : lobby.getPlayers()) {
                    gamer.sendMessage(new ComponentFactory("Administrator stopped the game! Resetting...", NamedTextColor.RED).toComponent());
                    PlayerHandler.PlayerReset(gamer);
                    LobbyHandler.LobbyReset(lobby);
                }
                player.sendMessage(new ComponentFactory("Stopped game: " + args[1], NamedTextColor.RED).toComponent());
                break;

            case "ban":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this Command!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(new ComponentFactory("Missing Args! Usage: /mg ban <player>", NamedTextColor.RED).toComponent());
                    return true;
                }
                player.sendMessage(new ComponentFactory("Banning player: " + args[1], NamedTextColor.RED).toComponent());
                plugin.banPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
                if (args.length == 2) {
                    getLogger().info(player.getName() + " banned Player: " + args[1] + ".");
                } else {
                    String[] tempReason = Arrays.copyOfRange(args, 2, args.length);
                    String reason = String.join(" ", tempReason);
                    getLogger().info(player.getName() + " banned Player: " + args[1] + "with reason: " + reason);
                }
                player.sendMessage(new ComponentFactory("Banned player: " + args[1], NamedTextColor.RED).toComponent());
                break;

            case "unban":
                if (!player.hasPermission("mgcore.admin")) {
                    player.sendMessage(new ComponentFactory("You have no permissions to use this Command!", NamedTextColor.RED).toComponent());
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(new ComponentFactory("Missing Args! Usage: /mg unban <player>", NamedTextColor.RED).toComponent());
                    return true;
                }
                player.sendMessage(new ComponentFactory("Unbanning player: " + args[1], NamedTextColor.RED).toComponent());
                plugin.unbanPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
                getLogger().info(player.getName() + " unbanned Player: " + args[1] + ".");
                player.sendMessage(new ComponentFactory("Unbanned player: " + args[1], NamedTextColor.RED).toComponent());
                break;

            default:
                player.sendMessage(new ComponentFactory("Unknown subcommand!", NamedTextColor.RED).toComponent());
                break;
        }

        return true;
    }
}