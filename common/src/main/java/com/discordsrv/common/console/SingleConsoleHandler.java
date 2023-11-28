package com.discordsrv.common.console;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordGuildChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.events.message.DiscordMessageReceiveEvent;
import com.discordsrv.api.discord.util.DiscordFormattingUtil;
import com.discordsrv.api.event.bus.Subscribe;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The log appending and command handling for a single console channel.
 */
public class SingleConsoleHandler {

    private static final int MESSAGE_MAX_LENGTH = Message.MAX_CONTENT_LENGTH;

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final ConsoleConfig config;
    private final Queue<LogEntry> queue;
    private Future<?> queueProcessingFuture;
    private boolean shutdown = false;

    // Editing
    private final List<LogMessage> messageCache;
    private Long mostRecentMessageId;

    // Preventing concurrent sends
    private final Object sendLock = new Object();
    private CompletableFuture<?> sendFuture;

    // Don't annoy console users twice about using /
    private final Set<Long> warnedSlashUsageUserIds = new HashSet<>();

    public SingleConsoleHandler(DiscordSRV discordSRV, Logger logger, ConsoleConfig config) {
        this.discordSRV = discordSRV;
        this.logger = logger;
        this.config = config;
        this.queue = config.appender.outputMode != ConsoleConfig.OutputMode.OFF ? new LinkedBlockingQueue<>() : null;
        this.messageCache = config.appender.useEditing ? new ArrayList<>() : null;

        timeQueueProcess();
        discordSRV.eventBus().subscribe(this);
    }

    @Subscribe
    public void onDiscordMessageReceived(DiscordMessageReceiveEvent event) {
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
        String threadName = destination.threadName;

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
        mostRecentMessageId = null;

        // Run the command
        discordSRV.console().runCommandWithLogging(discordSRV, user, command);
    }

    public void queue(LogEntry entry) {
        if (queue == null) {
            return;
        }

        queue.offer(entry);
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void shutdown() {
        shutdown = true;
        discordSRV.eventBus().unsubscribe(this);
        queueProcessingFuture.cancel(false);
        try {
            synchronized (queueProcessingFuture) {
                queueProcessingFuture.wait(TimeUnit.SECONDS.toMillis(3));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        queue.clear();
        if (messageCache != null) {
            messageCache.clear();
        }
        mostRecentMessageId = null;
    }

    private void timeQueueProcess() {
        if (shutdown) {
            return;
        }
        if (config.appender.outputMode == ConsoleConfig.OutputMode.OFF) {
            return;
        }
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
                            // Nowhere to send to
                            return null;
                        }

                        DiscordGuildMessageChannel channel = channels.get(0);
                        if (mostRecentMessageId != null) {
                            long messageId = mostRecentMessageId;
                            if (isFull) {
                                mostRecentMessageId = null;
                            }
                            return channel.editMessageById(messageId, sendableMessage);
                        }

                        return channel.sendMessage(sendableMessage)
                                .whenComplete((receivedMessage, t) -> {
                                    if (receivedMessage != null && messageCache != null) {
                                        mostRecentMessageId = receivedMessage.getId();
                                    }
                                });
                    }).exceptionally(ex -> {
                        String error = "Failed to send message to console channel";
                        if (message.contains(error)) {
                            // Prevent infinite loop of the same error
                            return null;
                        }
                        logger.error(error, ex);
                        return null;
                    });
        }
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
}
