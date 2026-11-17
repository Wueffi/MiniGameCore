package wueffi.MiniGameCore.api;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class GameSpectateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final UUID uuid;
    private final UUID targetUUID;
    private boolean cancelled;

    public GameSpectateEvent(UUID uuid, UUID targetUUID) {
        this.uuid = uuid;
        this.targetUUID = targetUUID;
    }

    public UUID getPlayerUUID() {
        return uuid;
    }

    public UUID getTargetUUID() {
        return targetUUID;
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
