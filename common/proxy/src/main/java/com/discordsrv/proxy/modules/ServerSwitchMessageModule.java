package com.discordsrv.proxy.modules;

import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.forward.game.ServerSwitchMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.ServerSwitchMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.module.modules.message.AbstractGameMessageModule;
import com.discordsrv.proxy.config.channels.ServerSwitchMessageConfig;
import com.discordsrv.proxy.config.channels.base.ProxyBaseChannelConfig;

public class ServerSwitchMessageModule extends AbstractGameMessageModule<ServerSwitchMessageConfig> {

    public ServerSwitchMessageModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onServerSwitchReceive(ServerSwitchMessageReceiveEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        process(event, event.getPlayer(), null);
        event.markAsProcessed();
    }

    @Override
    public OrDefault<ServerSwitchMessageConfig> mapConfig(OrDefault<BaseChannelConfig> channelConfig) {
        return channelConfig.map(cfg -> ((ProxyBaseChannelConfig) cfg).serverSwitchMessages);
    }

    @Override
    public boolean isEnabled(OrDefault<ServerSwitchMessageConfig> config) {
        return config.get(cfg -> cfg.enabled, false);
    }

    @Override
    public SendableDiscordMessage.Builder getFormat(OrDefault<ServerSwitchMessageConfig> config) {
        return config.get(cfg -> cfg.format);
    }

    @Override
    public void postClusterToEventBus(ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new ServerSwitchMessageForwardedEvent(cluster));
    }
}