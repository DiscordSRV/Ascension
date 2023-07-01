package com.discordsrv.common.config.main.generic;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class GameCommandFilterConfig {

    @Comment("true for blacklist (blocking commands), false for whitelist (allowing commands)")
    public boolean blacklist = true;

    @Comment("The role and user ids which this set of allowed/blocked commands is for")
    public List<Long> roleAndUserIds = new ArrayList<>();

    @Comment("The commands that are allowed/blocked. Use / at the beginning and end of a value for a regular expression (regex)")
    public List<String> commands = new ArrayList<>();
}
