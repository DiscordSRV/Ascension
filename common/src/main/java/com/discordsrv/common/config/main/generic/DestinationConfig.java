package com.discordsrv.common.config.main.generic;

import com.discordsrv.common.config.configurate.annotation.Constants;
import org.apache.commons.lang3.StringUtils;
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

    @ConfigSerializable
    public static class Single {
        @Setting("channel-id")
        public Long channelId = 0L;

        @Setting("thread-name")
        @Comment("If specified this destination will be a thread in the provided channel-id's channel, if left blank the destination will be the channel")
        public String threadName = "";
        public boolean privateThread = false;

        public DestinationConfig asDestination() {
            DestinationConfig config = new DestinationConfig();
            if (StringUtils.isEmpty(threadName)) {
                config.channelIds.add(channelId);
            } else {
                ThreadConfig threadConfig = new ThreadConfig();
                threadConfig.channelId = channelId;
                threadConfig.threadName = threadName;
                threadConfig.privateThread = privateThread;
                config.threads.add(threadConfig);
            }
            return config;
        }
    }
}
