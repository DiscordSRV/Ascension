package com.discordsrv.common.event.bus;

import com.discordsrv.api.event.bus.EventBus;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.Event;
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
