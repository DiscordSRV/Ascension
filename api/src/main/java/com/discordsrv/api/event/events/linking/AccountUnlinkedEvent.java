package com.discordsrv.api.event.events.linking;

import com.discordsrv.api.event.events.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * An event for when an account pair has been unlinked successfully.
 */
public class AccountUnlinkedEvent implements Event {

    private final UUID playerUUID;
    private final long userId;

    public AccountUnlinkedEvent(@NotNull UUID playerUUID, long userId) {
        this.playerUUID = playerUUID;
        this.userId = userId;
    }

    /**
     * The UUID of the player that was unlinked, this player may not be connected to the server at the time of this event.
     * @return the player's {@link UUID}
     */
    @NotNull
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * The user id of the user that was unlinked.
     * @return the user's Discord user id
     */
    public long getUserId() {
        return userId;
    }
}
