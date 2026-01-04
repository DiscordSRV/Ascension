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

package com.discordsrv.common.core.eventbus;

import java.util.function.Consumer;

public class DirectEventListener<E> extends AbstractEventListener<E> {

    private final Consumer<E> listener;

    public DirectEventListener(
            Class<E> eventClass,
            boolean ignoreCanceled,
            boolean ignoreProcessed,
            byte priority,
            Consumer<E> listener
    ) {
        super(eventClass, ignoreCanceled, ignoreProcessed, priority);
        this.listener = listener;
    }

    @Override
    public void invoke(E event) throws Throwable {
        listener.accept(event);
    }

    public String listenerClassName() {
        return listener.getClass().getName();
    }

    @Override
    public String toString() {
        return "DirectEventListener{listener=" + listenerClassName() + "}";
    }
}
