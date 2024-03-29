package com.discordsrv.common.config.main;

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.main.generic.GameCommandExecutionConditionConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigSerializable
public class DiscordCommandConfig {

    public ExecuteConfig execute = new ExecuteConfig();

    @ConfigSerializable
    public static class ExecuteConfig {

        public ExecuteConfig() {
            executionConditions.add(
                    new GameCommandExecutionConditionConfig(
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
                + "- code_block: Plain code block\n"
                + "- off: No command output")
        public OutputMode outputMode = OutputMode.MARKDOWN;

        @Comment("At least one condition has to match to allow execution")
        public List<GameCommandExecutionConditionConfig> executionConditions = new ArrayList<>();

        @Comment("If commands should be suggested while typing\n" +
                "Suggestions go through the server's main thread (on servers with a main thread) to ensure compatability.")
        public boolean suggest = true;

        @Comment("If suggestions should be filtered based on the \"%1\" option")
        @Constants.Comment("filters")
        public boolean filterSuggestions = true;
    }

    public enum OutputMode {
        MARKDOWN,
        ANSI,
        PLAIN,
        CODEBLOCK,
        OFF
    }
}
