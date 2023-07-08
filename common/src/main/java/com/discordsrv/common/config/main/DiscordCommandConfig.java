package com.discordsrv.common.config.main;

import com.discordsrv.common.config.main.generic.GameCommandFilterConfig;
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
            filters.add(
                    new GameCommandFilterConfig(
                            new ArrayList<>(),
                            false,
                            new ArrayList<>(Arrays.asList("say", "/gamemode(?: (?:survival|spectator)(?: .+)?)?/"))
                    )
            );
        }

        public boolean enabled = true;

        @Comment("At least one condition has to match to allow execution")
        public List<GameCommandFilterConfig> filters = new ArrayList<>();

        @Comment("If commands should be suggested while typing\n" +
                "Suggestions go through the server's main thread (on servers with a main thread) to ensure compatability.")
        public boolean suggest = true;

        @Comment("If suggestions should be filtered based on the \"filters\" option")
        public boolean filterSuggestions = true;
    }
}
