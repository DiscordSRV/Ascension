package com.discordsrv.bukkit.listener;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.debug.BukkitListenerTrackingModule;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.core.debug.DebugObservabilityEvent;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.module.type.AbstractModule;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Debugging utilities for Bukkit events.
 */
public abstract class AbstractBukkitEventObserver extends AbstractModule<BukkitDiscordSRV> {

    public AbstractBukkitEventObserver(BukkitDiscordSRV discordSRV, Logger logger) {
        super(discordSRV, logger);
    }

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

    @Subscribe
    public void onCollectHandlerList(BukkitListenerTrackingModule.CollectHandlerListEvent event) {
        collectRelevantHandlerLists(eventClass -> event.addHandlerList(this, eventClass));
    }

    protected abstract void collectRelevantHandlerLists(Consumer<Class<?>> eventClassConsumer);
}
