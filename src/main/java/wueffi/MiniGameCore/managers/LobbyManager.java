package wueffi.MiniGameCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import wueffi.MiniGameCore.utils.Lobby;

import java.io.File;
import java.util.*;

public final class LobbyManager {
    private static final Map<String, Lobby> lobbies = new HashMap<>();

    private LobbyManager() {
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

    public static Lobby createLobby(String gameName, int maxPlayers, Player owner, File newWorldFolder) {
        Set<Integer> used = new HashSet<>();
        String prefix = gameName + "-";
        for (String key : lobbies.keySet()) {
            if (key.startsWith(prefix)) {
                String numPart = key.substring(prefix.length());
                used.add(Integer.parseInt(numPart));
            }
        }

        int id = 1;
        while (used.contains(id)) {
            id++;
        }

        String lobbyId = gameName + "-" + id;
        Lobby lobby = new Lobby(lobbyId, gameName, maxPlayers, owner, newWorldFolder, "WAITING");
        lobbies.put(lobbyId, lobby);
        lobby.addPlayer(owner);

        return lobby;
    }

    public static Lobby getLobby(String lobbyId) {
        return lobbies.get(lobbyId);
    }

    public static List<Lobby> getOpenLobbies() {
        return lobbies.values().stream()
                .filter(lobby -> Objects.equals(lobby.getLobbyState(), "WAITING"))
                .toList();
    }

    public static List<Lobby> getClosedLobbies() {
        return lobbies.values().stream()
                .filter(lobby -> Objects.equals(lobby.getLobbyState(), "GAME"))
                .toList();
    }

    public static void cleanUpLobbies(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Lobby lobby : new ArrayList<>(LobbyManager.lobbies.values())) {
                Player owner = lobby.getOwner();
                if (owner == null || !owner.isOnline()) {
                    LobbyManager.removeLobby(lobby.getLobbyId());
                }
            }
        }, 0L, 400L);
    }
}