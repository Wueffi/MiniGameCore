package wueffi.MiniGameCore.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import wueffi.MiniGameCore.managers.GameManager;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.utils.Lobby;
import wueffi.MiniGameCore.utils.Team;
import wueffi.MiniGameCore.utils.Winner;
import wueffi.MiniGameCore.MiniGameCore;

import java.util.UUID;

public final class MiniGameCoreAPI {
    private static final LobbyManager lobbyManager = MiniGameCore.getPlugin().getLobbyManager();

    public static LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public static void winPlayer(Lobby lobby, Player player) {
        GameManager.endGame(lobby, new Winner.PlayerWinner(player));
    }

    public static void winTeam(Lobby lobby, Team team) {
        GameManager.endGame(lobby, new Winner.TeamWinner(team));
    }

    public static void playerDeath(UUID playerID) {
        GameManager.playerDeath(playerID);
    }

    public static void playerAlive(UUID playerID) {
        GameManager.playerAlive(playerID);
    }

    public static boolean isInLobby(UUID playerId) {
        return LobbyManager.getLobbyByPlayer(Bukkit.getPlayer(playerId)) != null;
    }

    public static Location getRespawnLocation(UUID playerID) {
        return GameManager.getRespawnPoint(playerID);
    }
}
