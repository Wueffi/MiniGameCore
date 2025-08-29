package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import wueffi.MiniGameCore.MiniGameCore;
import wueffi.MiniGameCore.managers.GameManager;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static java.nio.file.Files.move;
import static org.bukkit.Bukkit.getLogger;
import static wueffi.MiniGameCore.managers.LobbyManager.removeLobby;

public class LobbyHandler {
    private static MiniGameCore plugin;

    public static void LobbyReset(Lobby lobby) {
        if (lobby == null) {
            getLogger().warning("Lobby was null!");
            return;
        }
        deleteWorldFolder(lobby);
        removeLobby(lobby.getLobbyId());
    }

    private static void deleteWorldFolder(Lobby lobby) {
        String name = lobby.getWorldFolder().getName();
        World world = Bukkit.getWorld(name);

        if (world != null) {
            boolean unloaded = Bukkit.unloadWorld(world, false);
            if (!unloaded) {
                plugin.getLogger().warning("Could not unload world: " + name);
                return;
            }
        } else {
            return;
        }
        if (plugin.getKeepWorlds()) {
            File archivedDir = new File(plugin.getDataFolder(), "archivedWorlds");
            if (!archivedDir.exists() && !archivedDir.mkdirs()) {
                plugin.getLogger().severe("Could not create archive directory: " + archivedDir.getPath());
                return;
            }
            try {
                move(lobby.getWorldFolder().toPath(), archivedDir.toPath());
                plugin.getLogger().info("Archived world: " + name);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to archive world: " + name);
                e.printStackTrace();
            }
            return;
        }
        GameManager.runDelayed(() -> {
            if (delete(lobby.getWorldFolder())) {
                plugin.getLogger().info("Deleted world: " + name);
            } else {
                plugin.getLogger().warning("Failed to delete world folder: " + name);
            }
        }, 10);
    }

    private static boolean delete(File file) {
        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                delete(child);
            }
        }
        return file.delete();
    }
}
