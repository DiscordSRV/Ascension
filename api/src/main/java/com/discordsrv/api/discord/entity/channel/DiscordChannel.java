package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.discord.entity.Snowflake;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;

@PlaceholderPrefix("channel_")
public interface DiscordChannel extends Snowflake {

    /**
     * Returns the type of channel this is.
     * @return the type of the channel
     */
    DiscordChannelType getType();
}
