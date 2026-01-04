/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.common.exception.MessageException;
import com.discordsrv.common.util.DiscordPermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class DestinationLookupHelper {

    private final DiscordSRV discordSRV;
    private final Map<String, Task<DiscordThreadChannel>> threadActions = new HashMap<>();

    public DestinationLookupHelper(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public Task<LookupResult> lookupDestination(
            DestinationConfig config,
            boolean allowRequests,
            boolean failIfAnyErrors,
            Object... threadNameContext
    ) {
        List<Task<? extends DiscordGuildMessageChannel>> futures = new ArrayList<>();

        for (Long channelId : config.channelIds) {
            if (channelId == 0) {
                continue;
            }

            DiscordMessageChannel channel = discordSRV.discordAPI().getMessageChannelById(channelId);
            if (channel == null) {
                futures.add(Task.failed(new MessageException("Cannot find channel with id " + Long.toUnsignedString(channelId))));
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
                futures.add(Task.failed(new MessageException("Cannot find channel with id " + Long.toUnsignedString(channelId))));
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
                    future = createThread(threadContainer, threadName, privateThread);
                } else if (existingThread != null) {
                    // Unarchive existing thread
                    future = unarchiveThread(existingThread);
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
                            return unarchiveThread(archivedThread);
                        }

                        // Create thread
                        return createThread(threadContainer, threadName, privateThread);
                    }).mapException(t -> {
                        throw new RuntimeException("Failed to lookup threads in " + threadContainer, t);
                    });
                }

                if (future != existingFuture) {
                    threadActions.put(threadKey, future);
                    future.whenComplete((v, t) -> threadActions.remove(threadKey));
                }
            }
            futures.add(future);
        }

        List<Task<?>> tasks = new ArrayList<>(futures.size());

        List<Throwable> errors = new ArrayList<>();
        Set<Long> idDuplicateCheck = new HashSet<>();

        for (Task<? extends DiscordGuildMessageChannel> task : futures) {
            tasks.add(task.thenApply(channel -> {
                synchronized (idDuplicateCheck) {
                    return idDuplicateCheck.add(channel.getId()) ? channel : null;
                }
            }).mapException(t -> {
                synchronized (errors) {
                    errors.add(t);
                }
                return null;
            }));
        }

        return Task.allOf(tasks).thenApply(v -> {
            List<DiscordGuildMessageChannel> channels = new ArrayList<>(tasks.size());

            for (Task<? extends DiscordGuildMessageChannel> future : futures) {
                if (!failIfAnyErrors && future.isFailed()) {
                    continue;
                }

                DiscordGuildMessageChannel channel = future.join();
                if (channel != null) {
                    channels.add(channel);
                }
            }
            return new LookupResult(channels, errors);
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
            boolean privateThread
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
            return Task.failed(new MessageException(
                    "Failed to create thread \"" + threadName + "\" "
                            + "in channel " + threadContainer + ": " + missingPermissions));
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
            throw new RuntimeException("Failed to create thread \"" + threadName + "\" in channel " + threadContainer, t);
        });
    }

    private Task<DiscordThreadChannel> unarchiveThread(DiscordThreadChannel channel) {
        ThreadChannel jdaChannel = channel.asJDA();

        EnumSet<Permission> requiredPermissions = EnumSet.of(Permission.VIEW_CHANNEL);
        if (jdaChannel.isLocked() || !jdaChannel.isOwner()) {
            requiredPermissions.add(Permission.MANAGE_THREADS);
        }

        String missingPermissions = DiscordPermissionUtil.missingPermissionsString(jdaChannel, requiredPermissions);
        if (missingPermissions != null) {
            return Task.failed(new MessageException("Cannot unarchive thread " + channel + ": " + missingPermissions));
        }

        return discordSRV.discordAPI()
                .toTask(() -> channel.asJDA().getManager().setArchived(false).reason("DiscordSRV destination lookup"))
                .thenApply(v -> channel)
                .mapException(t -> {
                    throw new RuntimeException("Failed to unarchive thread " + channel, t);
                });
    }

    public static class LookupResult {

        private final List<DiscordGuildMessageChannel> channels;
        private final List<Throwable> errors;

        public LookupResult(List<DiscordGuildMessageChannel> channels, List<Throwable> errors) {
            this.channels = channels;
            this.errors = errors;
        }

        public List<DiscordGuildMessageChannel> channels() {
            return channels;
        }

        public List<Throwable> errors() {
            return errors;
        }

        public boolean anyErrors() {
            return !errors.isEmpty();
        }

        public MessageException compositeError(String message) {
            MessageException exception = new MessageException(message);
            errors().forEach(exception::addSuppressed);
            return exception;
        }
    }
}
