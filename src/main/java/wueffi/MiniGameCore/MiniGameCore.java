package wueffi.MiniGameCore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import wueffi.MiniGameCore.commands.MiniGameCommand;
import wueffi.MiniGameCore.commands.PartyCommand;
import wueffi.MiniGameCore.commands.TeamChatCommand;
import wueffi.MiniGameCore.managers.GameManager;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.managers.PartyManager;
import wueffi.MiniGameCore.managers.ScoreBoardManager;
import wueffi.MiniGameCore.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public final class MiniGameCore extends JavaPlugin {
    private static MiniGameCore plugin;
    private static LobbyManager lobbyManager;
    private static PartyManager partyManager;
    private List<String> availableGames;
    private List<UUID> bannedPlayers;
    private Boolean keepWorlds;
    public static final Component prefix = Component.text()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("MiniGameCore", NamedTextColor.GOLD))
            .append(Component.text("]", NamedTextColor.GRAY))
            .hoverEvent(HoverEvent.showText(
                    Component.text("Made with ❤ by Waffle & others", NamedTextColor.GOLD)
            ))
            .build();

    @Override
    public void onEnable() {
        getLogger().info("MinigameCore enabled!");
        plugin = this;
        saveDefaultConfig();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "velocity:main");

        List<String> availableGames = getConfig().getStringList("available-games");
        List<UUID> bannedPlayers = new ArrayList<>();
        boolean keepWorlds = getConfig().getBoolean("keep-worlds");
        boolean disableScoreBoard = getConfig().getBoolean("disable-scoreboard");
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
        this.keepWorlds = keepWorlds;
        getLogger().info("Config loaded!");

        lobbyManager = new LobbyManager();
        partyManager = new PartyManager();

        Stats.setup();
        getLogger().info("Stats loaded!");

        PartyCommand partyCommand = new PartyCommand(this);
        PartyTabCompleter partyTabCompleter = new PartyTabCompleter(this);
        TeamChatCommand teamChatCommand = new TeamChatCommand();

        getCommand("mg").setExecutor(new MiniGameCommand(this));
        getCommand("mg").setTabCompleter(new MiniGameTabCompleter(this));
        getCommand("party").setExecutor(partyCommand);
        getCommand("party").setTabCompleter(partyTabCompleter);
        getCommand("p").setExecutor(partyCommand);
        getCommand("p").setTabCompleter(partyTabCompleter);
        getCommand("teamchat").setExecutor(teamChatCommand);
        getCommand("tc").setExecutor(teamChatCommand);
        getLogger().info("Commands registered!");

        getLogger().info("Starting cleanup task...");
        CleanUpWorlds.cleanUpWorlds(this);
        LobbyManager.cleanUpLobbies(this);

        if (!disableScoreBoard) ScoreBoardManager.startAnimationLoop();

        LobbyHandler.setPlugin(this);

        Bukkit.getPluginManager().registerEvents(new GameManager(this), this);
        getServer().getPluginManager().registerEvents(new PlayerHandler(this), this);
    }

    @Override
    public void onDisable() {
        for (Lobby lobby : Stream.concat(lobbyManager.getOpenLobbies().stream(), lobbyManager.getClosedLobbies().stream()).toList()) {
            String lobbyid = lobby.getLobbyId();
            for (Player player : lobby.getPlayers()) {
                PlayerHandler.PlayerReset(player);
            }
            getLogger().info("Lobby disabling: " + lobbyid);
            LobbyHandler.LobbyReset(lobby);
            getLogger().info("Shut down Lobby: " + lobbyid);
        }
        GameManager.clearFrozenPlayers();
        getLogger().info("Starting cleanup task...");
        CleanUpWorlds.cleanUpWorlds(this);
        getLogger().info("MinigameCore disabled!");
    }

    public List<String> getAvailableGames() {
        return availableGames;
    }

    public List<UUID> getBannedPlayers() {
        return bannedPlayers;
    }

    public Boolean getKeepWorlds() {
        return keepWorlds;
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

    public LobbyManager getLobbyManager() {
        if (lobbyManager == null) {
            lobbyManager = new LobbyManager();
            return lobbyManager;
        }
        return lobbyManager;
    }

    public PartyManager getPartyManager() {
        if (partyManager == null) {
            partyManager = new PartyManager();
            return partyManager;
        }
        return partyManager;
    }

    public static void sendMGCInfo(Player player, String message) {
        player.sendMessage(Component.text()
                .append(prefix)
                .append(Component.text(" ", NamedTextColor.WHITE)
                .append(Component.text(message, NamedTextColor.DARK_GREEN))
                .build());
    }

    public static void sendMGCError(Player player, String message) {
        player.sendMessage(Component.text()
                .append(prefix)
                .append(Component.text(" ", NamedTextColor.WHITE)
                .append(Component.text(message, NamedTextColor.RED))
                .build());
    }

    public static MiniGameCore getPlugin() {
        return plugin;
    }
}
