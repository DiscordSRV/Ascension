/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.module.type;

import com.discordsrv.api.event.events.Cancellable;
import com.discordsrv.api.event.events.Processable;
import com.discordsrv.api.module.type.Module;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.event.util.EventUtil;
import com.discordsrv.common.logging.Logger;

public abstract class AbstractModule<DT extends DiscordSRV> implements Module {

    protected final DT discordSRV;
    private final Logger logger;
    private boolean hasBeenEnabled = false;

    public AbstractModule(DT discordSRV) {
        this(discordSRV, discordSRV.logger());
    }

    public AbstractModule(DT discordSRV, Logger logger) {
        this.discordSRV = discordSRV;
        this.logger = logger;
    }

    public final Logger logger() {
        return logger;
    }

    public boolean isHasBeenEnabled() {
        return hasBeenEnabled;
    }

    public final boolean enableModule() {
        if (hasBeenEnabled || !isEnabled()) {
            return false;
        }

        hasBeenEnabled = true;
        enable();

        try {
            discordSRV.eventBus().subscribe(this);
            // Ignore not having listener methods exception
        } catch (IllegalArgumentException ignored) {}
        return true;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{enabled=" + isEnabled() + "}";
    }

    // Utility
    protected final boolean checkProcessor(Processable event) {
        return EventUtil.checkProcessor(discordSRV, event, logger());
    }

    protected final boolean checkCancellation(Cancellable event) {
        return EventUtil.checkCancellation(discordSRV, event, logger());
    }
}
