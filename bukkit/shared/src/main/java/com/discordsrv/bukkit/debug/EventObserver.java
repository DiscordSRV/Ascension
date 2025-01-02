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

package com.discordsrv.bukkit.debug;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class EventObserver<E extends Event, P> {

    private final Plugin plugin;
    private final Class<?> eventClass;
    private final BiConsumer<RegisteredListener, E> observer;

    private final Function<E, P> propertyGetter;
    private final ThreadLocal<P> propertyValue = ThreadLocal.withInitial(() -> null);

    private final EnumMap<EventPriority, EventObserverList<E, P>> proxies = new EnumMap<>(EventPriority.class);
    private EnumMap<EventPriority, ArrayList<RegisteredListener>> originalMap;

    public EventObserver(Plugin plugin, @NotNull Class<E> eventClass, BiConsumer<RegisteredListener, E> observer, Function<E, P> propertyGetter) {
        this.plugin = plugin;
        this.eventClass = eventClass;
        this.observer = observer;
        this.propertyGetter = propertyGetter;
        inject();
    }

    public void close() {
        if (originalMap == null) {
            return;
        }

        try {
            for (Map.Entry<EventPriority, ArrayList<RegisteredListener>> entry : originalMap.entrySet()) {
                ArrayList<RegisteredListener> list = entry.getValue();
                list.clear();
                list.addAll(proxies.get(entry.getKey()).getRaw());
            }

            HandlerList handlerList = getHandlerList();
            setSlots(handlerList, originalMap);

            // Reset the handlers so it gets them again
            Field handlers = handlerList.getClass().getDeclaredField("handlers");
            handlers.setAccessible(true);
            handlers.set(handlerList, null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to clean up handler list.", e);
        }

        originalMap = null;
    }

    private void inject() {
        HandlerList handlerList = getHandlerList();
        EnumMap<EventPriority, ArrayList<RegisteredListener>> slots = getSlots(handlerList);
        originalMap = slots.clone();

        for (EventPriority eventPriority : slots.keySet()) {
            List<RegisteredListener> original = originalMap.get(eventPriority);
            EventObserverList<E, P> proxy = new EventObserverList<>(original, this);

            slots.put(eventPriority, proxy);
            proxies.put(eventPriority, proxy);
        }
    }

    private HandlerList getHandlerList() {
        Class<?> currentClass = eventClass;
        while (currentClass != null && Event.class.isAssignableFrom(currentClass)) {
            try {
                Method method = currentClass.getDeclaredMethod("getHandlerList");
                if (!method.isAccessible()) method.setAccessible(true);
                return (HandlerList) method.invoke(null);
            } catch (NoSuchMethodException ignored) {
                currentClass = currentClass.getSuperclass();
            } catch (Throwable e) {
                throw new RuntimeException("Could not get HandlerList", e);
            }
        }
        throw new RuntimeException("Unable to find HandlerList");
    }

    public void setSlots(HandlerList handlerList, EnumMap<EventPriority, ArrayList<RegisteredListener>> slots) {
        try {
            getSlotsField().set(handlerList, slots);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("Unable to set handlerslots field", e);
        }
    }

    @SuppressWarnings("unchecked")
    public EnumMap<EventPriority, ArrayList<RegisteredListener>> getSlots(HandlerList handlerList) {
        try {
            return (EnumMap<EventPriority, ArrayList<RegisteredListener>>) getSlotsField().get(handlerList);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to get handlerslots field", e);
        }
    }

    private static Field getSlotsField() throws NoSuchFieldException {
        Field field = HandlerList.class.getDeclaredField("handlerslots");
        if (!field.isAccessible()) field.setAccessible(true);
        return field;
    }

    public static class EventObserverList<E extends Event, P> extends ArrayList<RegisteredListener> {

        private final EventObserver<E, P> observer;

        public EventObserverList(Collection<RegisteredListener> original, EventObserver<E, P> observer) {
            super(original);
            this.observer = observer;
        }

        private List<RegisteredListener> getRaw() {
            // Avoid #toArray
            Iterator<RegisteredListener> iterator = iterator();
            List<RegisteredListener> listeners = new ArrayList<>();
            while (iterator.hasNext()) {
                listeners.add(iterator.next());
            }
            return listeners;
        }

        private List<RegisteredListener> getListeners() {
            List<RegisteredListener> listeners = new ArrayList<>();
            listeners.add(new CancellationDetectingListener<>(null, observer));
            for (RegisteredListener listener : this) {
                listeners.add(listener);
                listeners.add(new CancellationDetectingListener<>(listener, observer));
            }
            return listeners;
        }

        @Override
        public Object @NotNull [] toArray() {
            return getListeners().toArray();
        }

        @Override
        public <T> T @NotNull [] toArray(T[] a) {
            return getListeners().toArray(a);
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof CancellationDetectingListener) {
                // Prevent removing these from the collection
                return true;
            }
            return super.remove(o);
        }

        @Override
        public boolean add(RegisteredListener registeredListener) {
            if (registeredListener instanceof CancellationDetectingListener) {
                // Prevent adding these to the collection
                return true;
            }
            return super.add(registeredListener);
        }
    }

    public static class CancellationDetectingListener<E extends Event, P> extends RegisteredListener {

        private final RegisteredListener listener;
        private final EventObserver<E, P> observer;

        public CancellationDetectingListener(
                RegisteredListener listener,
                EventObserver<E, P> observer
        ) {
            super(
                    new Listener() {},
                    (l, e) -> {},
                    listener != null ? listener.getPriority() : EventPriority.LOWEST,
                    observer.plugin,
                    false
            );
            this.listener = listener;
            this.observer = observer;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void callEvent(Event event) {
            P propertyValue = observer.propertyGetter.apply((E) event);
            P previousValue = observer.propertyValue.get();

            if (!Objects.equals(propertyValue, previousValue)) {
                observer.propertyValue.set(propertyValue);
                if (listener != null) {
                    observer.observer.accept(listener, (E) event);
                }
            }
        }
    }
}
