package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.managers.ScoreBoardManager;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Lobby {
    private final String lobbyId;
    private final String gameName;
    private final int maxPlayers;
    private final HashSet<UUID> players = new HashSet<>();
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
        this.players.add(owner.getUniqueId());
        this.lobbyState = LobbyState;
    }

    public boolean addPlayer(Player player) {
        if (players.contains(player.getUniqueId())) return false;
        if (players.size() >= maxPlayers) return false;
        ScoreBoardManager.setPlayerStatus(player, "WAITING");
        return players.add(player.getUniqueId());
    }

    public boolean removePlayer(Player player) {
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        if (lobby.getPlayers().size() == 1 && players.contains(player.getUniqueId())) {
            LobbyHandler.LobbyReset(lobby);
        }
        ScoreBoardManager.setPlayerStatus(player, "NONE");
        return players.remove(player.getUniqueId());
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
        return players.contains(player.getUniqueId());
    }

    public Set<Player> getPlayers() {
        Set<Player> result = new HashSet<>();
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                result.add(player);
            }
        }
        return result;
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