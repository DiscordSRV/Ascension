package com.discordsrv.api.event.events.message.forward.game;

import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessageCluster;
import org.jetbrains.annotations.NotNull;

public class ServerSwitchMessageForwardedEvent extends AbstractGameMessageForwardedEvent {

    public ServerSwitchMessageForwardedEvent(@NotNull ReceivedDiscordMessageCluster discordMessage) {
        super(discordMessage);
    }
}
