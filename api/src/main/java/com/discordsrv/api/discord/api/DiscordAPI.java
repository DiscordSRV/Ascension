/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.api;

import com.discordsrv.api.discord.api.entity.channel.DiscordDMChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.api.entity.user.DiscordUser;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A basic Discord API wrapper for a limited amount of functions, with a minimal amount of breaking changes.
 */
public interface DiscordAPI {

    /**
     * Gets a Discord message channel by id, the provided entity can be cached and will not update if it changes on Discord.
     * @param id the id for the message channel
     * @return the message channel
     */
    @NotNull
    Optional<? extends DiscordMessageChannel> getMessageChannelById(long id);

    /**
     * Gets a Discord direct message channel by id, the provided entity can be cached and will not update if it changes on Discord.
     * @param id the id for the direct message channel
     * @return the direct message channel
     */
    @NotNull
    Optional<DiscordDMChannel> getDirectMessageChannelById(long id);

    /**
     * Gets a Discord text channel by id, the provided entity can be cached and will not update if it changes on Discord.
     * @param id the id for the text channel
     * @return the text channel
     */
    @NotNull
    Optional<DiscordTextChannel> getTextChannelById(long id);

    /**
     * Gets a Discord server by id, the provided entity can be cached and will not update if it changes on Discord.
     * @param id the id for the Discord server
     * @return the Discord server
     */
    @NotNull
    Optional<DiscordGuild> getGuildById(long id);

    /**
     * Gets a Discord user by id, the provided entity can be cached and will not update if it changes on Discord.
     * @param id the id for the Discord user
     * @return the Discord user
     */
    @NotNull
    Optional<DiscordUser> getUserById(long id);
}
