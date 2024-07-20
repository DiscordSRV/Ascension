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

package com.discordsrv.common.console;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordGuildChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.util.DiscordFormattingUtil;
import com.discordsrv.api.event.events.discord.message.DiscordMessageReceiveEvent;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.GameCommandExecutionHelper;
import com.discordsrv.common.config.main.ConsoleConfig;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.config.main.generic.GameCommandExecutionConditionConfig;
import com.discordsrv.common.console.entry.LogEntry;
import com.discordsrv.common.console.entry.LogMessage;
import com.discordsrv.common.console.message.ConsoleMessage;
import com.discordsrv.common.logging.LogLevel;
import com.discordsrv.common.logging.Logger;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The log appending and command handling for a single console channel.
 */
public class SingleConsoleHandler {

    private static final int MESSAGE_MAX_LENGTH = Message.MAX_CONTENT_LENGTH;
    private static final int SEND_QUEUE_MAX_SIZE = 6;

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private ConsoleConfig config;
    private Queue<LogEntry> messageQueue;
    private Deque<Pair<SendableDiscordMessage, Boolean>> sendQueue;
    private Future<?> queueProcessingFuture;
    private boolean shutdown = false;

    // Editing
    private List<LogMessage> messageCache;
    private final AtomicLong mostRecentMessageId = new AtomicLong(0);

    // Sending
    private boolean sentFirstBatch = false;
    private CompletableFuture<?> sendFuture;

    // Don't annoy console users twice about using /
    private final Set<Long> warnedSlashUsageUserIds = new HashSet<>();

    public SingleConsoleHandler(DiscordSRV discordSRV, Logger logger, ConsoleConfig config) {
        this.discordSRV = discordSRV;
        this.logger = logger;
        setConfig(config);
    }

    public void handleDiscordMessageReceived(DiscordMessageReceiveEvent event) {
        if (!config.commandExecution.enabled) {
            return;
        }

        DiscordMessageChannel messageChannel = event.getChannel();
        DiscordGuildChannel channel = messageChannel instanceof DiscordGuildChannel ? (DiscordGuildChannel) messageChannel : null;
        if (channel == null) {
            return;
        }

        ReceivedDiscordMessage message = event.getMessage();
        if (message.isFromSelf()) {
            return;
        }

        String command = event.getMessage().getContent();
        if (command == null) {
            return;
        }

        DestinationConfig.Single destination = config.channel;
        String threadName = destination.thread.threadName;

        DiscordGuildChannel checkChannel;
        if (StringUtils.isNotEmpty(threadName)) {
            if (!(channel instanceof DiscordThreadChannel)) {
                return;
            }

            if (!channel.getName().equals(threadName)) {
                return;
            }

            checkChannel = ((DiscordThreadChannel) channel).getParentChannel();
        } else {
            checkChannel = channel;
        }
        if (checkChannel.getId() != destination.channelId) {
            return;
        }

        DiscordUser user = message.getAuthor();
        DiscordGuildMember member = message.getMember();
        GameCommandExecutionHelper helper = discordSRV.executeHelper();

        if (command.startsWith("/") && config.commandExecution.enableSlashWarning) {
            long userId = user.getId();

            boolean newUser;
            synchronized (warnedSlashUsageUserIds) {
                newUser = !warnedSlashUsageUserIds.contains(userId);
                if (newUser) {
                    warnedSlashUsageUserIds.add(userId);
                }
            }

            if (newUser) {
                // TODO: translation
                message.reply(
                        SendableDiscordMessage.builder()
                                .setContent("Your command was prefixed with `/`, but normally commands in the Minecraft server console should **not** begin with `/`")
                                .build()
                );
            }
        }

        boolean pass = false;
        for (GameCommandExecutionConditionConfig filter : config.commandExecution.executionConditions) {
            if (filter.isAcceptableCommand(member, user, command, false, helper)) {
                pass = true;
                break;
            }
        }
        if (!pass) {
            if (!user.isBot()) {
                // TODO: translation
                message.reply(
                        SendableDiscordMessage.builder()
                                .setContent("You are not allowed to run that command")
                                .build()
                );
            }
            return;
        }

        // Split message when editing
        if (messageCache != null) {
            messageCache.clear();
        }
        synchronized (mostRecentMessageId) {
            mostRecentMessageId.set(0);
        }

        // Run the command
        discordSRV.console().runCommandWithLogging(discordSRV, user, command);
    }

    public void queue(LogEntry entry) {
        if (messageQueue == null) {
            return;
        }

        messageQueue.offer(entry);
    }

    public ConsoleConfig getConfig() {
        return config;
    }

    public void setConfig(ConsoleConfig config) {
        if (queueProcessingFuture != null) {
            queueProcessingFuture.cancel(false);
        }

        this.config = config;

        boolean sendOn = config.appender.outputMode != ConsoleConfig.OutputMode.OFF;
        if (sendOn) {
            if (messageQueue == null) {
                this.messageQueue = new LinkedBlockingQueue<>();
                this.sendQueue = new LinkedBlockingDeque<>();
            }
        } else {
            if (messageQueue != null) {
                this.messageQueue = null;
                this.sendQueue = null;
            }
        }

        boolean edit = config.appender.useEditing;
        if (edit) {
            if (messageCache == null) {
                this.messageCache = new ArrayList<>();
            }
        } else {
            if (messageCache != null) {
                this.messageCache = null;
            }
        }

        timeQueueProcess();
    }

    @SuppressWarnings({"BusyWait"})
    public void shutdown() {
        shutdown = true;
        queueProcessingFuture.cancel(false);
        processQueue();

        try {
            long start = System.nanoTime();
            while (!sendFuture.isDone()) {
                if (System.nanoTime() - start > TimeUnit.SECONDS.toNanos(3)) {
                    break;
                }
                Thread.sleep(50);
            }
            sendFuture.cancel(true);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (messageQueue != null) {
            messageQueue.clear();
        }
        if (sendQueue != null) {
            sendQueue.clear();
        }
        if (messageCache != null) {
            messageCache.clear();
        }
        mostRecentMessageId.set(0);
    }

    private void timeQueueProcess() {
        if (shutdown) {
            return;
        }
        if (config.appender.outputMode == ConsoleConfig.OutputMode.OFF) {
            return;
        }
        this.queueProcessingFuture = discordSRV.scheduler().runLater(this::processQueue, Duration.ofMillis(1500));
    }

    private void processQueue() {
        try {
            processMessageQueue();
        } catch (Exception e) {
            logger.error("Failed to process console lines", e);
        }

        int oversize = sendQueue.size() - SEND_QUEUE_MAX_SIZE;
        if (sentFirstBatch && oversize > 0) {
            int remove = oversize + 1;
            for (int i = 0; i < remove; i++) {
                sendQueue.pollLast();
            }

            logger.warning("Skipping " + remove + " log messages because the send queue is backed up");
        }

        if (!shutdown && !discordSRV.isReady()) {
            // Not ready yet
            timeQueueProcess();
            return;
        }

        try {
            processSendQueue();
        } catch (Exception e) {
            logger.error("Failed to send console lines", e);
        }

        if (sendFuture != null) {
            sendFuture.whenComplete((v, t) -> timeQueueProcess());
        } else {
            timeQueueProcess();
        }
    }

    private void processMessageQueue() {
        ConsoleConfig.Appender appenderConfig = config.appender;
        ConsoleConfig.OutputMode outputMode = appenderConfig.outputMode;

        Queue<LogMessage> currentBuffer = new LinkedBlockingQueue<>();
        LogEntry entry;
        while ((entry = messageQueue.poll()) != null) {
            String level = entry.level().name();
            if (appenderConfig.levels.levels.contains(level) == appenderConfig.levels.blacklist) {
                // Ignored level
                continue;
            }

            String loggerName = entry.loggerName();
            if (StringUtils.isEmpty(loggerName)) loggerName = "NONE";
            if (appenderConfig.loggers.loggers.contains(loggerName) == appenderConfig.loggers.blacklist) {
                // Ignored logger
                continue;
            }

            List<String> messages = formatEntry(entry, outputMode, config.appender.diffExceptions);
            if (messages.size() == 1) {
                LogMessage message = new LogMessage(entry, messages.get(0));
                currentBuffer.add(message);
            } else {
                clearBuffer(currentBuffer, outputMode);
                for (String message : messages) {
                    queueMessage(message, true, outputMode);
                }
            }
        }
        clearBuffer(currentBuffer, outputMode);
    }

    private void clearBuffer(Queue<LogMessage> currentBuffer, ConsoleConfig.OutputMode outputMode) {
        if (currentBuffer.isEmpty()) {
            return;
        }

        int blockLength = outputMode.blockLength();

        StringBuilder builder = new StringBuilder(MESSAGE_MAX_LENGTH);
        if (messageCache != null) {
            for (LogMessage logMessage : messageCache) {
                builder.append(logMessage.formatted());
            }
        }

        LogMessage current;
        while ((current = currentBuffer.poll()) != null) {
            String formatted = current.formatted();
            if (formatted.length() + builder.length() + blockLength > MESSAGE_MAX_LENGTH) {
                queueMessage(builder.toString(), true, outputMode);
                builder.setLength(0);
                if (messageCache != null) {
                    messageCache.clear();
                }
            }

            builder.append(formatted);
            if (messageCache != null) {
                messageCache.add(current);
            }
        }

        if (builder.length() > 0) {
            queueMessage(builder.toString(), false, outputMode);
        }
    }

    private void queueMessage(String message, boolean lastEdit, ConsoleConfig.OutputMode outputMode) {
        SendableDiscordMessage sendableMessage = SendableDiscordMessage.builder()
                .setContent(outputMode.prefix() + message + outputMode.suffix())
                .setSuppressedNotifications(config.appender.silentMessages)
                .setSuppressedEmbeds(config.appender.disableLinkEmbeds)
                .build();

        sendQueue.offer(Pair.of(sendableMessage, lastEdit));
    }

    private List<String> formatEntry(LogEntry entry, ConsoleConfig.OutputMode outputMode, boolean diffExceptions) {
        int blockLength = outputMode.blockLength();
        int maximumPart = MESSAGE_MAX_LENGTH - blockLength - "\n".length();

        // Escape content
        String plainMessage = entry.message();
        if (outputMode != ConsoleConfig.OutputMode.MARKDOWN) {
            if (outputMode == ConsoleConfig.OutputMode.PLAIN_CONTENT) {
                plainMessage = DiscordFormattingUtil.escapeContent(plainMessage);
            } else {
                plainMessage = plainMessage.replace("``", "`\u200B`"); // zero-width-space
            }
        }

        String parsedMessage;
        ConsoleMessage consoleMessage = new ConsoleMessage(discordSRV, plainMessage);
        switch (outputMode) {
            case ANSI:
                parsedMessage = consoleMessage.asAnsi();
                break;
            case MARKDOWN:
                parsedMessage = consoleMessage.asMarkdown();
                break;
            default:
                parsedMessage = consoleMessage.asPlain();
                break;
        }

        String message = PlainPlaceholderFormat.supplyWith(
                outputMode == ConsoleConfig.OutputMode.PLAIN_CONTENT
                    ? PlainPlaceholderFormat.Formatting.DISCORD
                    : PlainPlaceholderFormat.Formatting.PLAIN,
                () ->
                        discordSRV.placeholderService().replacePlaceholders(
                                config.appender.lineFormat,
                                entry,
                                new SinglePlaceholder("message", parsedMessage)
                        )
        );

        Throwable thrown = entry.throwable();
        String throwable = thrown != null ? ExceptionUtils.getStackTrace(thrown) : StringUtils.EMPTY;

        if (outputMode == ConsoleConfig.OutputMode.DIFF) {
            String diff = getLogLevelDiffCharacter(entry.level());
            if (!message.isEmpty()) {
                message = diff + message.replace("\n", "\n" + diff);
            }

            String exceptionCharacter = diffExceptions ? diff : "";
            if (!throwable.isEmpty()) {
                throwable = exceptionCharacter + throwable.replace("\n", "\n" + exceptionCharacter);
            }
        }

        if (!message.isEmpty()) {
            message += "\n";
        }
        if (!throwable.isEmpty()) {
            throwable += "\n";
        }

        List<String> formatted = new ArrayList<>();

        // Handle message being longer than a message
        if (message.length() > MESSAGE_MAX_LENGTH) {
            message = chopOnNewlines(message, blockLength, maximumPart, formatted);
        }

        // Handle log entry being longer than a message
        int totalLength = blockLength + throwable.length() + message.length();
        if (totalLength > MESSAGE_MAX_LENGTH) {
            String remainingPart = chopOnNewlines(message, blockLength, maximumPart, formatted);
            formatted.add(remainingPart);
        } else {
            formatted.add(message + throwable);
        }

        return formatted;
    }

    private String chopOnNewlines(String input, int blockLength, int maximumPart, List<String> formatted) {
        if (!input.contains("\n")) {
            return cutToSizeIfNeeded(input, blockLength, maximumPart, formatted);
        }

        StringBuilder builder = new StringBuilder();
        for (String line : input.split("\n")) {
            line += "\n";

            line = cutToSizeIfNeeded(line, blockLength, maximumPart, formatted);

            if (blockLength + line.length() + builder.length() > MESSAGE_MAX_LENGTH) {
                formatted.add(builder.toString());
                builder.setLength(0);
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private String cutToSizeIfNeeded(
            String content,
            int blockLength,
            int maximumPart,
            List<String> formatted
    ) {
        while (content.length() + blockLength > MESSAGE_MAX_LENGTH) {
            String cutToSize = content.substring(0, maximumPart) + "\n";
            if (cutToSize.endsWith("\n\n")) {
                // maximumPart excludes the newline at the end of message/line
                cutToSize = cutToSize.substring(0, cutToSize.length() - 1);
            }

            formatted.add(cutToSize);
            content = content.substring(maximumPart);
        }
        return content;
    }

    private String getLogLevelDiffCharacter(LogLevel level) {
        if (level == LogLevel.StandardLogLevel.WARNING) {
            return "+ ";
        } else if (level == LogLevel.StandardLogLevel.ERROR) {
            return "- ";
        }
        return "  ";
    }

    private void processSendQueue() {
        Pair<SendableDiscordMessage, Boolean> pair;
        do {
            pair = sendQueue.poll();
            if (pair == null) {
                // *crickets* Nothing to send
                continue;
            }
            SendableDiscordMessage sendableMessage = pair.getKey();
            boolean lastEdit = pair.getValue();

            if (sendFuture == null) {
                sendFuture = CompletableFuture.completedFuture(null);
            }

            sendFuture = sendFuture
                    .thenCompose(__ -> discordSRV.destinations().lookupDestination(config.channel.asDestination(), true, true))
                    .thenCompose(channels -> {
                        if (channels.isEmpty()) {
                            // Nowhere to send to
                            return null;
                        }

                        DiscordGuildMessageChannel channel = channels.iterator().next();
                        synchronized (mostRecentMessageId) {
                            long messageId = mostRecentMessageId.get();
                            if (messageId != 0) {
                                if (lastEdit) {
                                    mostRecentMessageId.set(0);
                                }
                                return channel.editMessageById(messageId, sendableMessage);
                            }
                        }

                        return channel.sendMessage(sendableMessage);
                    }).thenApply(msg -> {
                        if (!lastEdit && msg != null && messageCache != null) {
                            synchronized (mostRecentMessageId) {
                                mostRecentMessageId.set(msg.getId());
                            }
                        }

                        sentFirstBatch = true;
                        return msg;
                    }).exceptionally(ex -> {
                        String error = "Failed to send message to console channel";
                        String messageContent = sendableMessage.getContent();
                        if (messageContent != null && messageContent.contains(error)) {
                            // Prevent infinite loop of the same error
                            return null;
                        }

                        logger.error(error, ex);
                        return null;
                    });
        } while (pair != null);
    }
}
