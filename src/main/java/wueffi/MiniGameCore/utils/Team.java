package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public final class Team {
    private final String teamId;
    private final HashSet<UUID> players = new HashSet<>();
    private int alivePlayers;
    private final Map<Integer, String> colors = new HashMap<>();
    private final Map<Integer, String> colorCodes = new HashMap<>();

    public Team(String teamId) {
        this.teamId = teamId;
        this.alivePlayers = 0;
        colors.put(0, "Red");
        colors.put(1, "Blue");
        colors.put(2, "Yellow");
        colors.put(3, "Green");
        colors.put(4, "Cyan");
        colors.put(5, "Pink");
        colors.put(6, "Orange");
        colors.put(7, "White");
        colorCodes.put(0, "§4");
        colorCodes.put(1, "§1");
        colorCodes.put(2, "§e");
        colorCodes.put(3, "§2");
        colorCodes.put(4, "§b");
        colorCodes.put(5, "§d");
        colorCodes.put(6, "§6");
        colorCodes.put(7, "§f");
    }

    public boolean addPlayer(Player player) {
        if (players.contains(player.getUniqueId())) return false;
        return players.add(player.getUniqueId());
    }

    public boolean removePlayer(Player player) {
        return players.remove(player.getUniqueId());
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

    public void updateAlive() {
        alivePlayers = players.size();
    }

    public void decreaseAlive() {
        alivePlayers = alivePlayers -1;
        if (alivePlayers <= -1) alivePlayers = 0;
    }

    public int getAlivePlayers() {
        return alivePlayers;
    }

    public String getColor() {
        return colors.get(Integer.parseInt(teamId));
    }

    public String getColorCode() {
        return colorCodes.get(Integer.parseInt(teamId));
    }
}