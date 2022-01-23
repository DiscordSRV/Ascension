/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.discord.api.entity.guild;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.api.entity.DiscordUser;
import com.discordsrv.api.discord.api.entity.Mentionable;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A Discord server member.
 */
public interface DiscordGuildMember extends DiscordUser, Mentionable {

    /**
     * Gets the Discord server this member is from.
     * @return the Discord server this member is from.
     */
    @NotNull
    DiscordGuild getGuild();

    /**
     * Gets the nickname of the Discord server member.
     * @return the nickname server member
     */
    @NotNull
    Optional<String> getNickname();

    /**
     * Gets the roles of this Discord server member.
     * @return the server member's roles in order from highest to lowest, this does not include the "@everyone" role
     */
    @NotNull
    List<DiscordRole> getRoles();

    /**
     * Checks if the member has the given role.
     * @param role the role to check for
     * @return {@code true} if the member has the role
     */
    boolean hasRole(@NotNull DiscordRole role);

    /**
     * Gives the given role to this member.
     * @param role the role to give
     * @return a future
     */
    CompletableFuture<Void> addRole(@NotNull DiscordRole role);

    /**
     * Takes the given role from this member.
     * @param role the role to take
     * @return a future
     */
    CompletableFuture<Void> removeRole(@NotNull DiscordRole role);

    /**
     * Gets the effective name of this Discord server member.
     * @return the Discord server member's effective name
     */
    @Placeholder("user_effective_name")
    @NotNull
    default String getEffectiveName() {
        return getNickname().orElseGet(this::getUsername);
    }

    /**
     * Gets the avatar url that is active for this user in this server.
     * @return the user's avatar url in this server
     */
    @Placeholder("user_effective_avatar_url")
    @NotNull
    String getEffectiveServerAvatarUrl();

    /**
     * Gets the color of this user's highest role that has a color.
     * @return the color that will be used for this user
     */
    @Placeholder("user_color")
    Color getColor();

    /**
     * Returns the JDA representation of this object. This should not be used if it can be avoided.
     * @return the JDA representation of this object
     * @see DiscordSRVApi#jda()
     */
    Member getAsJDAMember();

}
