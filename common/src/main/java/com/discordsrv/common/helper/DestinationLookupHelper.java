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

package com.discordsrv.common.helper;

import com.discordsrv.api.discord.entity.channel.*;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.config.main.generic.ThreadConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.util.DiscordPermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class DestinationLookupHelper {

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final Map<String, Task<DiscordThreadChannel>> threadActions = new HashMap<>();

    public DestinationLookupHelper(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "DESTINATION_LOOKUP");
    }

    public Task<List<DiscordGuildMessageChannel>> lookupDestination(
            DestinationConfig config,
            boolean allowRequests,
            boolean logFailures,
            Object... threadNameContext
    ) {
        List<Task<? extends DiscordGuildMessageChannel>> futures = new ArrayList<>();

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
            futures.add(Task.completed((DiscordGuildMessageChannel) channel));
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
                DiscordForumChannel forumChannel = discordSRV.discordAPI().getForumChannelById(channelId);
                if (forumChannel != null) {
                    threadContainer = forumChannel;
                } else {
                    threadContainer = discordSRV.discordAPI().getMediaChannelById(channelId);
                }
            }
            if (threadContainer == null) {
                if (logFailures) {
                    logger.error("Channel with ID " + Long.toUnsignedString(channelId) + " not found");
                }
                continue;
            }

            String threadName = discordSRV.placeholderService().replacePlaceholders(threadConfig.threadName, threadNameContext);
            boolean privateThread = threadConfig.privateThread && !(threadContainer instanceof DiscordForumChannel);

            DiscordThreadChannel existingThread = findThread(threadContainer.getActiveThreads(), threadName, privateThread);
            if (existingThread != null && !existingThread.isArchived()) {
                futures.add(Task.completed(existingThread));
                continue;
            }

            if (!allowRequests) {
                continue;
            }

            String threadKey = Long.toUnsignedString(channelId) + ":" + threadName + "/" + privateThread;

            Task<DiscordThreadChannel> future;
            synchronized (threadActions) {
                Task<DiscordThreadChannel> existingFuture = threadActions.get(threadKey);

                if (existingFuture != null) {
                    future = existingFuture;
                } else if (!threadConfig.unarchiveExisting) {
                    // Unarchiving not allowed, create new
                    future = createThread(threadContainer, threadName, privateThread, logFailures);
                } else if (existingThread != null) {
                    // Unarchive existing thread
                    future = unarchiveThread(existingThread, logFailures);
                } else {
                    // Lookup threads
                    Task<List<DiscordThreadChannel>> threads =
                            privateThread
                            ? threadContainer.retrieveArchivedPrivateThreads()
                            : threadContainer.retrieveArchivedPublicThreads();

                    future = threads.then(archivedThreads -> {
                        DiscordThreadChannel archivedThread = findThread(archivedThreads, threadName, privateThread);
                        if (archivedThread != null) {
                            // Unarchive existing thread
                            return unarchiveThread(archivedThread, logFailures);
                        }

                        // Create thread
                        return createThread(threadContainer, threadName, privateThread, logFailures);
                    }).mapException(t -> {
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

        return Task.allOf(futures).thenApply(v -> {
            Set<Long> idsDuplicateCheck = new HashSet<>();
            List<DiscordGuildMessageChannel> channels = new ArrayList<>();

            for (Task<? extends DiscordGuildMessageChannel> future : futures) {
                DiscordGuildMessageChannel channel = future.join();
                if (channel != null && idsDuplicateCheck.add(channel.getId())) {
                    channels.add(channel);
                }
            }
            return channels;
        });
    }

    private DiscordThreadChannel findThread(Collection<DiscordThreadChannel> threads, String threadName, boolean privateThread) {
        for (DiscordThreadChannel thread : threads) {
            if (thread.getName().equals(threadName) && thread.isPublic() != privateThread) {
                return thread;
            }
        }
        return null;
    }

    private Task<DiscordThreadChannel> createThread(
            DiscordThreadContainer threadContainer,
            String threadName,
            boolean privateThread,
            boolean logFailures
    ) {
        boolean forum = threadContainer instanceof DiscordForumChannel || threadContainer instanceof DiscordMediaChannel;

        Permission createPermission;
        if (forum) {
            createPermission = Permission.MESSAGE_SEND;
        } else {
            createPermission = privateThread ? Permission.CREATE_PRIVATE_THREADS : Permission.CREATE_PUBLIC_THREADS;
        }

        IThreadContainer container = threadContainer.getAsJDAThreadContainer();
        String missingPermissions = DiscordPermissionUtil.missingPermissionsString(
                container,
                Permission.VIEW_CHANNEL,
                createPermission
        );
        if (missingPermissions != null) {
            if (logFailures) {
                logger.error("Failed to create thread \"" + threadName + "\" "
                                     + "in channel #" + threadContainer.getName() + ": " + missingPermissions);
            }
            return Task.completed(null);
        }

        Task<DiscordThreadChannel> future;
        if (forum) {
            SendableDiscordMessage message = SendableDiscordMessage.builder().setContent("\u200B").build(); // zero-width-space

            if (threadContainer instanceof DiscordForumChannel) {
                future = ((DiscordForumChannel) threadContainer).createPost(threadName, message);
            } else {
                future = ((DiscordMediaChannel) threadContainer).createPost(threadName, message);
            }
        } else {
            future = threadContainer.createThread(threadName, privateThread);
        }
        return future.mapException(t -> {
            if (logFailures) {
                logger.error("Failed to create thread \"" + threadName + "\" "
                                     + "in channel #" + threadContainer.getName(), t);
            }
            return null;
        });
    }

    private Task<DiscordThreadChannel> unarchiveThread(DiscordThreadChannel channel, boolean logFailures) {
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
            return Task.completed(null);
        }

        return discordSRV.discordAPI()
                .toTask(() -> channel.asJDA().getManager().setArchived(false).reason("DiscordSRV destination lookup"))
                .thenApply(v -> channel)
                .mapException(t -> {
                    if (logFailures) {
                        logger.error("Failed to unarchive thread \"" + channel.getName() + "\" "
                                             + "in channel #" + channel.getParentChannel().getName(), t);
                    }
                    return null;
                });
    }
}
