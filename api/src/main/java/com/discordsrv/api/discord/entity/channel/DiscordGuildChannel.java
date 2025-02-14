/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.discord.entity.Snowflake;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.task.Task;
import org.jetbrains.annotations.NotNull;

@PlaceholderPrefix("channel_")
public interface DiscordGuildChannel extends DiscordChannel, Snowflake {

    /**
     * Gets the name of the channel.
     * @return the name of the channel
     */
    @NotNull
    @Placeholder("name")
    String getName();

    /**
     * Gets the Discord server that this channel is in.
     * @return the Discord server that contains this channel
     */
    @Placeholder(value = "server", relookup = "server")
    @NotNull
    DiscordGuild getGuild();

    /**
     * Gets the jump url for this channel
     * @return the https url to go to this channel
     */
    @NotNull
    @Placeholder("jump_url")
    String getJumpUrl();

    /**
     * Deletes the channel.
     * @return a future completing upon deletion
     */
    Task<Void> delete();
}
