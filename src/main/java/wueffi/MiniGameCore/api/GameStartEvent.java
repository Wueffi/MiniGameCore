package wueffi.MiniGameCore.api;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import wueffi.MiniGameCore.utils.Lobby;

public final class GameStartEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final String gameName;
    private final Lobby lobby;
    private boolean cancelled;

    public GameStartEvent(String gameName, Lobby lobby) {
        this.gameName = gameName;
        this.lobby = lobby;
    }

    public String getGameName() {
        return gameName;
    }

    public Lobby getLobby() {
        return lobby;
    }

    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
