package com.discordsrv.api.discord.api.entity.guild;

import com.discordsrv.api.discord.api.entity.Snowflake;
import org.jetbrains.annotations.NotNull;

/**
 * A Discord server role.
 */
public interface DiscordRole extends Snowflake {

    /**
     * Gets the name of the Discord role.
     * @return the role name
     */
    @NotNull
    String getName();
}
