package wueffi.MiniGameCore.commands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.managers.GameManager;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.managers.PartyManager;
import wueffi.MiniGameCore.managers.ScoreBoardManager;
import wueffi.MiniGameCore.utils.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MiniGameCommand implements CommandExecutor {
    private final MiniGameCore plugin;
    private static final Map<Player, Lobby> confirmations = new HashMap<>();
    private static final HashMap<String, String> commandsPermissions = new HashMap<>();

    public MiniGameCommand(MiniGameCore plugin) {
        this.plugin = plugin;
    }

    public static @NotNull HashMap<String, String> getCommandsPermissions() {
        if (commandsPermissions.isEmpty()) {
            commandsPermissions.put("host", "mgcore.host");
            commandsPermissions.put("join", "mgcore.join");
            commandsPermissions.put("confirm", "mgcore.confirm");
            commandsPermissions.put("ready", "mgcore.ready");
            commandsPermissions.put("unready", "mgcore.unready");
            commandsPermissions.put("leave", "mgcore.leave");
            commandsPermissions.put("start", "mgcore.start");
            commandsPermissions.put("spectate", "mgcore.spectate");
            commandsPermissions.put("unspectate", "mgcore.unspectate");
            commandsPermissions.put("reload", "mgcore.admin");
            commandsPermissions.put("stopall", "mgcore.admin");
            commandsPermissions.put("stop", "mgcore.admin");
            commandsPermissions.put("ban", "mgcore.admin");
            commandsPermissions.put("unban", "mgcore.admin");
            commandsPermissions.put("version", "mgcore.use");
        }
        return commandsPermissions;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        Party party;
        GameConfig config;
        Lobby lobby;

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Yo console User, only players can use this command!");
            return true;
        }
        HashMap<String, String> commandsPermissions = getCommandsPermissions();

        if (args.length < 1) {
            StringBuilder availableCommands = new StringBuilder("§fUsage: §6/mg <");

            for (String command: commandsPermissions.keySet()) {
                if (player.hasPermission(commandsPermissions.get(command))) {
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

        String subcmd = args[0].toLowerCase();

        if (!commandsPermissions.containsKey(subcmd)) {
            player.sendMessage("§8[§6MiniGameCore§8] §cUnknown subcommand!");
            return true;
        }

        if (!(player.hasPermission(commandsPermissions.get(subcmd)) || player.hasPermission("mgcore.admin"))) {
            player.sendMessage("§8[§6MiniGameCore§8] §cYou don't have permission to use that subcommand!!");
            return true;
        }

        switch (subcmd) {
            case "host":
                if (plugin.getBannedPlayers().contains(player.getUniqueId())) {
                    player.sendMessage("§cYou were banned by an Administrator.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg host <game>");
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
                party = PartyManager.getPartyByPlayer(player);
                if (party != null) {
                    if (party.isOwner(player)) {
                        config = new GameConfig(new File("Minigames/" + gameName, "config.yml"));
                        if (!player.hasPermission(config.getHostPerm())) {
                            player.sendMessage("§8[§6MiniGameCore§8] §cYou don't have permission to host " + gameName + "!");
                            return true;
                        }
                        if (party.getPlayers().size() > config.getMaxPlayers()) {
                            player.sendMessage("§8[§6MiniGameCore§8]§c Party too big for game!");
                            return true;
                        }
                        lobby = GameManager.hostGame(gameName, sender);
                        if (lobby == null) {
                            return true; // the game manager already sent the message for us
                        }
                        World world = Bukkit.getWorld(lobby.getWorldFolder().getName());
                        for (Player gamer : lobby.getPlayers()) {
                            gamer.sendMessage("§8[§6MiniGameCore§8]§a " + player.getName() + " joined with a party of " + party.getPlayers().size() + "! " + (lobby.getPlayers().size() + party.getPlayers().size() - 1) + "/" + lobby.getMaxPlayers() + " players.");
                        }
                        for (Player player1 : party.getPlayers()) {
                            if (!party.isOwner(player1) && LobbyManager.getLobbyByPlayer(player1) != null) {
                                Lobby lobby2 = LobbyManager.getLobbyByPlayer(player1);
                                lobby2.removePlayer(player);
                                for (Player player2 : lobby2.getPlayers()) {
                                    player2.sendMessage("§8[§6MiniGameCore§8]§a " + player.getName() + " left! " + (lobby2.getPlayers().size()) + "/" + lobby2.getMaxPlayers() + " players.");
                                }
                            }
                            lobby.addPlayer(player1);
                            if (world == null) {
                                plugin.getLogger().warning("World was null! Teleporting to Owner instead. Lobby: " + lobby.getLobbyId() + ", State: " + lobby.getLobbyState());
                                player1.teleport(lobby.getOwner().getLocation());
                            } else {
                                Location spawnLocation = world.getSpawnLocation();
                                player1.teleport(spawnLocation);
                            }
                            PlayerHandler.PlayerSoftReset(player1);
                            player1.setGameMode(GameMode.SURVIVAL);
                            ScoreBoardManager.setPlayerStatus(player, "WAITING");
                            player1.sendTitle("", "If you are ready use §a/mg ready §fto ready-up!", 0, 40, 5);
                        }
                    } else {
                        player.sendMessage("§8[§6MiniGameCore§8]§c You are in a party!");
                        return true;
                    }
                } else {
                    lobby = GameManager.hostGame(gameName, sender); // again, it handled the message for us
                }
                player.sendMessage("§8[§6MiniGameCore§8]§a Hosting game: " + args[1]);
                ScoreBoardManager.setPlayerStatus(player, "WAITING");
                lobby.setLobbyState("WAITING");
                player.sendTitle("", "If you are ready use §a/mg ready §fto ready-up!", 0, 40, 5);
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
                lobby = LobbyManager.getLobby(lobbyName);

                if (lobby == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cLobby not found!");
                    return true;
                }

                if (LobbyManager.getLobbyByPlayer(player) != null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are already in another lobby!");
                    return true;
                }

                if (!Objects.equals(lobby.getLobbyState(), "WAITING")) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cThe game already started!");
                    return true;
                }

                if (lobby.isFull()) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cLobby is already full!");
                    return true;
                }

                party = PartyManager.getPartyByPlayer(player);
                if (party != null) {
                    if (party.isOwner(player)) {
                        if (party.getPlayers().size() + lobby.getMaxPlayers() > lobby.getMaxPlayers()) {
                            player.sendMessage("§8[§6MiniGameCore§8]§c Party too big for game!");
                            return true;
                        }
                        World world = Bukkit.getWorld(lobby.getWorldFolder().getName());
                        for (Player gamer : lobby.getPlayers()) {
                            gamer.sendMessage("§8[§6MiniGameCore§8]§a " + player.getName() + " joined with a party of " + party.getPlayers().size() + "!" + (lobby.getPlayers().size() + party.getPlayers().size()) + "/" + lobby.getMaxPlayers() + " players.");
                        }
                        for (Player player1 : party.getPlayers()) {
                            if (LobbyManager.getLobbyByPlayer(player1) != null) {
                                Lobby lobby1 = LobbyManager.getLobbyByPlayer(player1);
                                lobby1.removePlayer(player);
                                for (Player player2 : lobby1.getPlayers()) {
                                    player2.sendMessage("§8[§6MiniGameCore§8]§a " + player.getName() + " left! " + (lobby1.getPlayers().size()) + "/" + lobby1.getMaxPlayers() + " players.");
                                }
                            }
                            lobby.addPlayer(player1);
                            if (world == null) {
                                plugin.getLogger().warning("World was null! Teleporting to Owner instead. Lobby: " + lobby.getLobbyId() + ", State: " + lobby.getLobbyState());
                                player1.teleport(lobby.getOwner().getLocation());
                            } else {
                                Location spawnLocation = world.getSpawnLocation();
                                player1.teleport(spawnLocation);
                            }
                            PlayerHandler.PlayerSoftReset(player1);
                            player1.setGameMode(GameMode.SURVIVAL);
                            ScoreBoardManager.setPlayerStatus(player, "WAITING");
                            player1.sendTitle("", "If you are ready use §a/mg ready §fto ready-up!", 0, 40, 5);
                            return true;
                        }
                    } else {
                        player.sendMessage("§8[§6MiniGameCore§8]§c You are in a party!");
                        return true;
                    }
                }

                if (!lobby.addPlayer(player)) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cCould not join the lobby.");
                    return true;
                }
                for (Player gamer : lobby.getPlayers()) {
                    gamer.sendMessage("§8[§6MiniGameCore§8]§a " + player.getName() + " joined! " + lobby.getPlayers().size() + "/" + lobby.getMaxPlayers() + " players.");
                }
                World world1 = Bukkit.getWorld(lobby.getWorldFolder().getName());
                if (world1 == null) {
                    plugin.getLogger().warning("World was null! Teleporting to Owner instead. Lobby: " + lobby.getLobbyId() + ", State: " + lobby.getLobbyState());
                    player.teleport(lobby.getOwner().getLocation());
                } else {
                    Location spawnLocation = world1.getSpawnLocation();
                    player.teleport(spawnLocation);
                }
                PlayerHandler.PlayerSoftReset(player);
                player.setGameMode(GameMode.SURVIVAL);
                ScoreBoardManager.setPlayerStatus(player, "WAITING");
                player.sendTitle("", "If you are ready use §a/mg ready §fto ready-up!", 0, 40, 5);
                break;

            case "confirm":
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
                    p.sendMessage("§8[§6MiniGameCore§8] §7" + confirmLobby.getOwner().getName() + " §8force-started the Game!");
                }
                player.sendMessage("§8[§6MiniGameCore§8] §aStarting game: " + confirmLobby.getLobbyId());
                GameManager.startGame(confirmLobby);

            case "ready":
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

                if (!lobby.ready(player)) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cCould not ready!");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8] §aYou are now ready!");
                break;

            case "unready":
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
                }
                player.sendMessage("§8[§6MiniGameCore§8] §aYou not ready anymore!");
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
                if (lobby.getPlayers().size() > 1) player.sendMessage("§8[§6MiniGameCore§8] §aStarting game: " + lobby.getLobbyId());
                break;

            case "spectate":
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg spectate <game|player>");
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
                    lobby = LobbyManager.getLobby(target);
                    if (lobby != null) {
                        player.sendMessage("§8[§6MiniGameCore§8] §aYou are now spectating the lobby of " + lobby.getOwner().getName() + ".");
                        player.teleport(lobby.getOwner());
                        player.setGameMode(GameMode.SPECTATOR);
                    } else {
                        player.sendMessage("§8[§6MiniGameCore§8]§cNo player or lobby found with that name.");
                    }
                }
                break;

            case "unspectate":
                if (args.length > 1) {
                    player.sendMessage("§cToo many Args! Usage: /mg unspectate");
                    return true;
                }
                if (LobbyManager.getLobbyByPlayer(player) != null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cYou are already in a game! Type /mg leave to leave!");
                    return true;
                }

                player.sendMessage("§8[§6MiniGameCore§8] §aYou are not spectating the game anymore.");
                World world = Bukkit.getWorld("world");
                assert world != null;
                player.teleport(world.getSpawnLocation());
                player.setGameMode(Bukkit.getDefaultGameMode());
                break;

            case "stats":
                if (args.length == 1) {
                    player.sendMessage("§cUsage: /mg stats <Player>");
                    return true;
                }

                OfflinePlayer targetplayer = Bukkit.getOfflinePlayer(args[1]);

                if (!Stats.getStats(targetplayer.getUniqueId())) {
                    player.sendMessage("§cNo Stats for " + targetplayer.getName() + "!");
                    return true;
                }

                player.sendMessage("§8[§6MiniGameCore§8] §6Stats for " + targetplayer.getName() + ":");

                for (String game : plugin.getAvailableGames()) {
                    int played = Stats.getPlayed(game, targetplayer);
                    int wins = Stats.getWins(game, targetplayer);
                    int losses = Stats.getLosses(game, targetplayer);
                    int ties = Stats.getTies(game, targetplayer);
                    float winrate = 0;

                    if (played > 0 || wins > 0 || losses > 0) {
                        if (wins > 0) {
                            winrate = ((float) wins / played) * 100;
                            winrate = Math.round(winrate * 10) / 10.0f;
                        }
                        player.sendMessage("§7- §a" + game + "§7: §f" + played + " §agames played, §6" + wins + " §agames won, §3" + ties + " §agames tied, §c" + losses + " §alost. Win rate: §3" + winrate + "§a%");
                    }
                }
                break;

            case "reload":
                plugin.reloadConfig();
                Stats.setup();
                player.sendMessage("§8[§6MiniGameCore§8] §aPlugin reloaded!");
                break;

            case "stopall":
                if (LobbyManager.getOpenLobbies() == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cNo active Lobbies.");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cStopping all games!");
                for (Lobby lobby1 : LobbyManager.getOpenLobbies()) {
                    for (Player gamer : lobby1.getPlayers()) {
                        gamer.sendMessage("§8[§6MiniGameCore§8]§c Administrator stopped the game! Resetting...");
                        PlayerHandler.PlayerReset(gamer);
                        LobbyHandler.LobbyReset(lobby1);
                    }
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cStopped all games.");
                break;

            case "stop":
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg stop <game>");
                    return true;
                }
                if (LobbyManager.getLobby(args[1]) == null) {
                    player.sendMessage("§8[§6MiniGameCore§8] §cNo active Lobbies.");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cStopping game: " + args[1]);
                lobby = LobbyManager.getLobby(args[1]);
                for (Player gamer : lobby.getPlayers()) {
                    gamer.sendMessage("§8[§6MiniGameCore§8]§c Administrator stopped the game! Resetting...");
                    PlayerHandler.PlayerReset(gamer);
                    LobbyHandler.LobbyReset(lobby);
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cStopped game: " + args[1]);
                break;

            case "ban":
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg ban <player>");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cBanning player: " + args[1]);
                plugin.banPlayer(Objects.requireNonNull(Bukkit.getPlayer(args[1])).getUniqueId());
                if (args.length == 2) {
                    plugin.getLogger().info(player.getName() + " banned Player: " + args[1] + ".");
                } else {
                    String[] tempReason = Arrays.copyOfRange(args, 2, args.length);
                    String reason = String.join(" ", tempReason);
                    plugin.getLogger().info(player.getName() + " banned Player: " + args[1] + "with reason: " + reason);
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cBanned player: " + args[1]);
                break;

            case "unban":
                if (args.length < 2) {
                    player.sendMessage("§cMissing Args! Usage: /mg unban <player>");
                    return true;
                }
                player.sendMessage("§8[§6MiniGameCore§8] §cUnbanning player: " + args[1]);
                plugin.unbanPlayer(Objects.requireNonNull(Bukkit.getPlayer(args[1])).getUniqueId());
                plugin.getLogger().info(player.getName() + " unbanned Player: " + args[1] + ".");
                player.sendMessage("§8[§6MiniGameCore§8] §cUnbanned player: " + args[1]);
                break;

            case "version":
                player.sendMessage("§8[§6MiniGameCore§8] §aVersion: " + plugin.getDescription().getVersion());
                break;
        }

        return true;
    }
}