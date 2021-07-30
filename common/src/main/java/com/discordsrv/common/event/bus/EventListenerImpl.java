/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.event.bus;

import com.discordsrv.api.event.bus.EventListener;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class EventListenerImpl implements EventListener {

    private final Class<?> listenerClass;
    private final Subscribe annotation;
    private final Class<?> eventClass;
    private final Method method;

    public EventListenerImpl(Class<?> listenerClass, Subscribe annotation, Class<?> eventClass, Method method) {
        this.listenerClass = listenerClass;
        this.annotation = annotation;
        this.eventClass = eventClass;
        this.method = method;
    }

    public boolean isIgnoringCancelled() {
        return annotation.ignoreCancelled();
    }

    public EventPriority priority() {
        return annotation.priority();
    }

    public Class<?> eventClass() {
        return eventClass;
    }

    public Method method() {
        return method;
    }

    @Override
    public @NotNull String className() {
        return listenerClass.getName();
    }

    @Override
    public @NotNull String methodName() {
        return method.getName();
    }

    @Override
    public String toString() {
        return "EventListenerImpl{" + className() + "#" + methodName() + "}";
    }
}
