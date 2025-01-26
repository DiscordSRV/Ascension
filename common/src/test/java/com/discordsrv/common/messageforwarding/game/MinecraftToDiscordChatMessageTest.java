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

package com.discordsrv.common.messageforwarding.game;

import com.discordsrv.api.discord.entity.channel.*;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.eventbus.EventBus;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.forward.game.GameChatMessageForwardedEvent;
import com.discordsrv.api.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.FullBootExtension;
import com.discordsrv.common.MockDiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import com.discordsrv.common.helper.TestHelper;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ExtendWith(FullBootExtension.class)
public class MinecraftToDiscordChatMessageTest {

    @Test
    public void runTest() throws InterruptedException {
        DiscordSRV discordSRV = MockDiscordSRV.getInstance();
        EventBus bus = discordSRV.eventBus();

        String testMessage = UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Listener listener = new Listener(testMessage, future);

        bus.subscribe(listener);

        try {
            TestHelper.set(future::completeExceptionally);

            discordSRV.eventBus().publish(
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
                                    return discordSRV;
                                }

                                @Override
                                public @NotNull String username() {
                                    return "Vankka";
                                }

                                @Override
                                public CompletableFuture<Void> kick(Component component) {
                                    return null;
                                }

                                @Override
                                public void addChatSuggestions(Collection<String> suggestions) {}

                                @Override
                                public void removeChatSuggestions(Collection<String> suggestions) {}

                                @Override
                                public @Nullable SkinInfo skinInfo() {
                                    return null;
                                }

                                @Override
                                public @Nullable Locale locale() {
                                    return Locale.getDefault();
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
                            new GlobalChannel(discordSRV),
                            false
                    ));

            try {
                Boolean success = future.get(40, TimeUnit.SECONDS);
                if (success == null) {
                    Assertions.fail("Null amount returned by listener");
                    return;
                }

                Assertions.assertTrue(success, "Correct amount of messages received in the right channel types from listener");
            } catch (ExecutionException e) {
                Assertions.fail(e.getCause());
            } catch (TimeoutException e) {
                Assertions.fail("Failed to round trip message in 40 seconds", e);
            }
        } finally {
            TestHelper.set(null);
        }
    }

    public static class Listener {

        private final String lookFor;
        private final CompletableFuture<Boolean> success;

        public Listener(String lookFor, CompletableFuture<Boolean> success) {
            this.lookFor = lookFor;
            this.success = success;
        }

        @Subscribe
        public void onForwarded(GameChatMessageForwardedEvent event) {
            int text = 0;
            int news = 0;
            int voice = 0;
            int stage = 0;
            int textThread = 0;
            int forumThread = 0;
            int mediaThread = 0;

            for (ReceivedDiscordMessage message : event.getDiscordMessage().getMessages()) {
                String content = message.getContent();
                if (content != null && content.contains(lookFor)) {
                    DiscordMessageChannel channel = message.getChannel();
                    if (channel instanceof DiscordTextChannel) {
                        text++;
                    } else if (channel instanceof DiscordNewsChannel) {
                        news++;
                    } else if (channel instanceof DiscordVoiceChannel) {
                        voice++;
                    } else if (channel instanceof DiscordStageChannel) {
                        stage++;
                    } else if (channel instanceof DiscordThreadChannel) {
                        DiscordThreadContainer container = ((DiscordThreadChannel) channel).getParentChannel();
                        if (container instanceof DiscordTextChannel) {
                            textThread++;
                        } else if (container instanceof DiscordForumChannel) {
                            forumThread++;
                        } else if (container instanceof DiscordMediaChannel) {
                            mediaThread++;
                        }
                    }
                }
            }

            success.complete(text == 1 && news == 1 && voice == 1 && stage == 1
                                     && textThread == 1 && forumThread == 1 && mediaThread == 1);
        }
    }
}
