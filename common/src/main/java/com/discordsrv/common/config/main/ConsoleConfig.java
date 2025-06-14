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

package com.discordsrv.common.config.main;

import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.config.main.generic.DiscordOutputMode;
import com.discordsrv.common.config.main.generic.GameCommandExecutionConditionConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class ConsoleConfig {

    @Comment("The channel or thread that will be used for forwarding output and/or running commands in")
    public DestinationConfig.Single channel = new DestinationConfig.Single("DiscordSRV Console #%date:'w'%", true);

    @Comment("The number of threads to keep. Rotation interval is based on placeholders in the thread name")
    public int threadsToKeepInRotation = 3;

    public Appender appender = new Appender();

    public Execution commandExecution = new Execution();

    @ConfigSerializable
    public static class Appender {

        @Comment("The mode for the console output, available options are:\n"
                + "- off: Turn off console appending\n"
                + "- ansi: A colored ansi code block\n"
                + "- log: An \"accesslog\" code block\n"
                + "- diff: A \"diff\" code block highlighting warnings and errors with different colors\n"
                + "- markdown: Plain text with bold, italics, strikethrough and underlining\n"
                + "- code_block: Plain text code block\n"
                + "- plain: Plain text")
        public DiscordOutputMode outputMode = DiscordOutputMode.ANSI;

        @Comment("How individual log lines will be formatted")
        public String lineFormat = "[%log_time:'ccc HH:mm:ss zzz'%] [%log_level%]%logger_name:' [\\%s]'% %message%";

        @Comment("In \"diff\" mode, should exception (stack trace) lines have the prefix character as well, as opposed to only the first line")
        public boolean diffExceptions = true;

        @Comment("If urls should have embeds disabled (in \"plain\" output-mode)")
        public boolean disableLinkEmbeds = true;

        @Comment("Avoids sending new messages by editing the most recent message until it reaches it's maximum length")
        public boolean useEditing = true;

        @Comment("If console messages should be silent, not causing a notification")
        public boolean silentMessages = true;

        @Comment("A list of log levels to whitelist or blacklist")
        public Levels levels = new Levels();

        public static class Levels {
            public List<String> levels = new ArrayList<>(Arrays.asList("DEBUG", "TRACE"));
            public boolean blacklist = true;
        }

        @Comment("A list of logger names to whitelist or blacklist, use \"NONE\" for log messages with no logger name")
        public Loggers loggers = new Loggers();

        public static class Loggers {
            public List<String> loggers = new ArrayList<>(Collections.singletonList("ExcludedLogger"));
            public boolean blacklist = true;
        }

        @Comment("Options for how to deal with exception stack traces in the console")
        public Exceptions exceptions = new Exceptions();

        public static class Exceptions {

            @Comment("The time after which the exact same exception will not be forwarded to the console channel, in minutes\n"
                    + "0 to not filter out duplicate exceptions, -1 to always filter out duplicate exceptions")
            public int filterOutDuplicatesMinutes = 5;

            @Comment("If the message alongside an exception should also be ignored when ignoring duplicate exceptions")
            public boolean alsoBlockMessages = true;
        }

    }

    @ConfigSerializable
    public static class Execution {

        public Execution() {
            executionConditions.add(
                    new GameCommandExecutionConditionConfig(
                            new ArrayList<>(),
                            false,
                            new ArrayList<>(Arrays.asList("list", "whitelist"))
                    )
            );
            executionConditions.add(
                    new GameCommandExecutionConditionConfig(
                            new ArrayList<>(),
                            true,
                            new ArrayList<>(Arrays.asList(
                                    "?",
                                    "op",
                                    "deop",
                                    "execute"
                            ))
                    )
            );
        }

        @Comment("If command execution in this console channel is enabled\n"
                + "Requires the \"Message Content Intent\"")
        public boolean enabled = true;

        @Comment("At least one condition has to match to allow execution")
        public List<GameCommandExecutionConditionConfig> executionConditions = new ArrayList<>();

        @Comment("If a command is inputted starting with /, a warning response will be given if this is enabled")
        public boolean enableSlashWarning = true;

    }
}
