package wueffi.MiniGameCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.utils.Lobby;
import wueffi.MiniGameCore.utils.ScoreBoard;
import wueffi.MiniGameCore.utils.Stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.bukkit.Bukkit.getLogger;
import static wueffi.MiniGameCore.managers.GameManager.alivePlayers;

public class ScoreBoardManager implements Listener {

    private static final Map<Player, String> playerGameStatus = new HashMap<>();
    private static final MiniGameCore plugin = JavaPlugin.getPlugin(MiniGameCore.class);
    private static final Logger log = LoggerFactory.getLogger(ScoreBoardManager.class);

    public static void setPlayerStatus(Player player, String status) {
        playerGameStatus.put(player, status);
    }

    public static void startAnimationLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                ScoreBoard.tickAnimation();
            }
        }.runTaskTimer(plugin, 0, 60);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerBoard(player);
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    public static void updatePlayerBoard(Player player) {
        String status = playerGameStatus.getOrDefault(player, "NONE");
        // getLogger().info(status);
        switch (status) {
            case "WAITING":
                Lobby lobby = LobbyManager.getLobbyByPlayer(player);
                if (lobby != null) ScoreBoard.createLobbyBoard(player, lobby);
                break;
            case "GAME":
                Lobby gameLobby = LobbyManager.getLobbyByPlayer(player);
                if (gameLobby != null) ScoreBoard.createGameBoard(player, new ArrayList<>(alivePlayers.get(gameLobby)));
                break;
            default:
                LobbyManager Lobbymanager = LobbyManager.getInstance();
                ScoreBoard.createIdleBoard(player,
                        Stats.getMostPlayedGame(player),
                        Stats.getTotalPlayed(player),
                        Stats.getTotalWins(player),
                        Stats.getTotalLosses(player),
                        Lobbymanager.getOpenLobbies(),
                        Lobbymanager.getClosedLobbies()
                );
        }
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setPlayerStatus(player, "NONE");
    }
}
