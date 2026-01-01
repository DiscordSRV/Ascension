/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.*;
import com.discordsrv.api.discord.entity.guild.DiscordCustomEmoji;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A basic Discord API wrapper for a limited amount of functions, with a minimal amount of breaking changes.
 */
public interface DiscordAPI {

    /**
     * Gets the {@link DiscordUser} instance for the bot itself.
     * @return the self user
     */
    @NotNull
    DiscordUser getSelfUser();

    /**
     * Gets a Discord channel by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the channel
     * @return the channel
     */
    DiscordChannel getChannelById(long id);

    /**
     * Gets a Discord category by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the category
     * @return the category
     */
    @Nullable
    DiscordCategory getCategoryById(long id);

    /**
     * Gets a Discord message channel by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the message channel
     * @return the message channel
     */
    @Nullable
    DiscordMessageChannel getMessageChannelById(long id);

    /**
     * Gets a Discord direct message channel by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the direct message channel
     * @return the direct message channel
     */
    @Nullable
    DiscordDMChannel getDirectMessageChannelById(long id);

    /**
     * Gets a Discord news channel by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the news channel
     * @return the news channel
     */
    @Nullable
    DiscordNewsChannel getNewsChannelById(long id);

    /**
     * Gets a Discord text channel by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the text channel
     * @return the text channel
     */
    @Nullable
    DiscordTextChannel getTextChannelById(long id);

    /**
     * Gets a Discord forum channel by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the text channel
     * @return the forum channel
     */
    @Nullable
    DiscordForumChannel getForumChannelById(long id);

    /**
     * Gets a Discord media channel by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the text channel
     * @return the media channel
     */
    @Nullable
    DiscordMediaChannel getMediaChannelById(long id);

    /**
     * Gets a Discord voice channel by id, the provided entity should be stored for long periods of time.
     * @param id the id for the voice channel
     * @return the voice channel
     */
    @Nullable
    DiscordVoiceChannel getVoiceChannelById(long id);

    /**
     * Gets a Discord stage channel by id, the provided entity should be stored for long periods of time.
     * @param id the id for the voice channel
     * @return the voice channel
     */
    @Nullable
    DiscordStageChannel getStageChannelById(long id);

    /**
     * Gets a Discord thread channel by id from the cache, the provided entity should not be stored for long periods of time.
     * @param id the id for the thread channel
     * @return the thread channel
     */
    @Nullable
    DiscordThreadChannel getCachedThreadChannelById(long id);

    /**
     * Gets a Discord server by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the Discord server
     * @return the Discord server
     */
    @Nullable
    DiscordGuild getGuildById(long id);

    /**
     * Gets a Discord user by id, the provided entity should not be stored for long periods of time.
     * This will always return {@code null} if {@link #isUserCachingEnabled()} returns {@code false}.
     * @param id the id for the Discord user
     * @return the Discord user
     * @see #isUserCachingEnabled()
     */
    @Nullable
    DiscordUser getUserById(long id);

    /**
     * Looks up a Discord user by id from Discord, the provided entity should not be stored for long periods of time.
     * @param id the id for the Discord user
     * @return a future that will result in a {@link DiscordUser} for the id or throw a
     */
    @NotNull
    Task<DiscordUser> retrieveUserById(long id);

    /**
     * Gets if user caching is enabled.
     * @return {@code true} if user caching is enabled.
     */
    boolean isUserCachingEnabled();

    /**
     * Gets a Discord role by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the Discord role
     * @return the Discord role
     */
    @Nullable
    DiscordRole getRoleById(long id);

    /**
     * Gets a custom emoji for a Discord server by id, the provided entity should not be stored for long periods of time.
     * @param id the id for the custom emoji
     * @return the Discord custom emoji
     */
    DiscordCustomEmoji getEmojiById(long id);

    /**
     * Registers a Discord command.
     * @param command the command to register
     */
    DiscordCommand.RegistrationResult registerCommand(DiscordCommand command);

    /**
     * Unregisters a Discord command.
     * @param command the command to unregister
     */
    void unregisterCommand(DiscordCommand command);
}
