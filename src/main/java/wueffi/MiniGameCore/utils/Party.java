package wueffi.MiniGameCore.utils;

import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;

public class Party {
    private final String partyId;
    private final String partyName;
    private final Player owner;
    private final Set<Player> players = new HashSet<>();
    private final Set<Player> playersInvited = new HashSet<>();

    public Party(String partyId, String partyName, Player owner) {
        this.partyId = partyId;
        this.partyName = partyName;
        this.owner = owner;
        players.add(owner);
    }

    public boolean addPlayer(Player player) {
        if (players.contains(player)) return false;
        if (!playersInvited.contains(player)) return false;
        playersInvited.remove(player);
        return players.add(player);
    }

    public boolean removePlayer(Player player) {
        return players.remove(player);
    }

    public boolean invitePlayer(Player player) {
        if (players.contains(player)) return false;
        if (playersInvited.contains(player)) return false;
        return playersInvited.add(player);
    }

    public boolean containsPlayer(Player player) {
        return players.contains(player);
    }

    public boolean isInvited(Player player) {
        return playersInvited.contains(player);
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public Set<Player> getPlayersInvited() {
        return playersInvited;
    }

    public String getPartyId() {
        return partyId;
    }

    public String getPartyName() {
        return partyName;
    }

    public boolean isOwner(Player player) {
        return owner.equals(player);
    }

    public Player getOwner() {
        return owner;
    }

    public boolean denyInvite(Player player) {
        return playersInvited.remove(player);
    }
}