/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
