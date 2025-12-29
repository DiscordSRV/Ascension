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
