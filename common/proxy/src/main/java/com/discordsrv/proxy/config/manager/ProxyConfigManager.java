package com.discordsrv.proxy.config.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.proxy.config.channels.base.ProxyBaseChannelConfig;
import com.discordsrv.proxy.config.channels.base.ProxyChannelConfig;
import org.spongepowered.configurate.objectmapping.ObjectMapper;

public abstract class ProxyConfigManager<T extends MainConfig> extends MainConfigManager<T> {

    public ProxyConfigManager(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public ChannelConfig.Serializer getChannelConfigSerializer(ObjectMapper.Factory mapperFactory) {
        return new ChannelConfig.Serializer(mapperFactory, ProxyBaseChannelConfig.class, ProxyChannelConfig.class);
    }
}
