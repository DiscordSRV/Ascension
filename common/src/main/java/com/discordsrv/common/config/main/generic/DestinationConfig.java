package com.discordsrv.common.config.main.generic;

import com.discordsrv.common.config.configurate.annotation.Constants;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class DestinationConfig {

    @Setting("channel-ids")
    @Comment("The channels this in-game channel will forward to in Discord")
    public List<Long> channelIds = new ArrayList<>();

    @Setting("threads")
    @Comment("The threads that this in-game channel will forward to in Discord (this can be used instead of or with the %1 option)")
    @Constants.Comment("channel-ids")
    public List<ThreadConfig> threads = new ArrayList<>(Collections.singletonList(new ThreadConfig()));
}
