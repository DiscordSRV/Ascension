/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.eventbus.EventBus;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.Event;
import com.discordsrv.common.MockDiscordSRV;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class EventBusTest {

    private static final EventBus eventBus = MockDiscordSRV.getInstance().eventBus();

    @Test
    public void publishTest() {
        AtomicBoolean reached = new AtomicBoolean(false);
        ListenerEarly listener = new ListenerEarly(() -> reached.set(true));

        // Not subscribed, should not reach
        assertFalse(reached.get());

        // Subscribe & publish, should reach
        eventBus.subscribe(listener);
        eventBus.publish(new Event() {});
        assertTrue(reached.get());

        // Unsubscribe & publish, should not reach
        eventBus.unsubscribe(listener);
        reached.set(false);

        eventBus.publish(new Event() {});
        assertFalse(reached.get());
    }

    @Test
    public void orderTest() {
        AtomicBoolean earlyReached = new AtomicBoolean(false);

        ListenerEarly early = new ListenerEarly(() -> earlyReached.set(true));
        ListenerLate late = new ListenerLate(() -> {
            if (!earlyReached.get()) {
                fail("Reached late before early");
            }
        });

        eventBus.subscribe(early);
        eventBus.subscribe(late);
        eventBus.publish(new Event() {});
    }

    @SuppressWarnings("InstantiationOfUtilityClass") // Yeah, that's the problem :)
    @Test
    public void invalidTest() {
        assertThrows(IllegalArgumentException.class, () -> eventBus.subscribe(new InvalidListener1()));
        assertThrows(IllegalArgumentException.class, () -> eventBus.subscribe(new InvalidListener2()));
        assertThrows(IllegalArgumentException.class, () -> eventBus.subscribe(new InvalidListener3()));
        assertThrows(IllegalArgumentException.class, () -> eventBus.subscribe(new InvalidListener4()));
        assertThrows(IllegalArgumentException.class, () -> eventBus.subscribe(new InvalidListener5()));
        assertThrows(IllegalArgumentException.class, () -> eventBus.subscribe(new InvalidListener6()));
    }

    @Test
    public void jdaTest() {
        AtomicBoolean reached = new AtomicBoolean(false);
        eventBus.subscribe(new ListenerJDA(() -> reached.set(true)));
        eventBus.publish(new GenericEvent() {
            @SuppressWarnings("DataFlowIssue") // Won't be used
            @Override
            public @NotNull JDA getJDA() {
                return null;
            }

            @Override
            public long getResponseNumber() {
                return 0;
            }

            @Override
            public DataObject getRawData() {
                return null;
            }
        });

        assertTrue(reached.get());
    }

    public static class ListenerEarly {

        private final Runnable runnable;

        public ListenerEarly(Runnable runnable) {
            this.runnable = runnable;
        }

        @Subscribe(priority = EventPriorities.EARLY)
        public void onEvent(Event event) {
            runnable.run();
        }
    }

    public static class ListenerLate {

        private final Runnable runnable;

        public ListenerLate(Runnable runnable) {
            this.runnable = runnable;
        }

        @Subscribe(priority = EventPriorities.LATE)
        public void onEvent(Event event) {
            runnable.run();
        }
    }

    public static class ListenerJDA {

        private final Runnable runnable;

        public ListenerJDA(Runnable runnable) {
            this.runnable = runnable;
        }

        @Subscribe
        public void onJDAEvent(GenericEvent event) {
            runnable.run();
        }
    }

    public static class InvalidListener1 {
        @Subscribe
        public void noParameter() {}
    }

    public static class InvalidListener2 {
        @Subscribe
        public void invalidParameter(Object notAnEvent) {}
    }

    public static class InvalidListener3 {
        @Subscribe
        public void tooManyParameters(Event event, Object anotherParameter) {}
    }

    public static class InvalidListener4 {
        @Subscribe
        public static void staticMethod(Event event) {}
    }

    public static class InvalidListener5 {
        @Subscribe
        private void privateMethod(Event event) {}
    }

    public static class InvalidListener6 {
        @Subscribe
        private Object notReturnVoid(Event event) {
            return null;
        }
    }
}
