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

import com.discordsrv.api.eventbus.EventBus;
import com.discordsrv.api.eventbus.EventListener;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.eventbus.internal.EventStateHolder;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.events.Processable;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.exception.InvalidListenerMethodException;
import com.discordsrv.common.feature.debug.DebugGenerateEvent;
import com.discordsrv.common.feature.debug.file.TextDebugFile;
import com.discordsrv.common.helper.TestHelper;
import net.dv8tion.jda.api.events.GenericEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.discordsrv.common.util.ExceptionUtil.minifyException;

public class EventBusImpl implements EventBus {

    private static final List<State<?>> STATES = Arrays.asList(
            new State<>(Cancellable.class, Cancellable::isCancelled, EventStateHolder.CANCELLED),
            new State<>(Processable.class, Processable::isProcessed, EventStateHolder.PROCESSED)
    );

    private final Map<Object, List<EventListenerImpl>> listeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<EventListenerImpl>> listenersByEvent = new ConcurrentHashMap<>();
    private final Logger logger;

    public EventBusImpl(DiscordSRV discordSRV) {
        this.logger = new NamedLogger(discordSRV, "EVENT_BUS");

        // For debug generation
        subscribe(this);
    }

    public void shutdown() {
        listeners.clear();
        listenersByEvent.clear();
    }

    @Override
    public void subscribe(@NotNull Object eventListener) {
        if (listeners.containsKey(eventListener)) {
            throw new IllegalArgumentException("Listener is already registered");
        }

        Pair<List<EventListenerImpl>, List<Throwable>> parsed = parseListeners(eventListener);
        List<Throwable> suppressedMethods = parsed.getValue();
        List<EventListenerImpl> methods = parsed.getKey();

        if (methods.isEmpty() || !suppressedMethods.isEmpty()) {
            IllegalArgumentException exception = new IllegalArgumentException(eventListener.getClass().getName()
                    + " doesn't have valid listener methods that are annotated with " + Subscribe.class.getName());
            suppressedMethods.forEach(exception::addSuppressed);
            throw exception;
        }

        listeners.put(eventListener, methods);
        for (EventListenerImpl method : methods) {
            listenersByEvent.computeIfAbsent(method.eventClass(), key -> new CopyOnWriteArrayList<>())
                    .add(method);
        }
        logger.debug("Listener " + eventListener.getClass().getName() + " subscribed");
    }

    @Override
    public Collection<EventListenerImpl> getListeners(@NotNull Object eventListener) {
        return parseListeners(eventListener).getKey();
    }

    private Pair<List<EventListenerImpl>, List<Throwable>> parseListeners(Object eventListener) {
        Class<?> listenerClass = eventListener.getClass();

        List<Throwable> suppressedMethods = new ArrayList<>();
        List<EventListenerImpl> methods = new ArrayList<>();

        Class<?> currentClass = listenerClass;
        do {
            for (Method method : currentClass.getDeclaredMethods()) {
                checkMethod(eventListener, listenerClass, method, suppressedMethods, methods);
            }
        } while ((currentClass = currentClass.getSuperclass()) != null);

        return Pair.of(methods, suppressedMethods);
    }

    private void checkMethod(Object eventListener, Class<?> listenerClass, Method method,
                             List<Throwable> suppressedMethods, List<EventListenerImpl> methods) {
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        if (annotation == null) {
            return;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        int parameters = parameterTypes.length;
        List<Throwable> suppressed = new ArrayList<>();

        Class<?> returnType = method.getReturnType();
        if (Void.class.isAssignableFrom(returnType)) {
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

        MethodHandle handle = null;
        MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
        try {
            handle = MethodHandles.lookup().findVirtual(listenerClass, method.getName(), methodType);
        } catch (ReflectiveOperationException e) {
            suppressedMethods.add(e);
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

        EventListenerImpl listener = new EventListenerImpl(eventListener, listenerClass, annotation, firstParameter, method, handle);
        methods.add(listener);
    }

    private Throwable createReasonException(String message) {
        InvalidListenerMethodException exception = new InvalidListenerMethodException(message);
        return minifyException(exception);
    }

    @Override
    public void unsubscribe(@NotNull Object eventListener) {
        List<EventListenerImpl> removed = listeners.remove(eventListener);
        if (removed != null) {
            for (EventListenerImpl listener : removed) {
                Class<?> eventClass = listener.eventClass();
                List<EventListenerImpl> listeners = listenersByEvent.get(eventClass);
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    listenersByEvent.remove(eventClass);
                }
            }
            logger.debug("Listener " + eventListener.getClass().getName() + " unsubscribed");
        }
    }

    @Override
    public void publish(@NotNull Event event) {
        publishEvent(event);
    }

    @Override
    public void publish(@NotNull GenericEvent event) {
        publishEvent(event);
    }

    private void gatherListeners(Class<?> eventClass, List<EventListenerImpl> listeners) {
        List<EventListenerImpl> listenersForEvent = this.listenersByEvent.get(eventClass);
        if (listenersForEvent == null) {
            return;
        }
        listeners.addAll(listenersForEvent);
    }

    private void publishEvent(Object event) {
        Class<?> checkClass = event.getClass();

        Map<State<?>, Boolean> states = new HashMap<>(STATES.size());
        for (State<?> state : STATES) {
            if (state.eventClass().isAssignableFrom(checkClass)) {
                boolean value = state.statePredicate().test(event);
                states.put(state, value);

                if (value) {
                    state.stateHolder().set(EventStateHolder.UNKNOWN_LISTENER);
                }
            }
        }

        List<EventListenerImpl> listeners = new ArrayList<>();
        while (!Object.class.equals(checkClass)) {
            gatherListeners(checkClass, listeners);
            for (Class<?> anInterface : checkClass.getInterfaces()) {
                gatherListeners(anInterface, listeners);
            }

            checkClass = checkClass.getSuperclass();
        }

        listeners.sort(Comparator.comparingInt(EventListenerImpl::priority));

        for (EventListenerImpl eventListener : listeners) {
            if (eventListener.isIgnoringCancelled() && event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
                continue;
            }
            if (eventListener.isIgnoringProcessed() && event instanceof Processable && ((Processable) event).isProcessed()) {
                continue;
            }

            long startTime = System.currentTimeMillis();
            try {
                Object listener = eventListener.listener();
                eventListener.handle().invoke(listener, event);
            } catch (Throwable e) {
                String eventClassName = event.getClass().getName();
                if (eventListener.className().startsWith("com.discordsrv")) {
                    logger.error("Failed to pass " + eventClassName + " to " + eventListener, e);
                } else {
                    // Print the listener failing without references to the DiscordSRV event bus
                    // as it isn't relevant to the exception, and often causes users to suspect DiscordSRV is doing something wrong when it isn't
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
                TestHelper.fail(e);
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            logger.trace(eventListener + " took " + timeTaken + "ms to execute");

            for (Map.Entry<State<?>, Boolean> entry : states.entrySet()) {
                State<?> state = entry.getKey();
                boolean currentValue = entry.getValue();
                boolean newValue = state.statePredicate().test(event);

                if (currentValue == newValue) {
                    continue;
                }

                if (currentValue) {
                    state.stateHolder().set(eventListener);
                } else {
                    state.stateHolder().set(EventStateHolder.UNKNOWN_LISTENER);
                }
            }
        }

        // Clear the states
        for (State<?> state : states.keySet()) {
            state.stateHolder().remove();
        }
    }

    @Subscribe
    public void onDebugGenerate(DebugGenerateEvent event) {
        StringBuilder builder = new StringBuilder("Registered listeners\n");
        builder.append(" (").append(listeners.size()).append(" listeners classes)\n");
        builder.append(" (for ").append(listenersByEvent.size()).append(" events)\n");
        builder.append(" (for a total of ")
                .append(listeners.values().stream().mapToInt(List::size).sum())
                .append(" individual listeners methods)\n");

        for (Map.Entry<Object, List<EventListenerImpl>> entry : listeners.entrySet()) {
            Object listener = entry.getKey();
            List<EventListenerImpl> eventListeners = entry.getValue();
            eventListeners.sort(Comparator.comparingInt(EventListenerImpl::priority));

            builder.append('\n')
                    .append(listener)
                    .append(" (")
                    .append(listener.getClass().getName())
                    .append(") [")
                    .append(eventListeners.size())
                    .append("]\n");
            for (EventListenerImpl eventListener : eventListeners) {
                builder.append(" - ")
                        .append(eventListener.eventClass().getName())
                        .append(": ")
                        .append(eventListener.methodName())
                        .append(" @ ")
                        .append(eventListener.priority())
                        .append('\n');
            }
        }

        event.addFile(new TextDebugFile("event-bus.txt", builder));
    }

    private static class State<T> {

        private final Class<T> eventClass;
        private final Predicate<Object> statePredicate;
        private final ThreadLocal<EventListener> stateHolder;

        @SuppressWarnings("unchecked") // Converting generic to Object is easier down the line
        public State(Class<T> eventClass, Predicate<T> statePredicate, ThreadLocal<EventListener> stateHolder) {
            this.eventClass = eventClass;
            this.statePredicate = (Predicate<Object>) statePredicate;
            this.stateHolder = stateHolder;
        }

        public Class<T> eventClass() {
            return eventClass;
        }

        public Predicate<Object> statePredicate() {
            return statePredicate;
        }

        public ThreadLocal<EventListener> stateHolder() {
            return stateHolder;
        }
    }
}
