package wueffi.MiniGameCore.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.api.GameOverEvent;
import wueffi.MiniGameCore.api.GameStartEvent;
import wueffi.MiniGameCore.utils.*;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

import static wueffi.MiniGameCore.MiniGameCore.*;
import static wueffi.MiniGameCore.utils.PlayerHandler.PlayerSoftReset;

public final class GameManager implements Listener {
    static final Map<Lobby, List<Player>> alivePlayers = new HashMap<>();
    private static final Set<Player> frozenPlayers = new HashSet<>();
    private static final Map<UUID, Location> playerRespawnPoints = new HashMap<>();
    private static MiniGameCore plugin;
    private static final Map<UUID, UUID> lastHit = new HashMap<>();
    private static final Map<Lobby, GameConfig> configCache = new HashMap<>();
    private static final LobbyManager lobbyManager = MiniGameCore.getPlugin().getLobbyManager();

    public GameManager(MiniGameCore plugin) {
        GameManager.plugin = plugin;
    }

    public static GameConfig getConfig(Lobby lobby) {
        return configCache.computeIfAbsent(lobby, l -> loadGameConfigFromWorld(l.getWorldFolder()));
    }

    public static void startGame(Lobby lobby) {
        final GameConfig config = loadGameConfigFromWorld(lobby.getWorldFolder());

        if (config == null) {
            for (Player player : lobby.getPlayers()) {
                sendMGCError(player, " the game's config could not be loaded! Resetting...!");
            }
            LobbyHandler.LobbyReset(lobby);
            return;
        }

        int minPlayers = config.getMinPlayers();

        if (lobby.getPlayers().size() < minPlayers) {
                lobby.getOwner().sendMessage("§8[§6MiniGameCore§8]§c Not enough players to start Game! (required: " + minPlayers + ")");
                return;
        }

        for (Player player : lobby.getPlayers()) {
            sendMGCInfo(player, " " + lobby.getGameName() + " is starting!");
            lastHit.remove(player.getUniqueId()); // just to make sure
            frozenPlayers.add(player);
            PlayerSoftReset(player);
        }

        GameStartEvent gameStartEvent = new GameStartEvent(lobby.getGameName(), lobby);
        Bukkit.getPluginManager().callEvent(gameStartEvent);

        if (gameStartEvent.isCancelled()) {
            for (Player player : lobby.getPlayers()) {
                sendMGCError(player, " Game start was cancelled!");
            }
            return;
        }


        alivePlayers.put(lobby, new ArrayList<>(lobby.getPlayers()));
        startCountdown(lobby);
    }

    public static void endGame(Lobby lobby, Winner winner) {
        switch (winner) {
            case Winner.TieWinner(Collection<Player> players) -> {
                for (Player player : lobby.getPlayers()) {
                    if (players.contains(player)) Stats.tie(lobby.getGameName(), player);
                    else Stats.lose(lobby.getGameName(), player);
                    showTitle(player, "§6The Game", "was tied!", 10, 70, 20);
                    lastHit.remove(player.getUniqueId());
                    playerRespawnPoints.remove(player.getUniqueId());
                    runDelayed(() -> PlayerHandler.PlayerReset(player), 4);
                }
            }
            case Winner.TeamWinner(Team winnerTeam) -> {
                for (Team team : lobby.getTeamList()) {
                    if (team.equals(winnerTeam)) {
                        for (Player teamPlayer : team.getPlayers()) {
                            Stats.win(lobby.getGameName(), teamPlayer);
                            showTitle(teamPlayer, "§6Your Team", "won the Game!", 10, 70, 20);
                            teamPlayer.playSound(teamPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            lastHit.remove(teamPlayer.getUniqueId());
                            playerRespawnPoints.remove(teamPlayer.getUniqueId());
                            runDelayed(() -> PlayerHandler.PlayerReset(teamPlayer), 4);
                        }
                    } else {
                        for (Player teamPlayer : team.getPlayers()) {
                            Stats.lose(lobby.getGameName(), teamPlayer);
                            showTitle(teamPlayer, "§6The " + winnerTeam.getColorCode() + winnerTeam.getColor() + " §6Team", "won the Game!", 10, 70, 20);
                            teamPlayer.playSound(teamPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            lastHit.remove(teamPlayer.getUniqueId());
                            playerRespawnPoints.remove(teamPlayer.getUniqueId());
                            runDelayed(() -> PlayerHandler.PlayerReset(teamPlayer), 4);
                        }
                    }
                }
            }
            case Winner.PlayerWinner(Player winnerPlayer) -> {
                for (Player player : lobby.getPlayers()) {
                    if (player.equals(winnerPlayer)) {
                        Stats.win(lobby.getGameName(), player);
                    } else {
                        Stats.lose(lobby.getGameName(), player);
                    }

                    showTitle(player, "§6" + winnerPlayer.getName(), "won the Game!", 10, 70, 20);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    lastHit.remove(player.getUniqueId());
                    playerRespawnPoints.remove(player.getUniqueId());
                    runDelayed(() -> PlayerHandler.PlayerReset(player), 4);
                }
            }
            case null, default -> {
                for (Player player : lobby.getPlayers()) {
                    showTitle(player, "§6The Game", "was aborted.", 10, 70, 20);
                    player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
                    lastHit.remove(player.getUniqueId());
                    playerRespawnPoints.remove(player.getUniqueId());
                    runDelayed(() -> PlayerHandler.PlayerReset(player), 4);
                }
            }
        }

        configCache.remove(lobby);
        Bukkit.getPluginManager().callEvent(new GameOverEvent(lobby));
        runDelayed(() -> LobbyHandler.LobbyReset(lobby), 4);
    }

    private static void startCountdown(Lobby lobby) {
        lobby.setLobbyState("COUNTDOWN");
        GameConfig gameConfig = getConfig(lobby);
        List<Player> players = new ArrayList<>(lobby.getPlayers());
        Integer timeLimit = gameConfig.getTimeLimit();

        Collections.shuffle(players);

        if (gameConfig.getTeams() > 0) {
            int teamCount = gameConfig.getTeams();

            for (int i = 0; i < teamCount; i++) {
                if (!lobby.addTeam()) {
                    plugin.getLogger().warning("Failed to add team to " + lobby.getLobbyId() + ". ");
                }
            }

            for (int i = 0; i < players.size(); i++) {
                if (lobby.getTeam(i % teamCount).addPlayer(players.get(i))) {
                    lobby.getTeam(i % teamCount).updateAlive();
                } else {
                    plugin.getLogger().warning("Failed to add player " + players.get(i) + " to Team!");
                }
            }

            for (int teamIndex = 0; teamIndex < teamCount; teamIndex++) {
                Set<Player> teamPlayers = lobby.getTeam(teamIndex).getPlayers();
                List<GameConfig.TeamSpawnPoint> teamSpawns = new ArrayList<>(gameConfig.getTeamSpawnPoints().get(teamIndex).spawnPoints());
                Collections.shuffle(teamSpawns);

                for (Player teamPlayer : teamPlayers) {
                    if (teamSpawns.isEmpty()) {
                        plugin.getLogger().warning("Not enough SpawnPoints for Team " + (teamIndex + 1) + " in Lobby " + lobby.getLobbyId());
                        continue;
                    }

                    GameConfig.TeamSpawnPoint spawn = teamSpawns.removeFirst();
                    Location spawnLocation = new Location(teamPlayer.getWorld(), spawn.x(), spawn.y(), spawn.z());
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
                GameConfig.SpawnPoint spawn = spawnPoints.removeFirst();
                Location spawnLocation = new Location(player.getWorld(), spawn.x(), spawn.y(), spawn.z());
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
                        showTitle(player,"§aGame starting in " + timeLeft, "", 10, 70, 20);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
                    }
                    timeLeft--;
                } else {
                    lobby.setLobbyState("GAME");
                    for (Player player : lobby.getPlayers()) {
                        showTitle(player, "§aGame Started!", "§cTeaming / Cheating is bannable!", 10, 70, 20);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 5.0f);
                        ScoreBoardManager.setPlayerStatus(player, "GAME");
                        for (Material material : gameConfig.getStartInventory()) {
                            player.getInventory().addItem(new ItemStack(material));
                        }
                        frozenPlayers.remove(player);
                        runDelayed(() -> timeLimitGame(lobby), timeLimit);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static GameConfig loadGameConfigFromWorld(File worldFolder) {
        File configFile = new File(worldFolder, "config.yml");

        if (configFile.exists()) {
            return new GameConfig(configFile);
        } else {
            plugin.getLogger().warning("No config.yml found in world folder for " + worldFolder.getName() + ", this may cause Null-Pointer-Exceptions or other unwanted behaviour.");
            return null;
        }
    }

    public static void runDelayed(Runnable task, int seconds) {
        Bukkit.getScheduler().runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("MiniGameCore")), task, seconds * 20L);
    }

    public static Lobby hostGame(String gameName, Player player) {
        File miniGamesFolder = new File("plugins/MiniGameCore/MiniGames");

        File[] matchingWorlds = miniGamesFolder.listFiles(f ->
                f.isDirectory() && (f.getName().equals(gameName + "_world") || f.getName().matches(gameName + "_world\\d+"))
        );

        if (matchingWorlds == null || matchingWorlds.length == 0) {
            plugin.getLogger().warning("No template worlds found for game: " + gameName);
            return null;
        }

        File originalWorldFolder = matchingWorlds[new Random().nextInt(matchingWorlds.length)];
        String newWorldName = gameName + "_copy_" + System.currentTimeMillis();
        File newWorldFolder = new File(Bukkit.getWorldContainer(), newWorldName);

        if (originalWorldFolder.exists()) {
            try {
                copyWorldFolder(originalWorldFolder, newWorldFolder);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to copy world: " + e.getMessage());
                return null;
            }
            plugin.getLogger().info("World copied successfully.");
        } else {
            plugin.getLogger().warning("World folder not found for game: " + gameName);
            return null;
        }

        World newWorld = Bukkit.createWorld(new WorldCreator(newWorldFolder.getName()));

        if (newWorld == null) {
            plugin.getLogger().warning("Failed to load copied world: " + newWorldName);
            return null;
        } else {
            Location spawnLocation = newWorld.getSpawnLocation();
            player.teleport(spawnLocation);
            PlayerSoftReset(player);
            player.setGameMode(GameMode.SURVIVAL);
        }

        plugin.getLogger().info("Copied and loaded world: " + newWorldName);

        if (LobbyManager.getLobbyByPlayer(player) != null) {
            sendMGCError(player, " You are already in a game or lobby!");
            return null;
        }

        GameConfig gameConfig = loadGameConfigFromWorld(newWorldFolder);
        if (gameConfig == null) {
            sendMGCError(player, " Game config could not be loaded!");
            return null;
        }
        int maxPlayers = gameConfig.getMaxPlayers();

        Lobby lobby = lobbyManager.createLobby(gameName, maxPlayers, player, newWorldFolder);

        if (lobby == null) {
            sendMGCError(player, " Lobby could not be created!");
            return null;
        }

        Component message = Component.empty()
                .append(prefix)
                .append(Component.text(player.getName() + " is hosting " + lobby.getGameName() + "! ", NamedTextColor.DARK_GREEN))
                .append(Component.text(lobby.getPlayers().size() + "/" + maxPlayers + " players").color(NamedTextColor.GREEN))
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text("Click or type /mg join " + lobby.getLobbyId() + " to join the fun!")
                        .color(NamedTextColor.DARK_GREEN)
                        .clickEvent(ClickEvent.runCommand("/mg join " + lobby.getLobbyId()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to join " + lobby.getGameName(), NamedTextColor.GREEN)))
                );

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(gameConfig.getJoinPerm()))
                .forEach(p -> p.sendMessage(message));

        // Velocity Broadcast HEHEHEHE
        String serialized = LegacyComponentSerializer.legacySection().serialize(message);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Message");
        out.writeUTF("ALL");
        out.writeUTF(serialized);

        Bukkit.getOnlinePlayers().stream()
                .findFirst()
                .ifPresent(carrier ->
                        carrier.sendPluginMessage(plugin, "velocity:main", out.toByteArray())
                );

        if (lobby.isFull()) {
            startGame(lobby);
        }

        return lobby;
    }

    public static void timeLimitGame(Lobby lobby) {
        if (lobbyManager.getClosedLobbies().contains(lobby) || lobbyManager.getOpenLobbies().contains(lobby)) {
            List<Player> alive = alivePlayers.get(lobby);
            GameManager.endGame(lobby, new Winner.TieWinner(alive));
        }
    }

    public static void removeLastHitAndFrozen(UUID uuid) {
        lastHit.remove(uuid);
        if (Bukkit.getPlayer(uuid) != null) frozenPlayers.remove(Bukkit.getPlayer(uuid));
    }

    private static void copyWorldFolder(File source, File destination) throws Exception {
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

    public static Location getRespawnPoint(UUID playerId) {
        return playerRespawnPoints.getOrDefault(playerId, Bukkit.getWorlds().getFirst().getSpawnLocation());
    }

    public static void playerDeath(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby == null) return;

        GameConfig config = getConfig(lobby);

        if (config.getTeams() > 0) {
            Team team = lobby.getTeamByPlayer(player);
            if (team != null) team.decreaseAlive();
        } else {
            List<Player> alivePlayersNew = alivePlayers.get(lobby);
            alivePlayersNew.remove(player);

            alivePlayers.remove(lobby);
            alivePlayers.put(lobby, alivePlayersNew);
        }
    }

    public static void playerAlive(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);

        List<Player> alivePlayersNew = alivePlayers.get(lobby);
        alivePlayersNew.add(player);

        alivePlayers.remove(lobby);
        alivePlayers.put(lobby, alivePlayersNew);
    }

    public static void clearFrozenPlayers() {
        frozenPlayers.clear();
    }

    public static void showTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Component mainTitle = Component.text(title);
        Component subTitle = Component.text(subtitle);

        Title.Times titleTimes = Title.Times.times(
                Duration.ofMillis(50L * fadeInTicks),
                Duration.ofMillis(50L * stayTicks),
                Duration.ofMillis(50L * fadeOutTicks)
        );

        Title showPlayerTitle = Title.title(mainTitle, subTitle, titleTimes);
        player.showTitle(showPlayerTitle);
    }

    public static Collection<Player> getAlivePlayersByLobby(Lobby lobby) {
        return alivePlayers.get(lobby);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby == null) {
            event.setCancelled(false);
            return;
        }
        GameConfig config = getConfig(lobby);

        if (!config.getAllowedBreakBlocks().contains(event.getBlock().getType()) || frozenPlayers.contains(player) || lobby.getLobbyState().equals("WAITING")) {
            // TODO: If the message has been sent, maybe don't send another one for the same block for 5-10 seconds?
            sendMGCError(player, " You are not allowed to break this block!");
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
            player.setHealth(20);

            final World lobbyWorld = Bukkit.getWorld(lobby.getWorldFolder().getName());
            if (lobbyWorld != null) {
                player.teleport(lobbyWorld.getSpawnLocation());
            } else {
                plugin.getLogger().warning("Lobby world was null! " + player + "," + lobby);
                player.teleport(alivePlayers.get(lobby).getFirst());
            }
            player.setGameMode(GameMode.SPECTATOR);

            GameConfig config = getConfig(lobby);

            Player killer = null;
            UUID uuid = lastHit.get(player.getUniqueId());
            if (uuid != null) killer = Bukkit.getPlayer(uuid);

            if (!config.getSilenceDeathMessages()) {
                if (config.getTeams() > 0) {
                    Team team = lobby.getTeamByPlayer(player);
                    String color1 = "";
                    if (team != null) color1 = team.getColorCode();

                    team = lobby.getTeamByPlayer(killer);
                    String color2 = "";
                    if (team != null) color2 = team.getColorCode();

                    if (killer != null) {
                        for (Player player2 : lobby.getPlayers()) {
                            player2.sendMessage(color1 + player.getName() + "§7 was killed by " + color2 + killer.getName() + "§7.");
                        }
                    } else {
                        for (Player player2 : lobby.getPlayers()) {
                            player2.sendMessage(color1 + player.getName() + " §7died.");
                        }
                    }
                } else {
                    if (killer != null) {
                        for (Player player2 : lobby.getPlayers()) {
                            player2.sendMessage("§a" + player.getName() + "§7 was killed by §4" + killer.getName() + "§7.");
                        }
                    } else {
                        for (Player player2 : lobby.getPlayers()) {
                            player2.sendMessage("§a" + player.getName() + " §7died.");
                        }
                    }
                }
            }

            if (!config.getRespawnByAPI()) {
                List<Player> alive = alivePlayers.get(lobby);
                if (alive != null) {
                    alive.remove(player);
                }

                if (config.getTeams() > 0) {
                    Team team = lobby.getTeamByPlayer(player);
                    if (team == null) return;
                    team.decreaseAlive();
                    sendMGCError(player, " You died! §aYou are now spectating.");

                    int aliveTeams = 0;
                    Team lastAliveTeam = null;
                    for (Team team2 : lobby.getTeamList()) {
                        if (team2.getAlivePlayers() > 0) {
                            aliveTeams++;
                            lastAliveTeam = team2;
                        }
                    }

                    if (aliveTeams == 1 && lastAliveTeam != null) {
                        endGame(lobby, new Winner.TeamWinner(lastAliveTeam));
                    }
                } else {
                    if (!config.getRespawnMode()) {
                        sendMGCError(player, " You died! §aYou are now spectating.");

                        if (alive != null && alive.size() == 1) {
                            Player winner = alive.getFirst();
                            endGame(lobby, new Winner.PlayerWinner(winner));
                        }
                    } else {
                        int delay = config.getRespawnDelay();
                        uuid = player.getUniqueId();
                        Location respawnLocation = getRespawnPoint(uuid);

                        new BukkitRunnable() {
                            int secondsLeft = delay;

                            @Override
                            public void run() {
                                if (secondsLeft <= 0) {
                                    player.teleport(respawnLocation);
                                    player.setGameMode(GameMode.SURVIVAL);
                                    showTitle(player, "§aRespawned!", "", 10, 20, 10);

                                    List<Player> alive = alivePlayers.get(lobby);
                                    if (alive != null && !alive.contains(player)) {
                                        alive.add(player);
                                    }

                                    this.cancel();
                                } else {
                                    showTitle(player, "§cRespawning in", "§c" + secondsLeft + " s", 0, 20, 0);
                                    secondsLeft--;
                                }
                            }
                        }.runTaskTimer(plugin, 0, 20L);
                    }
                }
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

        GameConfig config = getConfig(lobby);

        if (!config.getAllowedPlaceBlocks().contains(event.getBlock().getType()) || frozenPlayers.contains(player) || lobby.getLobbyState().equals("WAITING")) {
            sendMGCError(player, " You are not allowed to place this block!");
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

        GameConfig config = getConfig(lobby);

        if (!config.getDurabilityMode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player damaged)) return;

        Entity d1 = event.getDamager();
        Player damager = null;

        if (d1 instanceof Player d2) {
            damager = d2;
        } else if (d1 instanceof Projectile proj && proj.getShooter() instanceof Player d2) {
            damager = d2;
        }

        Lobby lobby = LobbyManager.getLobbyByPlayer(damager); // also checks if damager is non-null
        if (lobby == null) return;

        if (Objects.equals(lobby.getLobbyState(), "WAITING")) {
            assert damager != null;
            damager.sendMessage("§8[§6MiniGameCore§8]§c You are not allowed to PVP (yet)");
            event.setCancelled(true);
            return;
        }

        GameConfig config = getConfig(lobby);
        if (!config.getPVPMode() && Objects.equals(lobby.getLobbyState(), "GAME")) {
            event.setCancelled(true);
            assert damager != null;
            damager.sendMessage("§8[§6MiniGameCore§8]§c You are not allowed to PVP");
            return;
        }

        if (!config.getAllowFriendlyFire() && config.getTeams() > 0 && Objects.equals(lobby.getTeamByPlayer(damager), lobby.getTeamByPlayer(damaged))) {
            event.setCancelled(true);
            assert damager != null;
            damager.sendMessage("§8[§6MiniGameCore§8]§c Friendly fire is not enabled for this minigame");
            return;
        }

        lastHit.remove(damaged.getUniqueId());
        assert damager != null;
        lastHit.put(damaged.getUniqueId(), damager.getUniqueId());
    }

    @EventHandler
    public void catchContainerOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby == null) {
            event.setCancelled(false);
            return;
        }
        GameConfig config = getConfig(lobby);

        if (lobby.getLobbyState().equals("GAME")) {
            return;
        }
        if (config.getAllowOpeningContainers()) {
            return;
        }
        if (event.getInventory().getType().equals(InventoryType.PLAYER)) {
            return;
        }

        sendMGCError(player, " You can't open Containers yet!");
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Lobby lobby = LobbyManager.getLobbyByPlayer(player);

        if (lobby == null) return;
        DamageCause damageCause = event.getCause();
        GameConfig config = getConfig(lobby);

        if (config.getBlockedDamageCauses().contains(damageCause)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);

        if (lobby == null) return;
        GameConfig config = getConfig(lobby);

        if (!config.getAllowCrafting()) {
            player.sendMessage("§7[§6MiniGameCore§7]§c You are not allowed to craft!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return; // annoyingly it uses HumanEntity :(
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby == null) return;
        GameConfig config = getConfig(lobby);

        if (config.getDoHunger()) return;

        event.setCancelled(true);
    }
}
