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

package com.discordsrv.common.event.bus;

import com.discordsrv.api.event.bus.EventBus;
import com.discordsrv.api.event.bus.EventListener;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.bus.internal.EventStateHolder;
import com.discordsrv.api.event.events.Cancellable;
import com.discordsrv.api.event.events.Event;
import com.discordsrv.api.event.events.Processable;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.exception.InvalidListenerMethodException;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import net.dv8tion.jda.api.events.GenericEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.discordsrv.common.exception.util.ExceptionUtil.minifyException;

public class EventBusImpl implements EventBus {

    private static final List<Pair<Function<Object, Boolean>, ThreadLocal<EventListener>>> STATES = Arrays.asList(
            Pair.of(event -> event instanceof Cancellable && ((Cancellable) event).isCancelled(), EventStateHolder.CANCELLED),
            Pair.of(event -> event instanceof Processable && ((Processable) event).isProcessed(), EventStateHolder.PROCESSED)
    );

    private final Map<Object, List<EventListenerImpl>> listeners = new ConcurrentHashMap<>();
    private final DiscordSRV discordSRV;
    private final Logger logger;

    public EventBusImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "EVENT_BUS");
    }

    @Override
    public void subscribe(@NotNull Object eventListener) {
        if (listeners.containsKey(eventListener)) {
            throw new IllegalArgumentException("Listener is already registered");
        }

        Class<?> listenerClass = eventListener.getClass();

        List<Throwable> suppressedMethods = new ArrayList<>();
        List<EventListenerImpl> methods = new ArrayList<>();
        EnumMap<EventPriority, List<EventListenerImpl>> methodsByPriority = new EnumMap<>(EventPriority.class);

        Class<?> currentClass = listenerClass;
        do {
            for (Method method : currentClass.getDeclaredMethods()) {
                checkMethod(listenerClass, method, suppressedMethods, methods, methodsByPriority);
            }
        } while ((currentClass = currentClass.getSuperclass()) != null);

        if (methods.isEmpty() || !suppressedMethods.isEmpty()) {
            IllegalArgumentException exception = new IllegalArgumentException(listenerClass.getName()
                    + " doesn't have valid listener methods that are annotated with " + Subscribe.class.getName());
            suppressedMethods.forEach(exception::addSuppressed);
            throw exception;
        }

        listeners.put(eventListener, methods);
        logger.debug("Listener " + eventListener.getClass().getName() + " subscribed");
    }

    private void checkMethod(Class<?> listenerClass, Method method,
                             List<Throwable> suppressedMethods, List<EventListenerImpl> methods,
                             EnumMap<EventPriority, List<EventListenerImpl>> methodsByPriority) {
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        if (annotation == null) {
            return;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        int parameters = parameterTypes.length;
        List<Throwable> suppressed = new ArrayList<>();

        if (Void.class.isAssignableFrom(method.getReturnType())) {
            suppressed.add(createReasonException("Must return void"));
        }

        int modifiers = method.getModifiers();
        if (Modifier.isAbstract(modifiers)) {
            suppressed.add(createReasonException("Cannot be abstract"));
        }
        if (Modifier.isStatic(modifiers)) {
            suppressed.add(createReasonException("Cannot be static"));
        }
        if (!Modifier.isPublic(modifiers)) {
            suppressed.add(createReasonException("Needs to be public"));
        }
        if (parameters != 1) {
            suppressed.add(createReasonException("Must have exactly 1 parameter"));
        }

        Class<?> firstParameter = null;
        if (parameters > 0) {
            firstParameter = parameterTypes[0];
            if (!Event.class.isAssignableFrom(firstParameter) && !GenericEvent.class.isAssignableFrom(firstParameter)) {
                suppressed.add(createReasonException("#1 argument must be a DiscordSRV or JDA event"));
            }
        }

        if (!suppressed.isEmpty()) {
            Exception methodException = new InvalidListenerMethodException("Method " + method.getName() + "(" +
                    (parameters > 0 ? Arrays.stream(method.getParameterTypes())
                            .map(Class::getName).collect(Collectors.joining(", ")) : "")
                    + ") is invalid");
            suppressed.forEach(methodException::addSuppressed);
            suppressedMethods.add(minifyException(methodException));
            return;
        }

        EventPriority eventPriority = annotation.priority();
        EventListenerImpl listener = new EventListenerImpl(listenerClass, annotation, firstParameter, method);

        methods.add(listener);
        methodsByPriority.computeIfAbsent(eventPriority, key -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    private Throwable createReasonException(String message) {
        InvalidListenerMethodException exception = new InvalidListenerMethodException(message);
        return minifyException(exception);
    }

    @Override
    public void unsubscribe(@NotNull Object eventListener) {
        listeners.remove(eventListener);
        logger.debug("Listener " + eventListener.getClass().getName() + " unsubscribed");
    }

    @Override
    public void publish(@NotNull Event event) {
        publishEvent(event);
    }

    @Override
    public void publish(@NotNull GenericEvent event) {
        publishEvent(event);
    }

    private void publishEvent(Object event) {
        List<Boolean> states = new ArrayList<>(STATES.size());
        for (Pair<Function<Object, Boolean>, ThreadLocal<EventListener>> entry : STATES) {
            if (entry.getKey().apply(event)) {
                // If the state is already set before listeners, we mark it as being changed by a 'unknown' event listener
                states.add(true);
                entry.getValue().set(EventStateHolder.UNKNOWN_LISTENER);
                continue;
            }
            states.add(false);
        }

        Class<?> eventClass = event.getClass();
        for (EventPriority priority : EventPriority.values()) {
            for (Map.Entry<Object, List<EventListenerImpl>> entry : listeners.entrySet()) {
                Object listener = entry.getKey();
                for (EventListenerImpl eventListener : entry.getValue()) {
                    if (eventListener.isIgnoringCancelled() && event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
                        continue;
                    }
                    if (eventListener.priority() != priority) {
                        continue;
                    }
                    if (!eventListener.eventClass().isAssignableFrom(eventClass)) {
                        continue;
                    }

                    long startTime = System.currentTimeMillis();
                    try {
                        eventListener.method().invoke(listener, event);
                    } catch (IllegalAccessException e) {
                        discordSRV.logger().error("Failed to access listener method: " + eventListener.methodName(), e);
                    } catch (InvocationTargetException e) {
                        discordSRV.logger().error("Failed to pass " + event.getClass().getSimpleName() + " to " + eventListener, e.getCause());
                    }
                    long timeTaken = System.currentTimeMillis() - startTime;
                    logger.trace(eventListener + " took " + timeTaken + "ms to execute");

                    for (int index = 0; index < STATES.size(); index++) {
                        Pair<Function<Object, Boolean>, ThreadLocal<EventListener>> state = STATES.get(index);

                        boolean current = states.get(index);
                        boolean updated = state.getKey().apply(event);
                        states.set(index, updated);

                        ThreadLocal<EventListener> stateHolder = state.getValue();
                        if (current != updated) {
                            if (updated) {
                                stateHolder.set(eventListener);
                            } else {
                                stateHolder.remove();
                            }
                        }
                    }
                }
            }
        }

        // Clear the states
        for (Pair<Function<Object, Boolean>, ThreadLocal<EventListener>> state : STATES) {
            state.getValue().remove();
        }
    }
}
