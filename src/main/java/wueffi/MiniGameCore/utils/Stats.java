package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Stats {

    private static File statsFile;
    private static FileConfiguration statsConfig;

    public static void setup() {
        statsFile = new File(Bukkit.getPluginManager().getPlugin("MiniGameCore").getDataFolder(), "Stats.yml");

        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    private static void save() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean getStats(UUID uuid) {
        return statsConfig.contains(uuid.toString());
    };

    private static void ensurePlayer(UUID uuid, String game) {
        String base = uuid.toString() + "." + game;
        if (!statsConfig.contains(base)) {
            statsConfig.set(base + ".played", 0);
            statsConfig.set(base + ".won", 0);
            statsConfig.set(base + ".lost", 0);
        }
    }

    public static void win(String game, Player player) {
        UUID uuid = player.getUniqueId();
        ensurePlayer(uuid, game);

        String base = uuid + "." + game;
        statsConfig.set(base + ".played", statsConfig.getInt(base + ".played") + 1);
        statsConfig.set(base + ".won", statsConfig.getInt(base + ".won") + 1);
        save();
    }

    public static void lose(String game, Player player) {
        UUID uuid = player.getUniqueId();
        ensurePlayer(uuid, game);

        String base = uuid + "." + game;
        statsConfig.set(base + ".played", statsConfig.getInt(base + ".played") + 1);
        statsConfig.set(base + ".lost", statsConfig.getInt(base + ".lost") + 1);
        save();
    }

    public static int getPlayed(String game, OfflinePlayer player) {
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        String base = player.getUniqueId() + "." + game;
        return statsConfig.getInt(base + ".played");
    }

    public static int getWins(String game, OfflinePlayer player) {
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        String base = player.getUniqueId() + "." + game;
        return statsConfig.getInt(base + ".won");
    }

    public static int getLosses(String game, OfflinePlayer player) {
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        String base = player.getUniqueId() + "." + game;
        return statsConfig.getInt(base + ".lost");
    }

    public static int getTotalPlayed(OfflinePlayer player) {
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        String uuid = player.getUniqueId().toString();
        int total = 0;

        if (statsConfig.contains(uuid)) {
            for (String game : statsConfig.getConfigurationSection(uuid).getKeys(false)) {
                total += statsConfig.getInt(uuid + "." + game + ".played");
            }
        }

        return total;
    }

    public static int getTotalWins(OfflinePlayer player) {
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        String uuid = player.getUniqueId().toString();
        int total = 0;

        if (statsConfig.contains(uuid)) {
            for (String game : statsConfig.getConfigurationSection(uuid).getKeys(false)) {
                total += statsConfig.getInt(uuid + "." + game + ".won");
            }
        }

        return total;
    }

    public static int getTotalLosses(OfflinePlayer player) {
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        String uuid = player.getUniqueId().toString();
        int total = 0;

        if (statsConfig.contains(uuid)) {
            for (String game : statsConfig.getConfigurationSection(uuid).getKeys(false)) {
                total += statsConfig.getInt(uuid + "." + game + ".lost");
            }
        }

        return total;
    }

    public static String getMostPlayedGame(OfflinePlayer player) {
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        String uuid = player.getUniqueId().toString();
        String mostPlayedGame = "None";
        int maxPlayed = -1;

        if (statsConfig.contains(uuid)) {
            for (String game : statsConfig.getConfigurationSection(uuid).getKeys(false)) {
                int played = statsConfig.getInt(uuid + "." + game + ".played");
                if (played > maxPlayed) {
                    maxPlayed = played;
                    mostPlayedGame = game;
                }
            }
        }

        return mostPlayedGame;
    }
}