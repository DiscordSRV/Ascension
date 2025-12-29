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
