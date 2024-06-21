package com.discordsrv.common.destination;

import com.discordsrv.api.discord.entity.channel.*;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.config.main.generic.ThreadConfig;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
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
                    future = unarchiveThread(existingThread);
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
                            return unarchiveThread(archivedThread);
                        }

                        // Create thread
                        return createThread(threadContainer, threadConfig, logFailures);
                    }).exceptionally(t -> {
                        if (logFailures) {
                            logger.error("Failed to lookup threads in channel ID " + Long.toUnsignedString(channelId), t);
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

        //noinspection rawtypes
        return CompletableFuture.allOf(
                futures.stream()
                        .map(future -> (CompletableFuture) future)
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
        CompletableFuture<DiscordThreadChannel> future;
        if (threadContainer instanceof DiscordForumChannel) {
            future = ((DiscordForumChannel) threadContainer).createPost(
                    threadConfig.threadName,
                    SendableDiscordMessage.builder().setContent("\u200B").build() // zero-width-space
            );
        } else {
            future = threadContainer.createThread(threadConfig.threadName, threadConfig.privateThread);
        }
        return future.exceptionally(t -> {
            if (logFailures) {
                logger.error("Failed to create thread \"" + threadConfig.threadName + "\" "
                                     + "in channel ID " + Long.toUnsignedString(threadContainer.getId()), t);
            }
            return null;
        });
    }

    private CompletableFuture<DiscordThreadChannel> unarchiveThread(DiscordThreadChannel channel) {
        return discordSRV.discordAPI().mapExceptions(
                channel.asJDA().getManager()
                        .setArchived(false)
                        .reason("DiscordSRV destination lookup")
                        .submit()
        ).thenApply(v -> channel).exceptionally(t -> {
            logger.error("Failed to unarchive thread \"" + channel.getName() + "\" "
                                 + "in channel ID " + Long.toUnsignedString(channel.getParentChannel().getId()), t);
            return null;
        });
    }
}
