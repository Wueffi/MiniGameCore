package wueffi.MiniGameCore.utils;

import org.bukkit.entity.Player;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.managers.ScoreBoardManager;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class Lobby {
    private final String lobbyId;
    private final String gameName;
    private final int maxPlayers;
    private final Set<Player> players = new HashSet<>();
    private final Player owner;
    private final File worldFolder;
    private final Set<Player> readyPlayers = new HashSet<>();
    private String lobbyState;

    public Lobby(String lobbyId, String gameName, int maxPlayers, Player owner, File worldFolder, String LobbyState) {
        this.lobbyId = lobbyId;
        this.gameName = gameName;
        this.maxPlayers = maxPlayers;
        this.owner = owner;
        this.worldFolder = worldFolder;
        this.players.add(owner);
        this.lobbyState = LobbyState;
    }

    public boolean addPlayer(Player player) {
        if (players.contains(player)) return false;
        if (players.size() >= maxPlayers) return false;
        ScoreBoardManager.setPlayerStatus(player, "WAITING");
        return players.add(player);
    }

    public boolean removePlayer(Player player) {
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby.getPlayers() == player) {
            LobbyHandler.LobbyReset(lobby);
        }
        ScoreBoardManager.setPlayerStatus(player, "NONE");
        return players.remove(player);
    }

    public boolean ready(Player player) {
        return readyPlayers.add(player);
    }

    public boolean unready(Player player) {
        return readyPlayers.remove(player);
    }

    public String getLobbyState() {
        return lobbyState;
    }

    public void setLobbyState(String state) {
        this.lobbyState = state;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean containsPlayer(Player player) {
        return players.contains(player);
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public Set<Player> getReadyPlayers() {
        return readyPlayers;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public String getGameName() {
        return gameName;
    }

    public boolean isOwner(Player player) {
        return owner.equals(player);
    }

    public Player getOwner() {
        return owner;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public File getWorldFolder() {
        return worldFolder;
    }
}