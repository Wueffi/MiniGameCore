package wueffi.MiniGameCore.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import wueffi.MiniGameCore.utils.Lobby;

public final class GameOverEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Lobby lobby;

    public GameOverEvent(Lobby lobby) {
        this.lobby = lobby;
    }

    public Lobby getLobby() {
        return lobby;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
