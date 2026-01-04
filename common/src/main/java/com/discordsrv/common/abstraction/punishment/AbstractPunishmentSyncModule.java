/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.abstraction.punishment;

import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.cause.ISyncCause;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.Game;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public abstract class AbstractPunishmentSyncModule<C extends AbstractSyncConfig<C, Game, Long>>
        extends AbstractSyncModule<DiscordSRV, C, Game, Long, Punishment> {

    protected final Map<Long, PunishmentEvent> events = new ConcurrentHashMap<>();

    protected AbstractPunishmentSyncModule(DiscordSRV discordSRV, String loggerName) {
        super(discordSRV, loggerName);
    }

    @Override
    protected @Nullable ISyncResult doesStateMatch(Punishment one, Punishment two) {
        boolean oneActive = one != null;
        boolean twoActive = two != null;
        if (oneActive != twoActive) return null;

        if (!oneActive) return GenericSyncResults.both(false);
        if (one.until() != null && one.until().equals(two.until())) return GenericSyncResults.both(true);

        return null;
    }

    @Override
    public Punishment getRemovedState() {
        return null;
    }

    protected PunishmentEvent upsertEvent(long guildId, long userId, boolean newState, ISyncCause fallbackCause) {
        return events.computeIfAbsent(userId, key -> new PunishmentEvent(guildId, userId, newState, fallbackCause));
    }

    protected class PunishmentEvent {
        private final long guildId;
        private final long userId;
        private final boolean newState;
        private final Future<?> future;

        public PunishmentEvent(long guildId, long userId, boolean newState, ISyncCause fallbackCause) {
            this.guildId = guildId;
            this.userId = userId;
            this.newState = newState;

            // If audit log entry doesn't arrive, apply fallback after 5s.
            this.future = discordSRV.scheduler().runLater(
                    () -> applyPunishment(null, fallbackCause),
                    Duration.ofSeconds(5)
            );
        }

        public void applyPunishment(@Nullable Punishment punishment, ISyncCause cause) {
            if (!future.cancel(false)) {
                return; // fallback already executed
            }

            if (newState && punishment == null) {
                punishment = Punishment.UNKNOWN;
            }

            discordChanged(
                    cause,
                    Someone.of(discordSRV, userId),
                    guildId,
                    punishment
            );

            events.remove(userId);
        }

        public long guildId() {
            return guildId;
        }

        public long userId() {
            return userId;
        }
    }
}
