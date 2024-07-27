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
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.Event;
import com.discordsrv.common.MockDiscordSRV;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventBusTest {

    private static final Listener listener = new Listener();
    private static final EventBus eventBus = MockDiscordSRV.INSTANCE.eventBus();

    @BeforeAll
    public static void subscribe() {
        eventBus.subscribe(listener);
    }

    @AfterAll
    public static void unsubscribe() {
        eventBus.unsubscribe(listener);
    }

    @Test
    public void publishTest() {
        eventBus.publish(new Event() {});
        assertTrue(listener.reached);
    }

    public static class Listener {

        public boolean reached = false;

        @Subscribe
        public void onEvent(Event event) {
            reached = true;
        }
    }
}
