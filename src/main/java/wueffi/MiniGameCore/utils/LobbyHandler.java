package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import wueffi.MiniGameCore.MiniGameCore;

import java.io.File;
import java.util.Objects;

import static wueffi.MiniGameCore.managers.LobbyManager.removeLobby;

public class LobbyHandler {

    public static void LobbyReset(Lobby lobby) {
        if (lobby == null) {
            MiniGameCore.getPlugin().getLogger().warning("Lobby was null!");
            return;
        }
        deleteWorldFolder(lobby);
        removeLobby(lobby.getLobbyId());
    }

    private static void deleteWorldFolder(Lobby lobby) {
        World world = Bukkit.getWorld(lobby.getWorldFolder().getName());

        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
        delete(lobby.getWorldFolder());
        MiniGameCore.getPlugin().getLogger().info("Deleted world: " + lobby.getWorldFolder().getName());
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                delete(child);
            }
        }
        file.delete();
    }
}
