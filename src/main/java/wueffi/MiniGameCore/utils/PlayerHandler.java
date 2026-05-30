package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.managers.GameManager;
import wueffi.MiniGameCore.managers.LobbyManager;
import wueffi.MiniGameCore.managers.PartyManager;

public final class PlayerHandler implements Listener {
    private static Plugin plugin;
    private static final PartyManager partyManager = MiniGameCore.getPlugin().getPartyManager();

    public PlayerHandler(Plugin plugin2) {
        plugin = plugin2;
    }

    public static void PlayerReset(Player player) {
        Lobby lobby = LobbyManager.getLobbyByPlayer(player);
        Party party = PartyManager.getPartyByPlayer(player);

        if (lobby != null) {
            GameConfig config = GameManager.loadGameConfigFromWorld(lobby.getWorldFolder());
            if (config == null) return;
            if (config.getTeams() > 0) {
                Team team = lobby.getTeamByPlayer(player);
                if (team != null) {
                    if (team.removePlayer(player)) {
                        team.decreaseAlive();
                    } else {
                        plugin.getLogger().warning("Failed to remove player " + player + " from Team!");
                    }
                }
            }


            if (lobby.getOwner() == player) {
                lobby.removePlayer(player);
                for (Player player1 : lobby.getPlayers()) {
                    player1.sendMessage("§8[§6MiniGameCore§8]§c Owner of the lobby left... resetting");
                }
                LobbyHandler.LobbyReset(lobby);
            } else {
                lobby.removePlayer(player);
                if (lobby.getPlayers().isEmpty()) {
                    LobbyHandler.LobbyReset(lobby);
                }
            }
        }
        if (party != null) {
            if (player == party.getOwner()) {
                for (Player partyplayer : party.getPlayers()) {
                    partyplayer.sendMessage("§8[§6MiniGameCore§8]§c Owner of the party left... resetting");
                }
                partyManager.removeParty(party.getPartyId());
            } else {
                party.removePlayer(player);
            }
        }
        PlayerSoftReset(player);
        player.setGameMode(Bukkit.getDefaultGameMode());
        World mainWorld = Bukkit.getWorlds().getFirst();
        if (mainWorld != null) {
            Location spawn = mainWorld.getSpawnLocation();
            player.teleport(spawn);
        }
    }

    public static void PlayerSoftReset(Player player) {
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(5);
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        player.setFireTicks(0);
        player.setExp(0);
        player.setLevel(69);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setItemOnCursor(null);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager.removeLastHitAndFrozen(player.getUniqueId());
        GameManager.playerDeath(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> PlayerReset(player), 5L);
    }
}
