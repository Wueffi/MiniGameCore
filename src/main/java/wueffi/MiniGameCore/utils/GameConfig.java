package wueffi.MiniGameCore.utils;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameConfig {
    public final boolean RespawnMode;
    public final int RespawnDelay;
    private final FileConfiguration config;
    private final String gameName;
    private final int maxPlayers;
    private final int teams;
    private final List<SpawnPoint> spawnPoints = new ArrayList<>();
    private final List<TeamSpawnPoints> teamSpawnPoints = new ArrayList<>();
    private final List<Material> startInventory = new ArrayList<>();
    private final Set<Material> allowedBreakBlocks = new HashSet<>();
    private final Set<Material> allowedPlaceBlocks = new HashSet<>();
    private final boolean doDurability;

    public GameConfig(File configFile) {
        this.config = YamlConfiguration.loadConfiguration(configFile);

        this.gameName = config.getString("game.name", "default_game_name");
        this.maxPlayers = config.getInt("game.maxPlayers", 8);
        this.teams = config.getInt("game.teams", 0);
        this.RespawnMode = config.getBoolean("game.respawnMode", false);
        this.RespawnDelay = config.getInt("game.respawnDelay", 0);
        this.doDurability = config.getBoolean("game.doDurability", true);

        if (config.contains("game.spawnPoints")) {
            for (String key : config.getConfigurationSection("game.spawnPoints").getKeys(false)) {
                int x = config.getInt("game.spawnPoints." + key + ".x");
                int y = config.getInt("game.spawnPoints." + key + ".y");
                int z = config.getInt("game.spawnPoints." + key + ".z");
                spawnPoints.add(new SpawnPoint(x, y, z));
            }
        }

        if (config.contains("game.teamSpawnPoints")) {
            for (String team : config.getConfigurationSection("game.teamSpawnPoints").getKeys(false)) {
                List<TeamSpawnPoint> teamSpawns = new ArrayList<>();
                for (String key : config.getConfigurationSection("game.teamSpawnPoints." + team).getKeys(false)) {
                    int x = config.getInt("game.teamSpawnPoints." + team + "." + key + ".x");
                    int y = config.getInt("game.teamSpawnPoints." + team + "." + key + ".y");
                    int z = config.getInt("game.teamSpawnPoints." + team + "." + key + ".z");
                    teamSpawns.add(new TeamSpawnPoint(x, y, z));
                }
                teamSpawnPoints.add(new TeamSpawnPoints(team, teamSpawns));
            }
        }

        if (config.contains("game.inventory")) {
            for (String item : config.getStringList("game.inventory")) {
                Material material = Material.getMaterial(item.toUpperCase());
                if (material != null) {
                    startInventory.add(material);
                }
            }
        }

        if (config.contains("game.allowed_break_blocks")) {
            for (String block : config.getStringList("game.allowed_break_blocks")) {
                Material material = Material.getMaterial(block.toUpperCase());
                if (material != null) {
                    allowedBreakBlocks.add(material);
                }
            }
        }

        if (config.contains("game.allowed_place_blocks")) {
            for (String block : config.getStringList("game.allowed_place_blocks")) {
                Material material = Material.getMaterial(block.toUpperCase());
                if (material != null) {
                    allowedPlaceBlocks.add(material);
                }
            }
        }

    }

    public String getGameName() {
        return gameName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getTeams() {
        return teams;
    }

    public List<SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    public List<TeamSpawnPoints> getTeamSpawnPoints() {
        return teamSpawnPoints;
    }

    public List<Material> getStartInventory() {
        return startInventory;
    }

    public Set<Material> getAllowedBreakBlocks() {
        return allowedBreakBlocks;
    }

    public Set<Material> getAllowedPlaceBlocks() {
        return allowedPlaceBlocks;
    }

    public boolean getRespawnMode() {
        return RespawnMode;
    }

    public boolean getDurabilityMode() {
        return doDurability;
    }

    public Integer getRespawnDelay() {
        return RespawnDelay;
    }

    public static class SpawnPoint {
        private final int x, y, z;

        public SpawnPoint(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }
    }

    public static class TeamSpawnPoint {
        private final int x, y, z;

        public TeamSpawnPoint(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }
    }

    public static class TeamSpawnPoints {
        private final String teamName;
        private final List<TeamSpawnPoint> spawnPoints;

        public TeamSpawnPoints(String teamName, List<TeamSpawnPoint> spawnPoints) {
            this.teamName = teamName;
            this.spawnPoints = spawnPoints;
        }

        public String getTeamName() {
            return teamName;
        }

        public List<TeamSpawnPoint> getSpawnPoints() {
            return spawnPoints;
        }
    }
}
