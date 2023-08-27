package com.discordsrv.common.console;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.ConsoleConfig;
import com.discordsrv.common.console.entry.LogEntry;
import com.discordsrv.common.console.entry.LogMessage;
import com.discordsrv.common.logging.LogLevel;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class SingleConsoleHandler {

    private static final int MESSAGE_MAX_LENGTH = Message.MAX_CONTENT_LENGTH;

    private final DiscordSRV discordSRV;
    private final ConsoleConfig config;
    private final Queue<LogEntry> queue = new LinkedBlockingQueue<>();
    private final List<LogMessage> messageCache;

    public SingleConsoleHandler(DiscordSRV discordSRV, ConsoleConfig config) {
        this.discordSRV = discordSRV;
        this.config = config;
        this.messageCache = config.useEditing ? new ArrayList<>() : null;
    }

    public void queue(LogEntry entry) {
        queue.offer(entry);
    }

    public void shutdown() {

    }

    private void processQueue() {
        ConsoleConfig.OutputMode outputMode = config.getOutputMode();


    }

    private List<String> formatEntry(LogEntry entry) {
        String message = entry.message();
        String throwable = ExceptionUtils.getMessage(entry.throwable());

        message = discordSRV.placeholderService().replacePlaceholders(config.lineFormat, entry);

        ConsoleConfig.OutputMode outputMode = config.getOutputMode();

        String prefix = outputMode.prefix();
        String suffix = outputMode.suffix();
        int blockLength = prefix.length() + suffix.length();
        int maximumPart = MESSAGE_MAX_LENGTH - blockLength - "\n".length();

        if (outputMode == ConsoleConfig.OutputMode.DIFF) {
            message = getLogLevelDiffCharacter(entry.level()) + message;
            // TODO: also format throwable?
        }

        message += "\n";
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
