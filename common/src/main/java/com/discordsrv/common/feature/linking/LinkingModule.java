/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.feature.linking;

import com.discordsrv.api.events.linking.AccountLinkedEvent;
import com.discordsrv.api.events.linking.AccountUnlinkedEvent;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.feature.profile.ProfileImpl;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.UUID;

public class LinkingModule extends AbstractModule<DiscordSRV> {

    private final Cache<Object, Boolean> linkCheckRateLimit;

    public LinkingModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "LINKING"));
        this.linkCheckRateLimit = discordSRV.caffeineBuilder()
                .expireAfterWrite(LinkStore.LINKING_CODE_RATE_LIMIT)
                .build();
    }

    public boolean rateLimit(Object identifier) {
        synchronized (linkCheckRateLimit) {
            boolean rateLimited = linkCheckRateLimit.getIfPresent(identifier) != null;
            if (!rateLimited) {
                linkCheckRateLimit.put(identifier, true);
            }
            return rateLimited;
        }
    }

    private LinkStore store() {
        LinkProvider provider = discordSRV.linkProvider();
        if (provider == null) {
            throw new IllegalStateException("LinkProvider is null");
        }

        return provider.store();
    }

    public Task<ProfileImpl> link(UUID playerUUID, long userId) {
        return store().createLink(playerUUID, userId)
                .then(v -> discordSRV.profileManager().loadProfile(playerUUID))
                .whenSuccessful(profile -> {
                    logger().debug("Linked: " + playerUUID + " & " + Long.toUnsignedString(userId));
                    discordSRV.eventBus().publish(new AccountLinkedEvent(profile));
                })
                .whenFailed(t -> logger().error("Failed to link " + playerUUID + " and " + Long.toUnsignedString(userId), t));
    }

    public Task<Void> unlink(UUID playerUUID, long userId) {
        return store().removeLink(playerUUID, userId)
                .whenComplete((v, t) -> {
                    logger().debug("Unlinked: " + playerUUID + " & " + Long.toUnsignedString(userId));
                    discordSRV.eventBus().publish(new AccountUnlinkedEvent(playerUUID, userId));
                })
                .whenFailed(t -> logger().error("Failed to unlink " + playerUUID + " and " + Long.toUnsignedString(userId), t));
    }
}
