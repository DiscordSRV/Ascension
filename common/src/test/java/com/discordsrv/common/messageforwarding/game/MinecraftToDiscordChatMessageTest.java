package com.discordsrv.common.messageforwarding.game;

import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.event.bus.EventBus;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.forward.game.GameChatMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.FullBootExtension;
import com.discordsrv.common.MockDiscordSRV;
import com.discordsrv.common.channel.GlobalChannel;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.testing.TestHelper;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ExtendWith(FullBootExtension.class)
public class MinecraftToDiscordChatMessageTest {

    @Test
    public void runTest() throws InterruptedException {
        DiscordSRV discordSRV = MockDiscordSRV.INSTANCE;
        EventBus bus = discordSRV.eventBus();

        String testMessage = UUID.randomUUID().toString();
        CompletableFuture<Void> future = new CompletableFuture<>();
        Listener listener = new Listener(testMessage, future);

        bus.subscribe(listener);

        try {
            TestHelper.set(future::completeExceptionally);

            MockDiscordSRV.INSTANCE.eventBus().publish(
                    new GameChatMessageReceiveEvent(
                            null,
                            new IPlayer() {

                                @Override
                                public @NotNull Identity identity() {
                                    return Identity.identity(UUID.fromString("6c983d46-0631-48b8-9baf-5e33eb5ffec4"));
                                }

                                @Override
                                public @NotNull Audience audience() {
                                    return Audience.empty();
                                }

                                @Override
                                public DiscordSRV discordSRV() {
                                    return MockDiscordSRV.INSTANCE;
                                }

                                @Override
                                public @NotNull String username() {
                                    return "Vankka";
                                }

                                @Override
                                public @NotNull Component displayName() {
                                    return Component.text("Vankka");
                                }

                                @Override
                                public boolean hasPermission(String permission) {
                                    return true;
                                }

                                @Override
                                public void runCommand(String command) {}
                            },
                            ComponentUtil.toAPI(Component.text(testMessage)),
                            new GlobalChannel(MockDiscordSRV.INSTANCE),
                            false
                    ));
        } finally {
            TestHelper.set(null);
        }

        try {
            future.get(40, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Assertions.fail(e.getCause());
        } catch (TimeoutException e) {
            Assertions.fail("Failed to round trip message in 40 seconds", e);
        }
    }

    public static class Listener {

        private final String lookFor;
        private final CompletableFuture<Void> success;

        public Listener(String lookFor, CompletableFuture<Void> success) {
            this.lookFor = lookFor;
            this.success = success;
        }

        @Subscribe
        public void onForwarded(GameChatMessageForwardedEvent event) {
            for (ReceivedDiscordMessage message : event.getDiscordMessage().getMessages()) {
                String content = message.getContent();
                if (content != null && content.contains(lookFor)) {
                    success.complete(null);
                }
            }
        }
    }
}
