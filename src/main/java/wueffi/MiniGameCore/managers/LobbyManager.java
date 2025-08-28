package wueffi.MiniGameCore.managers;

import org.bukkit.entity.Player;
import wueffi.MiniGameCore.utils.Lobby;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LobbyManager {
    private static final LobbyManager instance = new LobbyManager();
    static final Map<String, Lobby> lobbies = new HashMap<>();
    private final Map<String, Integer> gameCounters = new HashMap<>();

    private LobbyManager() {
    }

    public static LobbyManager getInstance() {
        return instance;
    }

    public static Lobby getLobbyByPlayer(Player player) {
        return lobbies.values().stream()
                .filter(lobby -> lobby.containsPlayer(player))
                .findFirst()
                .orElse(null);
    }

    public static boolean removeLobby(String lobbyId) {
        return lobbies.remove(lobbyId) != null;
    }

    public Lobby createLobby(String gameName, int maxPlayers, Player owner, File newWorldFolder) {
        int id = gameCounters.getOrDefault(gameName, 0) + 1;
        gameCounters.put(gameName, id);

        String lobbyId = gameName + "-" + id;
        Lobby lobby = new Lobby(lobbyId, gameName, maxPlayers, owner, newWorldFolder, "WAITING");
        lobbies.put(lobbyId, lobby);
        lobby.addPlayer(owner);

        return lobby;
    }

    public Lobby getLobby(String lobbyId) {
        return lobbies.get(lobbyId);
    }

    public List<Lobby> getOpenLobbies() {
        return lobbies.values().stream()
                .filter(lobby -> Objects.equals(lobby.getLobbyState(), "WAITING"))
                .toList();
    }

    public List<Lobby> getClosedLobbies() {
        return lobbies.values().stream()
                .filter(lobby -> Objects.equals(lobby.getLobbyState(), "GAME"))
                .toList();
    }
}