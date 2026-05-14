package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import wueffi.MiniGameCore.MiniGameCore;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static java.nio.file.Files.move;
import static org.bukkit.Bukkit.getServer;

public final class CleanUpWorlds {

    public static int worldsDeleted = 0;
    private static final File serverDirectory = getServer().getWorldContainer();

    public static void cleanUpWorlds(MiniGameCore plugin) {
        worldsDeleted = 0;
        for (File directory : serverDirectory.listFiles()) {
            for (String gamename : plugin.getAvailableGames()) {
                if (directory.isDirectory() && directory.getName().startsWith(gamename + "_copy_")) {
                    String name = directory.getName();
                    World world = Bukkit.getWorld(directory.getName());

                    if (world != null) {
                        boolean unloaded = Bukkit.unloadWorld(world, false);
                        for (Player player : world.getPlayers()) {
                            player.teleport(Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation());
                        }
                        if (!unloaded) {
                            plugin.getLogger().warning("Could not unload world: " + name);
                            return;
                        }
                    }
                    if (plugin.getKeepWorlds()) {
                        File archivedDir = new File(plugin.getDataFolder(), "archivedWorlds");
                        if (!archivedDir.exists() && !archivedDir.mkdirs()) {
                            plugin.getLogger().severe("Could not create archive directory: " + archivedDir.getPath());
                            return;
                        }
                        try {
                            move(directory.toPath(), archivedDir.toPath());
                            plugin.getLogger().info("Archived world: " + name);
                        } catch (IOException e) {
                            plugin.getLogger().severe("Failed to archive world: " + name);
                            e.printStackTrace();
                        }
                        return;
                    }
                    if (!delete(directory)) {
                        plugin.getLogger().warning("Failed to delete world folder: " + name);
                    }
                    worldsDeleted += 1;
                }
            }
        }
        if (plugin.getKeepWorlds() == true) plugin.getLogger().info("Found and deleted " + worldsDeleted + " old worlds.");
        else plugin.getLogger().info("Found and archived " + worldsDeleted + " old worlds.");
    }

    private static boolean delete (File file){
        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                delete(child);
            }
        }
        return file.delete();
    }
}

