package com.discordsrv.common.config.main;

import com.discordsrv.common.config.main.generic.GameCommandFilterConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@ConfigSerializable
public class DiscordCommandConfig {

    public ExecuteConfig execute = new ExecuteConfig();

    @ConfigSerializable
    public static class ExecuteConfig {

        public ExecuteConfig() {
            filters.add(
                    new GameCommandFilterConfig(
                            new ArrayList<>(),
                            false,
                            new ArrayList<>(Arrays.asList("say", "/gamemode(?: (?:survival|spectator)(?: .+)?)?/"))
                    )
            );
        }

        public boolean enabled = true;

        @Comment("If the command output should only be visible to the user who ran the command")
        public boolean ephemeral = true;

        @Comment("The mode for the command output, available options are:\n"
                + "- markdown: Regular Discord markdown\n"
                + "- ansi: A colored ansi code block\n"
                + "- plain: Plain text\n"
                + "- codeblock: Plain code block\n"
                + "- off: No command output")
        public String outputMode = "markdown";

        public OutputMode getOutputMode() {
            switch (outputMode.toLowerCase(Locale.ROOT)) {
                default:
                case "markdown": return OutputMode.MARKDOWN;
                case "ansi": return OutputMode.ANSI;
                case "plain": return OutputMode.PLAIN;
                case "codeblock": return OutputMode.PLAIN_BLOCK;
                case "off": return OutputMode.OFF;
            }
        }

        @Comment("At least one condition has to match to allow execution")
        public List<GameCommandFilterConfig> filters = new ArrayList<>();

        @Comment("If commands should be suggested while typing\n" +
                "Suggestions go through the server's main thread (on servers with a main thread) to ensure compatability.")
        public boolean suggest = true;

        @Comment("If suggestions should be filtered based on the \"filters\" option")
        public boolean filterSuggestions = true;
    }

    public enum OutputMode {
        MARKDOWN,
        ANSI,
        PLAIN,
        PLAIN_BLOCK,
        OFF
    }
}
