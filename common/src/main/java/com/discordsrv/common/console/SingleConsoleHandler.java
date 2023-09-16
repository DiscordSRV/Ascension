package com.discordsrv.common.console;

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.ConsoleConfig;
import com.discordsrv.common.console.entry.LogEntry;
import com.discordsrv.common.console.entry.LogMessage;
import com.discordsrv.common.console.message.ConsoleMessage;
import com.discordsrv.common.logging.LogLevel;
import com.discordsrv.common.logging.Logger;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SingleConsoleHandler {

    private static final int MESSAGE_MAX_LENGTH = Message.MAX_CONTENT_LENGTH;

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final ConsoleConfig config;
    private final Queue<LogEntry> queue = new LinkedBlockingQueue<>();
    private Future<?> queueProcessingFuture;

    // Editing
    private final List<LogMessage> messageCache;
    private Long mostRecentMessageId;

    // Preventing concurrent sends
    private final Object sendLock = new Object();
    private CompletableFuture<?> sendFuture;

    public SingleConsoleHandler(DiscordSRV discordSRV, Logger logger, ConsoleConfig config) {
        this.discordSRV = discordSRV;
        this.logger = logger;
        this.config = config;
        this.messageCache = config.appender.useEditing ? new ArrayList<>() : null;

        timeQueueProcess();
    }

    public void queue(LogEntry entry) {
        queue.offer(entry);
    }

    public void shutdown() {
        queueProcessingFuture.cancel(false);
        queue.clear();
        messageCache.clear();
        mostRecentMessageId = null;
    }

    private void timeQueueProcess() {
        this.queueProcessingFuture = discordSRV.scheduler().runLater(this::processQueue, 2, TimeUnit.SECONDS);
    }

    private void processQueue() {
        try {
            ConsoleConfig.Appender appenderConfig = config.appender;
            ConsoleConfig.OutputMode outputMode = appenderConfig.outputMode;

            Queue<LogMessage> currentBuffer = new LinkedBlockingQueue<>();
            LogEntry entry;
            while ((entry = queue.poll()) != null) {
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
                        send(message, true, outputMode);
                    }
                }
            }
            clearBuffer(currentBuffer, outputMode);
        } catch (Exception ex) {
            logger.error("Failed to process console lines", ex);
        }

        if (sendFuture != null) {
            sendFuture.whenComplete((__, ___) -> {
                sendFuture = null;
                timeQueueProcess();
            });
        } else {
            timeQueueProcess();
        }
    }

    private void clearBuffer(Queue<LogMessage> currentBuffer, ConsoleConfig.OutputMode outputMode) {
        if (currentBuffer.isEmpty()) {
            return;
        }

        int blockLength = outputMode.blockLength();

        StringBuilder builder = new StringBuilder();
        if (messageCache != null) {
            for (LogMessage logMessage : messageCache) {
                builder.append(logMessage.formatted());
            }
        }

        LogMessage current;
        while ((current = currentBuffer.poll()) != null) {
            String formatted = current.formatted();
            if (formatted.length() + builder.length() + blockLength > MESSAGE_MAX_LENGTH) {
                send(builder.toString(), true, outputMode);
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
            send(builder.toString(), false, outputMode);
        }
    }

    private void send(String message, boolean isFull, ConsoleConfig.OutputMode outputMode) {
        SendableDiscordMessage sendableMessage = SendableDiscordMessage.builder()
                .setContent(outputMode.prefix() + message + outputMode.suffix())
                .setSuppressedNotifications(config.appender.silentMessages)
                .setSuppressedEmbeds(config.appender.disableLinkEmbeds)
                .build();

        synchronized (sendLock) {
            CompletableFuture<?> future = sendFuture != null ? sendFuture : CompletableFuture.completedFuture(null);

            sendFuture = future
                    .thenCompose(__ ->
                           discordSRV.discordAPI()
                                   .findOrCreateDestinations(config.channel.asDestination(), true, true, true)
                    )
                    .thenApply(channels -> {
                        if (channels.isEmpty()) {
                            throw new IllegalStateException("No channel");
                        }

                        DiscordGuildMessageChannel channel = channels.get(0);
                        if (mostRecentMessageId != null) {
                            long channelId = mostRecentMessageId;
                            if (isFull) {
                                mostRecentMessageId = null;
                            }
                            return channel.editMessageById(channelId, sendableMessage);
                        }

                        return channel.sendMessage(sendableMessage)
                                .whenComplete((receivedMessage, t) -> {
                                    if (receivedMessage != null) {
                                        mostRecentMessageId = receivedMessage.getId();
                                    }
                                });
                    });
        }
    }

    private List<String> formatEntry(LogEntry entry, ConsoleConfig.OutputMode outputMode, boolean diffExceptions) {
        int blockLength = outputMode.blockLength();
        int maximumPart = MESSAGE_MAX_LENGTH - blockLength - "\n".length();

        String parsedMessage;
        ConsoleMessage consoleMessage = new ConsoleMessage(discordSRV, entry.message());
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

        String message = discordSRV.placeholderService().replacePlaceholders(
                config.appender.lineFormat,
                entry,
                new SinglePlaceholder("message", parsedMessage)
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
        message = cutToSizeIfNeeded(message, blockLength, maximumPart, formatted);

        // Handle log entry being longer than a message
        int totalLength = blockLength + throwable.length() + message.length();
        if (totalLength > MESSAGE_MAX_LENGTH) {
            StringBuilder builder = new StringBuilder(message);
            for (String line : throwable.split("\n")) {
                line += "\n";

                // Handle a single line of a throwable being longer than a message
                line = cutToSizeIfNeeded(line, blockLength, maximumPart, formatted);

                if (blockLength + line.length() > MESSAGE_MAX_LENGTH) {
                    // Need to split here
                    formatted.add(builder.toString());
                    builder.setLength(0);
                }
                builder.append(line);
            }
            formatted.add(builder.toString());
        } else {
            formatted.add(message + throwable);
        }

        return formatted;
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
}
