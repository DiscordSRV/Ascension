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

package com.discordsrv.bukkit.debug;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.module.Module;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.core.debug.DebugGenerateEvent;
import com.discordsrv.common.core.debug.file.TextDebugFile;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class BukkitListenerTrackingModule extends AbstractModule<BukkitDiscordSRV> {

    public BukkitListenerTrackingModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "BUKKIT_LISTENER_TRACKER"));
    }

    @Subscribe
    public void onDebugGenerate(DebugGenerateEvent event) {
        CollectHandlerListEvent collectingEvent = new CollectHandlerListEvent();
        discordSRV.eventBus().publish(collectingEvent);

        Map<Class<?>, List<Module>> modulesByEvent = new LinkedHashMap<>();
        for (CollectedHandlerList handlerList : collectingEvent.getHandlerLists()) {
            Class<?> eventClass = handlerList.eventClass();
            modulesByEvent.computeIfAbsent(eventClass, key -> new ArrayList<>()).add(handlerList.module());
        }

        StringBuilder builder = new StringBuilder();
        for (Class<?> eventClass : modulesByEvent.keySet()) {
            builder.append(eventClass.getName()).append(": \n");

            HandlerList handlerList;
            try {
                handlerList = getHandlerList(eventClass);
            } catch (Throwable throwable) {
                builder.append(ExceptionUtils.getStackTrace(throwable)).append("\n\n");
                continue;
            }

            Plugin selfPlugin = discordSRV.plugin();

            RegisteredListener[] registeredListeners = handlerList.getRegisteredListeners();
            List<String> allUniquePlugins = SetUniqueList.setUniqueList(new ArrayList<>());
            for (RegisteredListener registeredListener : registeredListeners) {
                Plugin plugin = registeredListener.getPlugin();
                Listener listener = registeredListener.getListener();
                builder.append(" ")
                        .append(plugin == selfPlugin ? "*" : "-")
                        .append(" ")
                        .append(plugin.getName()).append(": ")
                        .append(listener.getClass().getName())
                        .append(" @ ")
                        .append(registeredListener.getPriority().name())
                        .append("\n");
                
                allUniquePlugins.add(plugin.getName());
            }
            allUniquePlugins.remove(selfPlugin.getName());
            if (registeredListeners.length == 0) {
                builder.append("no listeners");
            } else if (!allUniquePlugins.isEmpty()) {
                builder.append("listeners besides DiscordSRV: ")
                        .append(allUniquePlugins.stream().sorted().collect(Collectors.joining(", ")));
            } else {
                builder.append("no listeners besides DiscordSRV");
            }
            builder.append("\n\n");
        }

        event.addFile("registered-listeners.txt", new TextDebugFile(builder));
    }

    private HandlerList getHandlerList(Class<?> eventClass) throws ReflectiveOperationException, ClassCastException {
        while (eventClass != null) {
            try {
                Method method = eventClass.getMethod("getHandlerList");
                HandlerList handlerList = (HandlerList) method.invoke(null);
                if (handlerList != null) {
                    return handlerList;
                }
            } catch (NoSuchMethodException ignored) {}

            eventClass = eventClass.getSuperclass();
        }
        return null;
    }

    public static class CollectedHandlerList {

        private final Module module;
        private final Class<?> eventClass;

        public CollectedHandlerList(Module module, Class<?> eventClass) {
            this.module = module;
            this.eventClass = eventClass;
        }

        public Module module() {
            return module;
        }

        public Class<?> eventClass() {
            return eventClass;
        }
    }

    public static class CollectHandlerListEvent implements Event {

        private final List<CollectedHandlerList> handlerLists = new ArrayList<>();

        public void addHandlerList(Module module, Class<?> eventClass) {
            handlerLists.add(new CollectedHandlerList(module, eventClass));
        }

        public List<CollectedHandlerList> getHandlerLists() {
            return Collections.unmodifiableList(handlerLists);
        }
    }
}
