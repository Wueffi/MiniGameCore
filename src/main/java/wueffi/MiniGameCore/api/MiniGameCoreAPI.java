package wueffi.MiniGameCore.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import wueffi.MiniGameCore.managers.GameManager;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.utils.Lobby;
import wueffi.MiniGameCore.utils.Team;
import wueffi.MiniGameCore.utils.Winner;

import java.util.UUID;

public class MiniGameCoreAPI {
    private static final LobbyManager lobbyManager = LobbyManager.getInstance();

    public static LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public static void winPlayer(Lobby lobby, Player player) {
        GameManager.endGame(lobby, new Winner.PlayerWinner(player));
    }

    public static void winTeam(Lobby lobby, Team team) {
        GameManager.endGame(lobby, new Winner.TeamWinner(team));
    }

    public static void playerDeath(UUID playerid) {
        GameManager.playerDeath(playerid);
    }

    public static void playerAlive(UUID playerid) {
        GameManager.playerAlive(playerid);
    }

    public static Location getRespawnLocation(UUID playerid) {
        return  GameManager.getRespawnPoint(playerid);
    }
}
