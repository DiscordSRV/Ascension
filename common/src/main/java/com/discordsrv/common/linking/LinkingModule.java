package com.discordsrv.common.linking;

import com.discordsrv.api.event.events.linking.AccountLinkedEvent;
import com.discordsrv.api.event.events.linking.AccountUnlinkedEvent;
import com.discordsrv.api.profile.IProfile;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;

import java.util.UUID;

public class LinkingModule extends AbstractModule<DiscordSRV> {

    public LinkingModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "LINKING"));
    }

    public void linked(UUID playerUUID, long userId) {
        IProfile profile = discordSRV.profileManager().getProfile(playerUUID);
        if (profile == null || !profile.isLinked()) {
            throw new IllegalStateException("Notified that account linked, but profile is null or unlinked");
        }

        discordSRV.eventBus().publish(new AccountLinkedEvent(profile));
    }

    public void unlinked(UUID playerUUID, long userId) {
        discordSRV.eventBus().publish(new AccountUnlinkedEvent(playerUUID, userId));
    }
}
