package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.discord.entity.Snowflake;

public interface DiscordChannel extends Snowflake {

    /**
     * Returns the type of channel this is.
     * @return the type of the channel
     */
    DiscordChannelType getType();
}
