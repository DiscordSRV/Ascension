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

package com.discordsrv.common.destination;

import com.discordsrv.api.discord.entity.channel.*;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.config.main.generic.ThreadConfig;
import com.discordsrv.common.discord.util.DiscordPermissionUtil;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DestinationLookupHelper {

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final Map<String, CompletableFuture<DiscordThreadChannel>> threadActions = new HashMap<>();

    public DestinationLookupHelper(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "DESTINATION_LOOKUP");
    }

    public CompletableFuture<List<DiscordGuildMessageChannel>> lookupDestination(
            DestinationConfig config,
            boolean allowRequests,
            boolean logFailures
    ) {
        List<CompletableFuture<? extends DiscordGuildMessageChannel>> futures = new ArrayList<>();

        for (Long channelId : config.channelIds) {
            if (channelId == 0) {
                continue;
            }

            DiscordMessageChannel channel = discordSRV.discordAPI().getMessageChannelById(channelId);
            if (channel == null) {
                if (logFailures) {
                    logger.error("Channel with ID " + Long.toUnsignedString(channelId) + " not found");
                }
                continue;
            }
            if (!(channel instanceof DiscordGuildMessageChannel)) {
                continue;
            }
            futures.add(CompletableFuture.completedFuture((DiscordGuildMessageChannel) channel));
        }

        for (ThreadConfig threadConfig : config.threads) {
            long channelId = threadConfig.channelId;
            if (channelId == 0 || StringUtils.isEmpty(threadConfig.threadName)) {
                continue;
            }

            DiscordThreadContainer threadContainer;
            DiscordMessageChannel messageChannel = discordSRV.discordAPI().getMessageChannelById(channelId);
            if (messageChannel instanceof DiscordThreadContainer) {
                threadContainer = (DiscordThreadContainer) messageChannel;
            } else {
                threadContainer = discordSRV.discordAPI().getForumChannelById(channelId);
            }
            if (threadContainer == null) {
                if (logFailures) {
                    logger.error("Channel with ID " + Long.toUnsignedString(channelId) + " not found");
                }
                continue;
            }

            DiscordThreadChannel existingThread = findThread(threadContainer.getActiveThreads(), threadConfig);
            if (existingThread != null && !existingThread.isArchived()) {
                futures.add(CompletableFuture.completedFuture(existingThread));
                continue;
            }

            if (!allowRequests) {
                continue;
            }

            String threadKey = Long.toUnsignedString(channelId) + ":" + threadConfig.threadName + "/" + threadConfig.privateThread;

            CompletableFuture<DiscordThreadChannel> future;
            synchronized (threadActions) {
                CompletableFuture<DiscordThreadChannel> existingFuture = threadActions.get(threadKey);

                if (existingFuture != null) {
                    future = existingFuture;
                } else if (!threadConfig.unarchiveExisting) {
                    // Unarchiving not allowed, create new
                    future = createThread(threadContainer, threadConfig, logFailures);
                } else if (existingThread != null) {
                    // Unarchive existing thread
                    future = unarchiveThread(existingThread, logFailures);
                } else {
                    // Lookup threads
                    CompletableFuture<List<DiscordThreadChannel>> threads =
                            threadConfig.privateThread
                            ? threadContainer.retrieveArchivedPrivateThreads()
                            : threadContainer.retrieveArchivedPublicThreads();

                    future = threads.thenCompose(archivedThreads -> {
                        DiscordThreadChannel archivedThread = findThread(archivedThreads, threadConfig);
                        if (archivedThread != null) {
                            // Unarchive existing thread
                            return unarchiveThread(archivedThread, logFailures);
                        }

                        // Create thread
                        return createThread(threadContainer, threadConfig, logFailures);
                    }).exceptionally(t -> {
                        if (logFailures) {
                            logger.error("Failed to lookup threads in channel #" + threadContainer.getName(), t);
                        }
                        return null;
                    });
                }

                if (future != existingFuture) {
                    threadActions.put(threadKey, future);
                    future.whenComplete((v, t) -> threadActions.remove(threadKey));
                }
            }
            futures.add(future);
        }

        return CompletableFuture.allOf(
                futures.stream()
                        .map(future -> (CompletableFuture<?>) future)
                        .toArray(CompletableFuture[]::new)
        ).thenApply(v -> {
            Set<Long> idsDuplicateCheck = new HashSet<>();
            List<DiscordGuildMessageChannel> channels = new ArrayList<>();

            for (CompletableFuture<? extends DiscordGuildMessageChannel> future : futures) {
                DiscordGuildMessageChannel channel = future.join();
                if (channel != null && idsDuplicateCheck.add(channel.getId())) {
                    channels.add(channel);
                }
            }
            return channels;
        });
    }

    private DiscordThreadChannel findThread(Collection<DiscordThreadChannel> threads, ThreadConfig config) {
        for (DiscordThreadChannel thread : threads) {
            if (thread.getName().equals(config.threadName)) {
                return thread;
            }
        }
        return null;
    }

    private CompletableFuture<DiscordThreadChannel> createThread(
            DiscordThreadContainer threadContainer,
            ThreadConfig threadConfig,
            boolean logFailures
    ) {
        boolean forum = threadContainer instanceof DiscordForumChannel;
        boolean privateThread = !forum && threadConfig.privateThread;

        IThreadContainer container = threadContainer.getAsJDAThreadContainer();
        String missingPermissions = DiscordPermissionUtil.missingPermissionsString(
                container,
                Permission.VIEW_CHANNEL,
                privateThread ? Permission.CREATE_PRIVATE_THREADS : Permission.CREATE_PUBLIC_THREADS
        );
        if (missingPermissions != null) {
            if (logFailures) {
                logger.error("Failed to create thread \"" + threadConfig.threadName + "\" "
                                     + "in channel #" + threadContainer.getName() + ": " + missingPermissions);
            }
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<DiscordThreadChannel> future;
        if (forum) {
            future = ((DiscordForumChannel) threadContainer).createPost(
                    threadConfig.threadName,
                    SendableDiscordMessage.builder().setContent("\u200B").build() // zero-width-space
            );
        } else {
            future = threadContainer.createThread(threadConfig.threadName, privateThread);
        }
        return future.exceptionally(t -> {
            if (logFailures) {
                logger.error("Failed to create thread \"" + threadConfig.threadName + "\" "
                                     + "in channel #" + threadContainer.getName(), t);
            }
            return null;
        });
    }

    private CompletableFuture<DiscordThreadChannel> unarchiveThread(DiscordThreadChannel channel, boolean logFailures) {
        ThreadChannel jdaChannel = channel.asJDA();

        EnumSet<Permission> requiredPermissions = EnumSet.of(Permission.VIEW_CHANNEL);
        if (jdaChannel.isLocked() || !jdaChannel.isOwner()) {
            requiredPermissions.add(Permission.MANAGE_THREADS);
        }

        String missingPermissions = DiscordPermissionUtil.missingPermissionsString(jdaChannel, requiredPermissions);
        if (missingPermissions != null) {
            if (logFailures) {
                logger.error("Cannot unarchive thread \"" + channel.getName() + "\" "
                                     + "in channel #" + channel.getParentChannel().getName() + ": " + missingPermissions);
            }
            return CompletableFuture.completedFuture(null);
        }

        return discordSRV.discordAPI().mapExceptions(
                channel.asJDA().getManager()
                        .setArchived(false)
                        .reason("DiscordSRV destination lookup")
                        .submit()
        ).thenApply(v -> channel).exceptionally(t -> {
            if (logFailures) {
                logger.error("Failed to unarchive thread \"" + channel.getName() + "\" "
                                     + "in channel #" + channel.getParentChannel().getName(), t);
            }
            return null;
        });
    }
}
