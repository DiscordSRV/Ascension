package com.discordsrv.common.config.main;

import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Locale;

public class ConsoleConfig {

    @Comment("The format for log lines")
    public String lineFormat = "[%log_time:'ccc HH:mm:ss zzz'%] [%log_level%] [%logger_name%] %message%";

    @Comment("The mode for the console output, available options are:\n"
            + "- ansi: A colored ansi code block\n"
            + "- log: A \"accesslog\" code block\n"
            + "- diff: A \"diff\" code block highlighting warnings and errors with different colors\n"
            + "- plain: Plain text code block\n"
            + "- plain_content: Plain text")
    public String outputMode = "ansi";

    public OutputMode getOutputMode() {
        switch (outputMode.toLowerCase(Locale.ROOT)) {
            default:
            case "ansi": return OutputMode.ANSI;
            case "log": return OutputMode.LOG;
            case "diff": return OutputMode.DIFF;
            case "plain": return OutputMode.PLAIN;
            case "plain_content": return OutputMode.PLAIN_CONTENT;
        }
    }

    @Comment("Avoids sending new messages by editing the most recent message until it reaches it's maximum length")
    public boolean useEditing = true;

    @Comment("If console messages should be silent, not causing a notification")
    public boolean silentMessages = true;

    public enum OutputMode {
        ANSI("```ansi\n", "```"),
        LOG("```accesslog\n", "```"),
        DIFF("```diff\n", "```"),
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
    }
}
