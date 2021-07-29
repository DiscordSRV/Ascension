package com.discordsrv.api.discord.api.entity.user;

import com.discordsrv.api.discord.api.entity.Snowflake;
import org.jetbrains.annotations.NotNull;

/**
 * A Discord user.
 */
public interface DiscordUser extends Snowflake {

    /**
     * Gets the username of the Discord user.
     * @return the user's username
     */
    @NotNull
    String getUsername();

    /**
     * Gets the Discord user's discriminator.
     * @return the user's discriminator
     */
    @NotNull
    String getDiscriminator();

}
