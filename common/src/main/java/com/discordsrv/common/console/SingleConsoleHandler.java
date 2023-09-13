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
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SingleConsoleHandler {

    private static final int MESSAGE_MAX_LENGTH = Message.MAX_CONTENT_LENGTH;

    private final DiscordSRV discordSRV;
    private final ConsoleConfig config;
    private final Future<?> queueProcessingFuture;
    private final Queue<LogEntry> queue = new LinkedBlockingQueue<>();

    // Editing
    private final List<LogMessage> messageCache;
    private Long mostRecentMessageId;

    // Preventing concurrent sends
    private final Object sendLock = new Object();
    private Future<?> sendFuture;

    public SingleConsoleHandler(DiscordSRV discordSRV, ConsoleConfig config) {
        this.discordSRV = discordSRV;
        this.config = config;
        this.messageCache = config.appender.useEditing ? new ArrayList<>() : null;

        this.queueProcessingFuture = discordSRV.scheduler().runAtFixedRate(this::processQueue, 2, TimeUnit.SECONDS);
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

    private void processQueue() {
        if (sendFuture != null && !sendFuture.isDone()) {
            // Previous send still in progress.
            return;
        }

        ConsoleConfig.Appender appenderConfig = config.appender;
        ConsoleConfig.OutputMode outputMode = appenderConfig.getOutputMode();

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

            List<String> messages = formatEntry(entry, outputMode);
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
                .build();

        synchronized (sendLock) {
            if (sendFuture != null && !sendFuture.isDone()) {
                try {
                    sendFuture.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ignored) {}
            }

            sendFuture = discordSRV.discordAPI()
                    .findOrCreateDestinations(config.channel.asDestination(), true, true, true)
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

    private List<String> formatEntry(LogEntry entry, ConsoleConfig.OutputMode outputMode) {
        int blockLength = outputMode.blockLength();
        int maximumPart = MESSAGE_MAX_LENGTH - blockLength - "\n".length();

        String parsedMessage;
        switch (outputMode) {
            case ANSI:
                parsedMessage = new ConsoleMessage(discordSRV, entry.message()).asAnsi();
                break;
            case MARKDOWN:
                parsedMessage = new ConsoleMessage(discordSRV, entry.message()).asMarkdown();
                break;
            default:
                parsedMessage = entry.message();
                break;
        }

        String message = discordSRV.placeholderService().replacePlaceholders(
                config.appender.lineFormat,
                entry,
                new SinglePlaceholder("message", parsedMessage)
        ) + "\n";

        if (outputMode == ConsoleConfig.OutputMode.DIFF) {
            message = getLogLevelDiffCharacter(entry.level()) + message;
            // TODO: also format throwable?
        }

        String throwable = ExceptionUtils.getMessage(entry.throwable());
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
            return "+";
        } else if (level == LogLevel.StandardLogLevel.ERROR) {
            return "-";
        }
        return " ";
    }
}
