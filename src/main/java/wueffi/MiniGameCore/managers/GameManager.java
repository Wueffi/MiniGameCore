package wueffi.MiniGameCore.managers;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.utils.*;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class GameManager implements Listener {
    static final Map<Lobby, List<Player>> alivePlayers = new HashMap<>();
    private static final Set<Player> frozenPlayers = new HashSet<>();
    static Map<UUID, Location> playerRespawnPoints = new HashMap<>();
    private static MiniGameCore plugin;

    public GameManager(MiniGameCore plugin) {
        GameManager.plugin = plugin;
    }

    public static void startGame(Lobby lobby) {
        for (Player player : lobby.getPlayers()) {
            player.sendMessage("§8[§6MiniGameCore§8]§a " + lobby.getGameName() + " is starting!");
            frozenPlayers.add(player);
        }
        alivePlayers.put(lobby, new ArrayList<>(lobby.getPlayers()));
        startCountdown(lobby);
    }

    public static void winGame(Lobby lobby, Player winner) {
        for (Player player : lobby.getPlayers()) {
            player.sendTitle("§6" + winner.getName(), "won the Game!", 10, 70, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            Stats.win(lobby.getGameName(), winner);
            if (!player.equals(winner)) {
                Stats.lose(lobby.getGameName(), player);
            }
            runDelayed(() -> {
                PlayerHandler.PlayerReset(player);
            }, 4);
        }
        runDelayed(() -> {
            LobbyHandler.LobbyReset(lobby);
        }, 4);
    }

    private static void startCountdown(Lobby lobby) {
        lobby.setLobbyState("COUNTDOWN");
        GameConfig gameConfig = loadGameConfigFromWorld(lobby.getWorldFolder());
        List<Player> players = new ArrayList<>(lobby.getPlayers());
        List<List<Player>> teams = new ArrayList<>();

        Collections.shuffle(players); // Shuffly Shuff

        if (gameConfig.getTeams() > 0) {
            int teamCount = gameConfig.getTeams();

            for (int i = 0; i < teamCount; i++) {
                teams.add(new ArrayList<>());
            }

            for (int i = 0; i < players.size(); i++) {
                teams.get(i % teamCount).add(players.get(i));
            }

            for (int teamIndex = 0; teamIndex < teamCount; teamIndex++) {
                List<Player> teamPlayers = teams.get(teamIndex);
                List<GameConfig.TeamSpawnPoint> teamSpawns = new ArrayList<>(gameConfig.getTeamSpawnPoints().get(teamIndex).getSpawnPoints());
                Collections.shuffle(teamSpawns);

                for (Player teamPlayer : teamPlayers) {
                    if (teamSpawns.isEmpty()) {
                        getLogger().warning("Not enough SpawnPoints for Team " + (teamIndex + 1) + " in Lobby " + lobby.getLobbyId());
                        continue;
                    }

                    GameConfig.TeamSpawnPoint spawn = teamSpawns.remove(0);
                    Location spawnLocation = new Location(teamPlayer.getWorld(), spawn.getX(), spawn.getY(), spawn.getZ());
                    teamPlayer.teleport(spawnLocation);

                    if (gameConfig.getRespawnMode()) {
                        playerRespawnPoints.put(teamPlayer.getUniqueId(), spawnLocation);
                    }
                }
            }
        } else {
            List<GameConfig.SpawnPoint> spawnPoints = new ArrayList<>(gameConfig.getSpawnPoints());
            Collections.shuffle(spawnPoints);

            for (Player player : players) {
                GameConfig.SpawnPoint spawn = spawnPoints.remove(0);
                Location spawnLocation = new Location(player.getWorld(), spawn.getX(), spawn.getY(), spawn.getZ());
                player.teleport(spawnLocation);

                if (gameConfig.getRespawnMode()) {
                    playerRespawnPoints.put(player.getUniqueId(), spawnLocation);
                }
            }
        }

        new BukkitRunnable() {
            int timeLeft = 10;

            @Override
            public void run() {
                if (timeLeft > 0) {
                    for (Player player : lobby.getPlayers()) {
                        player.sendTitle("§aGame starting in " + timeLeft, "", 10, 70, 20);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
                    }
                    timeLeft--;
                } else {
                    lobby.setLobbyState("GAME");
                    for (Player player : lobby.getPlayers()) {
                        player.sendTitle("§aGame Started!", "§cTeaming / Cheating is bannable!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 5.0f);
                        ScoreBoardManager.setPlayerStatus(player, "GAME");
                        player.getInventory().clear();
                        player.getInventory().setArmorContents(null);
                        player.setItemOnCursor(null);
                        for (Material material : gameConfig.getStartInventory()) {
                            player.getInventory().addItem(new ItemStack(material));
                        }
                        frozenPlayers.remove(player);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static GameConfig loadGameConfigFromWorld(File worldFolder) {
        File configFile = new File(worldFolder, "config.yml");

        if (configFile.exists()) {
            return new GameConfig(configFile);
        } else {
            plugin.getLogger().warning("No config.yml found in world folder for " + worldFolder.getName());
            return new GameConfig(configFile);
        }
    }

    private static void runDelayed(Runnable task, int seconds) {
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MiniGameCore"), task, seconds * 20L);
    }

    public void hostGame(String gameName, CommandSender sender) {
        Player player = (Player) sender;

        String originalWorldName = gameName + "_world";
        String newWorldName = gameName + "_copy_" + System.currentTimeMillis();

        File originalWorldFolder = new File("MiniGames", originalWorldName);
        if (!originalWorldFolder.exists()) {
            plugin.getLogger().warning("Template world " + originalWorldName + " not found in" + originalWorldFolder.getAbsolutePath() + ".");
            return;
        }

        File newWorldFolder = new File(Bukkit.getWorldContainer(), newWorldName);

        if (originalWorldFolder.exists()) {
            try {
                copyWorldFolder(originalWorldFolder, newWorldFolder);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to copy world: " + e.getMessage());
                return;
            }
            plugin.getLogger().info("World copied successfully.");
        } else {
            plugin.getLogger().warning("World folder " + originalWorldName + " not found.");
            return;
        }

        World newWorld = Bukkit.createWorld(new WorldCreator(newWorldFolder.getName()));

        if (newWorld == null) {
            plugin.getLogger().warning("Failed to load copied world: " + newWorldName);
            return;
        } else {
            Location spawnLocation = newWorld.getSpawnLocation();
            player.teleport(spawnLocation);
            player.setGameMode(GameMode.SURVIVAL);
        }

        plugin.getLogger().info("Copied and loaded world: " + newWorldName);

        if (LobbyManager.getLobbyByPlayer(player) != null) {
            player.sendMessage("§8[§6MiniGameCore§8]§c You are already in a game or lobby!");
            return;
        }

        GameConfig gameConfig = loadGameConfigFromWorld(newWorldFolder);
        int maxPlayers = gameConfig.getMaxPlayers();

        LobbyManager lobbyManager = LobbyManager.getInstance();
        Lobby lobby = lobbyManager.createLobby(gameName, maxPlayers, player, newWorldFolder);

        if (lobby == null) {
            player.sendMessage("§8[§6MiniGameCore§8]§c Lobby could not be created!");
            return;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage("§8[§6MiniGameCore§8]§a " + player.getName() + " is hosting " + lobby.getGameName() + "! " +
                    lobby.getPlayers().size() + "/" + maxPlayers + " players - type /mg join " + lobby.getLobbyId() + " to join the fun!");
        }

        if (lobby.isFull()) {
            startGame(lobby);
        }
    }

    private void copyWorldFolder(File source, File destination) throws Exception {
        if (!source.exists()) {
            throw new Exception("Source folder does not exist.");
        }

        if (!destination.exists()) {
            destination.mkdirs();
        }

        for (File file : Objects.requireNonNull(source.listFiles())) {
            if (file.isDirectory()) {
                copyWorldFolder(file, new File(destination, file.getName()));
            } else {
                Files.copy(file.toPath(), new File(destination, file.getName()).toPath());
            }
        }
    }

    public Location getRespawnPoint(UUID playerId) {
        return playerRespawnPoints.getOrDefault(playerId, Bukkit.getWorlds().get(0).getSpawnLocation());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby == null) {
            event.setCancelled(false);
            return;
        }
        GameConfig config = loadGameConfigFromWorld(lobby.getWorldFolder());

        if (!config.getAllowedBreakBlocks().contains(event.getBlock().getType()) || frozenPlayers.contains(player)) {
            player.sendMessage("§8[§6MiniGameCore§8]§c You are not allowed to break this block!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby == null) {
            event.setCancelled(false);
            return;
        }
        if (frozenPlayers.contains(player)) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);

        if (lobby != null) {
            event.setCancelled(true);
            player.setGameMode(GameMode.SPECTATOR);

            GameConfig config = loadGameConfigFromWorld(lobby.getWorldFolder());

            World lobbyWorld = Bukkit.getWorld(lobby.getWorldFolder().getName());
            if (lobbyWorld != null) {
                player.teleport(lobbyWorld.getSpawnLocation());
            }
            if (!config.getRespawnMode()) {
                player.sendMessage("§8[§6MiniGameCore§8]§c You died! §aYou are now spectating.");
                List<Player> alive = alivePlayers.get(lobby);
                if (alive != null) {
                    alive.remove(player);

                    if (alive.size() == 1) {
                        Player winner = alive.get(0);
                        winGame(lobby, winner);
                        alive.remove(lobby);
                    }
                }
            } else {
                int delay = config.getRespawnDelay();
                UUID uuid = player.getUniqueId();
                Location respawnLocation = getRespawnPoint(uuid);

                new BukkitRunnable() {
                    int secondsLeft = delay;

                    @Override
                    public void run() {
                        if (secondsLeft <= 0) {
                            player.teleport(respawnLocation);
                            player.setGameMode(GameMode.SURVIVAL);
                            player.sendTitle("§aRespawned!", "", 10, 20, 10);
                            this.cancel();
                        } else {
                            player.sendTitle("§cRespawning in", "§c" + secondsLeft + " s", 0, 20, 0);
                            secondsLeft--;
                        }
                    }
                }.runTaskTimer(plugin, 0, 20L);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby == null) {
            event.setCancelled(false);
            return;
        }

        GameConfig config = loadGameConfigFromWorld(lobby.getWorldFolder());

        if (!config.getAllowedPlaceBlocks().contains(event.getBlock().getType()) || frozenPlayers.contains(player)) {
            player.sendMessage("§8[§6MiniGameCore§8]§c You are not allowed to place this block!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onToolDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby == null) {
            event.setCancelled(false);
            return;
        }

        GameConfig config = loadGameConfigFromWorld(lobby.getWorldFolder());

        if (!config.getDurabilityMode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        final Entity damager = event.getDamager();
        final Entity damaged = event.getEntity();

        if (damager instanceof Player && damaged instanceof Player) {
            Lobby lobby = LobbyManager.getLobbyByPlayer((Player) damager);
            if (lobby == null) {
                event.setCancelled(false);
                return;
            }
            if (Objects.equals(lobby.getLobbyState(), "WAITING")) {
                damager.sendMessage("§8[§6MiniGameCore§8]§c You are not allowed to PVP (yet)");
                event.setCancelled(true);
            }

            GameConfig config = loadGameConfigFromWorld(lobby.getWorldFolder());
            if (!config.getPVPMode()) {
                event.setCancelled(true);
                damager.sendMessage("§8[§6MiniGameCore§8]§c You are not allowed to PVP");
            }
        }
    }

    @EventHandler
    public void catchContainerOpen(InventoryOpenEvent event)
    {
        if(!event.getInventory().getType().equals(InventoryType.PLAYER))
        {
            final Player player = (Player) event.getPlayer();
            Lobby lobby = LobbyManager.getLobbyByPlayer(player);
            if (lobby == null) {
                event.setCancelled(false);
                return;
            }
            if (!Objects.equals(lobby.getLobbyState(), "GAME")) {
                player.sendMessage("§8[§6MiniGameCore§8]§c You can't open Containers yet!");
                event.setCancelled(true);
            }
        }
    }
}
