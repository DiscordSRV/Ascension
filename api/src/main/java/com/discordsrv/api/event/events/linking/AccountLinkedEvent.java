package com.discordsrv.api.event.events.linking;

import com.discordsrv.api.event.events.Event;
import com.discordsrv.api.profile.IProfile;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * An event for when an account pair has been linked successfully.
 */
public class AccountLinkedEvent implements Event {

    private final IProfile profile;

    public AccountLinkedEvent(@NotNull IProfile profile) {
        if (!profile.isLinked()) {
            throw new IllegalStateException("Profile is not linked");
        }
        this.profile = profile;
    }

    /**
     * The profile of the linked account pair.
     * @return the profile
     */
    @NotNull
    public IProfile getProfile() {
        return profile;
    }

    /**
     * The UUID of the player that was linked, this player may not be connected to the server at the time of this event.
     * @return the player's {@link UUID}
     */
    @NotNull
    public UUID getPlayerUUID() {
        UUID uuid = profile.playerUUID();
        if (uuid == null) {
            throw new IllegalStateException("playerUUID is null");
        }
        return uuid;
    }

    /**
     * The user id of the user that was linked.
     * @return the user's Discord user id
     */
    public long getUserId() {
        Long userId = profile.userId();
        if (userId == null) {
            throw new IllegalStateException("userId is null");
        }
        return userId;
    }
}
