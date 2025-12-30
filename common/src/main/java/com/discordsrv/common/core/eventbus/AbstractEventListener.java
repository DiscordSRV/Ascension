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

package com.discordsrv.common.core.eventbus;

import com.discordsrv.api.eventbus.EventListener;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractEventListener<E> implements EventListener {

    private final Class<E> eventClass;
    private final boolean ignoreCanceled;
    private final boolean ignoreProcessed;
    private final byte priority;

    public AbstractEventListener(
            Class<E> eventClass,
            boolean ignoreCanceled,
            boolean ignoreProcessed,
            byte priority
    ) {
        this.eventClass = eventClass;
        this.ignoreCanceled = ignoreCanceled;
        this.ignoreProcessed = ignoreProcessed;
        this.priority = priority;
    }

    @Override
    public @NotNull Class<?> eventClass() {
        return eventClass;
    }

    @Override
    public boolean ignoringCanceled() {
        return ignoreCanceled;
    }

    @Override
    public boolean ignoringProcessed() {
        return ignoreProcessed;
    }

    @Override
    public byte priority() {
        return priority;
    }

    public abstract void invoke(E event) throws Throwable;
}
