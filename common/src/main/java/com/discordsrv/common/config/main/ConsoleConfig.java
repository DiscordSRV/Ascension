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

package com.discordsrv.common.config.main;

import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.config.main.generic.GameCommandExecutionConditionConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class ConsoleConfig {

    @Comment("The console channel or thread")
    public DestinationConfig.Single channel = new DestinationConfig.Single("DiscordSRV Console #%date:'w'%", true);

    @Comment("The amount of threads to keep. Rotation interval is based on placeholders in the thread name")
    public int threadsToKeepInRotation = 3;

    public Appender appender = new Appender();

    public Execution commandExecution = new Execution();

    @ConfigSerializable
    public static class Appender {

        @Comment("The format for log lines")
        public String lineFormat = "[%log_time:'ccc HH:mm:ss zzz'%] [%log_level%] [%logger_name%] %message%";

        @Comment("The mode for the console output, available options are:\n"
                + "- off: Turn off console appending\n"
                + "- ansi: A colored ansi code block\n"
                + "- log: A \"accesslog\" code block\n"
                + "- diff: A \"diff\" code block highlighting warnings and errors with different colors\n"
                + "- markdown: Plain text with bold, italics, strikethrough and underlining\n"
                + "- plain: Plain text code block\n"
                + "- plain_content: Plain text")
        public OutputMode outputMode = OutputMode.ANSI;

        @Comment("In \"diff\" mode, should exception lines have the prefix character as well")
        public boolean diffExceptions = true;

        @Comment("If urls should have embeds disabled")
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

    public enum OutputMode {
        OFF(null, null),
        ANSI("```ansi\n", "```"),
        LOG("```accesslog\n", "```"),
        DIFF("```diff\n", "```"),
        MARKDOWN("", ""),
        PLAIN("```\n", "```"),
        PLAIN_CONTENT("", "");

        private final String prefix;
        private final String suffix;

        OutputMode(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String prefix() {
            return prefix;
        }

        public String suffix() {
            return suffix;
        }

        public int blockLength() {
            return prefix().length() + suffix().length();
        }
    }
}
