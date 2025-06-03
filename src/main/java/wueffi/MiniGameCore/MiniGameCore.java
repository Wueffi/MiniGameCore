package wueffi.MiniGameCore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import wueffi.MiniGameCore.commands.MiniGameCommand;
import wueffi.MiniGameCore.managers.GameManager;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.managers.ScoreBoardManager;
import wueffi.MiniGameCore.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MiniGameCore extends JavaPlugin {
    private static MiniGameCore plugin;
    private List<String> availableGames;
    private List<UUID> bannedPlayers;

    @Override
    public void onEnable() {
        plugin.getLogger().info("MinigameCore enabled!");
        saveDefaultConfig();

        List<String> availableGames = getConfig().getStringList("available-games");
        List<UUID> bannedPlayers = new ArrayList<UUID>();
        for (String UUIDstring : getConfig().getStringList("banned-players")) {
            try {
                UUID uuid = UUID.fromString(UUIDstring);
                bannedPlayers.add(uuid);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Found invalid UUID in banned players list " + UUIDstring + ". Ignoring.");
            }
        }
        this.availableGames = availableGames;
        this.bannedPlayers = bannedPlayers;
        plugin.getLogger().info("Config loaded!");

        Stats.setup();
        plugin.getLogger().info("Stats loaded!");

        getCommand("mg").setExecutor(new MiniGameCommand(this));
        getCommand("mg").setTabCompleter(new MiniGameTabCompleter(this));
        plugin.getLogger().info("Commands registered!");

        ScoreBoardManager.startAnimationLoop();

        Bukkit.getPluginManager().registerEvents(new GameManager(this), this);
        getServer().getPluginManager().registerEvents(new PlayerHandler(), this);
    }

    @Override
    public void onDisable() {
        for (Lobby lobby : LobbyManager.getInstance().getOpenLobbies()) {
            String lobbyid = lobby.getLobbyId();
            plugin.getLogger().info("Lobby: " + lobbyid);
            for (Player player : LobbyManager.getInstance().getLobby(lobbyid).getPlayers()) {
                plugin.getLogger().info("Player: " + lobbyid);
                PlayerHandler.PlayerReset(player);
            }
            plugin.getLogger().info("Lobby disabling: " + lobbyid);
            LobbyHandler.LobbyReset(LobbyManager.getInstance().getLobby(lobbyid));
            plugin.getLogger().info("Shut down Lobby: " + lobbyid);
        }
        GameManager.frozenPlayers.clear();
        plugin.getLogger().info("MinigameCore disabled!");
    }

    public List<String> getAvailableGames() {
        return availableGames;
    }

    public List<UUID> getBannedPlayers() {
        return bannedPlayers;
    }

    private void writeBannedPlayers() {
        List<String> bannedPlayersString = new ArrayList<>();
        for (UUID player: bannedPlayers) {
            bannedPlayersString.add(player.toString());
        }
        getConfig().set("banned-players", bannedPlayersString);
        saveConfig();
    }

    public void banPlayer(UUID player) {
        bannedPlayers.add(player);
        writeBannedPlayers();
    }

    public void unbanPlayer(UUID player) {
        bannedPlayers.remove(player);
        writeBannedPlayers();
    }

    public static MiniGameCore getPlugin() {
        if (plugin == null) {
            plugin = JavaPlugin.getPlugin(MiniGameCore.class);
        }
        return plugin;
    }
}
