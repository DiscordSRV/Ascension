package com.discordsrv.proxy.config.channels.base;

import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

public class ProxyChannelConfig extends ProxyBaseChannelConfig implements IChannelConfig {

    public ProxyChannelConfig() {
        initialize();
    }

    @Comment(CHANNEL_IDS_COMMENT)
    public List<Long> channelIds = new ArrayList<>();

    @Override
    public List<Long> ids() {
        return channelIds;
    }
}
