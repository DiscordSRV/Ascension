package com.discordsrv.api.discord.api.entity.guild;

import java.util.Optional;

/**
 * A Discord server.
 */
public interface DiscordGuild {

    /**
     * Gets the id of this Discord guild.
     * @return the guild's id
     */
    String getId();

    /**
     * Gets the member count of the guild.
     * @return the guild's member count
     */
    int getMemberCount();

    /**
     * Gets a Discord guild member by id from the cache.
     * @param id the id for the Discord guild member
     * @return the Discord guild member from the cache
     */
    Optional<DiscordGuildMember> getMemberById(String id);
}
