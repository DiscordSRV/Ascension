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

package com.discordsrv.common.feature.linking;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.events.linking.AccountLinkedEvent;
import com.discordsrv.api.events.linking.AccountUnlinkedEvent;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.common.util.TaskUtil;
import com.github.benmanes.caffeine.cache.Cache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public class LinkingModule extends AbstractModule<DiscordSRV> {

    private final Cache<Object, Boolean> linkCheckRateLimit;
    private final Map<UUID, Semaphore> playerLinkingLock = new HashMap<>();
    private final Map<Long, Semaphore> userLinkingLock = new HashMap<>();

    public LinkingModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "LINKING"));
        this.linkCheckRateLimit = discordSRV.caffeineBuilder()
                .expireAfterWrite(LinkStore.LINKING_CODE_RATE_LIMIT)
                .build();
    }

    public boolean rateLimit(Object identifier) {
        synchronized (linkCheckRateLimit) {
            boolean rateLimited = linkCheckRateLimit.getIfPresent(identifier) != null;
            if (!rateLimited) {
                linkCheckRateLimit.put(identifier, true);
            }
            return rateLimited;
        }
    }

    private LinkStore store() {
        LinkProvider provider = discordSRV.linkProvider();
        if (provider == null) {
            throw new IllegalStateException("LinkProvider is null");
        }

        return provider.store();
    }

    private <T> T locking(UUID playerUUID, long userId, Supplier<T> operation) {
        Semaphore playerSemaphore = null, userSemaphore = null;

        try {
            synchronized (playerLinkingLock) {
                playerSemaphore = playerLinkingLock.computeIfAbsent(playerUUID, k -> new Semaphore(1));
            }
            playerSemaphore.acquire();

            try {
                synchronized (userLinkingLock) {
                    userSemaphore = userLinkingLock.computeIfAbsent(userId, k -> new Semaphore(1));
                }
                userSemaphore.acquire();

                return operation.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (userSemaphore != null) {
                    synchronized (userLinkingLock) {
                        boolean empty = !userSemaphore.hasQueuedThreads();
                        userSemaphore.release();

                        if (empty) {
                            // If there are no queued threads, cleanup the memory
                            userLinkingLock.remove(userId);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (playerSemaphore != null) {
                synchronized (playerLinkingLock) {
                    boolean empty = !playerSemaphore.hasQueuedThreads();
                    playerSemaphore.release();

                    if (empty) {
                        // If there are no queued threads, cleanup the memory
                        playerLinkingLock.remove(playerUUID);
                    }
                }
            }
        }
        return null;
    }

    public Task<Void> link(UUID playerUUID, long userId) {
        return locking(playerUUID, userId, () -> store().createLink(new AccountLink(playerUUID, userId, LocalDateTime.now(), LocalDateTime.now()))
                .whenSuccessful(v -> {
                    logger().debug("Linked: " + playerUUID + " & " + Long.toUnsignedString(userId));
                    discordSRV.eventBus().publish(new AccountLinkedEvent(playerUUID, userId));

                    IPlayer player = discordSRV.playerProvider().player(playerUUID);
                    if (player == null) {
                        return;
                    }

                    Task<DiscordUser> userFuture = TaskUtil.timeout(
                            discordSRV,
                            discordSRV.discordAPI().retrieveUserById(userId),
                            Duration.ofSeconds(5)
                    );

                    userFuture.whenComplete((user, __) -> {
                        MinecraftComponent linkedMessage = discordSRV.messagesConfig(player).nowLinked1st.minecraft().textBuilder()
                                .addContext(player, user)
                                .addPlaceholder("user_id", userId)
                                .addPlaceholder("player_uuid", player)
                                .applyPlaceholderService()
                                .build();
                        player.sendMessage(ComponentUtil.fromAPI(linkedMessage));
                    });
                })
                .whenFailed(t -> logger().error("Failed to link " + playerUUID + " and " + Long.toUnsignedString(userId), t)));
    }

    public Task<Void> unlink(UUID playerUUID, long userId) {
        return locking(playerUUID, userId, () -> store().removeLink(playerUUID, userId)
                .whenComplete((v, t) -> {
                    logger().debug("Unlinked: " + playerUUID + " & " + Long.toUnsignedString(userId));
                    discordSRV.eventBus().publish(new AccountUnlinkedEvent(playerUUID, userId));
                })
                .whenFailed(t -> logger().error("Failed to unlink " + playerUUID + " and " + Long.toUnsignedString(userId), t)));
    }
}
