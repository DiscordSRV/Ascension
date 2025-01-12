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

package com.discordsrv.bukkit.listener;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.feature.debug.DebugObservabilityEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.function.Function;

public abstract class AbstractBukkitListener<E extends Event> extends AbstractModule<BukkitDiscordSRV> implements Listener {

    public AbstractBukkitListener(BukkitDiscordSRV discordSRV, Logger logger) {
        super(discordSRV, logger);
    }

    @Override
    public void enable() {
        discordSRV.server().getPluginManager().registerEvents(this, discordSRV.plugin());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    protected final void handleEvent(E event) {
        try {
            handleEvent(event, null);
        } catch (Throwable throwable) {
            logger().error("Failed to pass " + event.getClass().getName() + " to " + getClass().getName(), throwable);
        }
    }

    protected abstract void handleEvent(E event, Void __);

    @Subscribe
    public void onDebugObservability(DebugObservabilityEvent event) {
        observeEvents(event.isEnable());
    }

    protected abstract void observeEvents(boolean enable);

    protected final <T extends Event> EventObserver<T, Boolean> observeEvent(
            EventObserver<T, Boolean> observer,
            Class<T> eventClass,
            Function<T, Boolean> cancelProperty,
            boolean enable
    ) {
        if (observer != null) {
            observer.close();
        }

        if (!enable) {
            return null;
        }

        return new EventObserver<>(
                discordSRV.plugin(),
                eventClass,
                (registeredListener, event) -> {
                    boolean cancelled = cancelProperty.apply(event);
                    if (!cancelled) {
                        return;
                    }

                    Listener listener = registeredListener.getListener();
                    Plugin plugin = registeredListener.getPlugin();
                    logger().debug(
                            "Event \"" + event.getClass().getName() + "\" "
                                    + "cancelled by \"" + listener.getClass().getName() + "\" "
                                    + "of " + plugin.getName());
                },
                cancelProperty
        );
    }
}
