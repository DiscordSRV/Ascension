package com.discordsrv.bungee.config.main;

import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.PresenceUpdaterConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.proxy.ProxyBaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.proxy.ProxyChannelConfig;
import com.discordsrv.common.config.main.linking.ProxyRequiredLinkingConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class BungeeConfig extends MainConfig {

    @Override
    public BaseChannelConfig createDefaultBaseChannel() {
        return new ProxyBaseChannelConfig();
    }

    @Override
    public BaseChannelConfig createDefaultChannel() {
        return new ProxyChannelConfig();
    }

    @Comment("Options for requiring players to link (and optionally meet other requirements) before being able to play")
    @Order(410)
    public ProxyRequiredLinkingConfig requiredLinking = new ProxyRequiredLinkingConfig();

    @Override
    public PresenceUpdaterConfig defaultPresenceUpdater() {
        return new PresenceUpdaterConfig.Proxy();
    }
}
