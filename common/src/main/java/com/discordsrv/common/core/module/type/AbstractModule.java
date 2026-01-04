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

package com.discordsrv.common.core.module.type;

import com.discordsrv.api.discord.connection.details.DiscordCacheFlag;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.Processable;
import com.discordsrv.api.module.Module;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.util.EventUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractModule<DT extends DiscordSRV> implements Module {

    protected final DT discordSRV;
    private final Logger logger;
    private boolean isCurrentlyEnabled = false;
    private boolean serverHasStarted = false;

    private final List<DiscordGatewayIntent> requestedIntents = new ArrayList<>();
    private final List<DiscordCacheFlag> requestedCacheFlags = new ArrayList<>();
    private int requestedMemberCachePolicies = 0;

    public AbstractModule(DT discordSRV) {
        this(discordSRV, discordSRV.logger());
    }

    public AbstractModule(DT discordSRV, Logger logger) {
        this.discordSRV = discordSRV;
        this.logger = logger;
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    // Utility

    public final Logger logger() {
        return logger;
    }

    protected final boolean checkProcessor(Processable event) {
        return EventUtil.checkProcessor(discordSRV, event, logger());
    }

    protected final boolean checkCancellation(Cancellable event) {
        return EventUtil.checkCancellation(discordSRV, event, logger());
    }

    // Internal

    public boolean isCurrentlyDisabled() {
        return !isCurrentlyEnabled;
    }

    public final boolean enableModule() {
        if (isCurrentlyEnabled) {
            return false;
        }

        isCurrentlyEnabled = true;
        enable();

        try {
            discordSRV.eventBus().subscribe(getEventBusListener());
            // Ignore not having listener methods exception
        } catch (IllegalArgumentException ignored) {}
        return true;
    }

    public final void serverStartedForModule() {
        if (!serverHasStarted && isCurrentlyEnabled) {
            serverStarted();
        }
        serverHasStarted = true;
    }

    public final boolean disableModule() {
        if (!isCurrentlyEnabled) {
            return false;
        }

        try {
            discordSRV.eventBus().unsubscribe(this);
            // Ignore not having listener methods exception
        } catch (IllegalArgumentException ignored) {}

        disable();
        isCurrentlyEnabled = false;
        return true;
    }

    protected Object getEventBusListener() {
        return this;
    }

    public final void setRequestedIntents(Collection<DiscordGatewayIntent> intents) {
        this.requestedIntents.clear();
        this.requestedIntents.addAll(intents);
    }

    public final List<DiscordGatewayIntent> getRequestedIntents() {
        return requestedIntents;
    }

    public final void setRequestedCacheFlags(Collection<DiscordCacheFlag> cacheFlags) {
        this.requestedCacheFlags.clear();
        this.requestedCacheFlags.addAll(cacheFlags);
    }

    public final List<DiscordCacheFlag> getRequestedCacheFlags() {
        return requestedCacheFlags;
    }

    public final void setRequestedMemberCachePolicies(int amount) {
        this.requestedMemberCachePolicies = amount;
    }

    public final int getRequestedMemberCachePolicies() {
        return requestedMemberCachePolicies;
    }
}
