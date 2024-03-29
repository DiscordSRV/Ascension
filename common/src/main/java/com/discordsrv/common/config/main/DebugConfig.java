package com.discordsrv.common.config.main;

import com.discordsrv.common.config.configurate.annotation.Constants;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class DebugConfig {

    @Comment("If debug messages should be logged into the config")
    public boolean logToConsole = false;

    @Comment("Additional levels to log\nExample value: %1")
    @Constants.Comment("{\"AWARD_LISTENER\":[\"TRACE\"]}")
    public Map<String, List<String>> additionalLevels = new HashMap<>();

}
