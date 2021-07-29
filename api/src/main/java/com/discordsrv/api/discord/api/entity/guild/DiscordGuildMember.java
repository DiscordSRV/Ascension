package com.discordsrv.api.discord.api.entity.guild;

import com.discordsrv.api.discord.api.entity.user.DiscordUser;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A Discord server member.
 */
public interface DiscordGuildMember extends DiscordUser {

    /**
     * Gets the nickname of the Discord server member.
     * @return the nickname server member
     */
    @NotNull
    Optional<String> getNickname();

    /**
     * Gets the effective name of this Discord server member.
     * @return the Discord server member's effective name
     */
    default String getEffectiveName() {
        return getNickname().orElseGet(this::getUsername);
    }

}
