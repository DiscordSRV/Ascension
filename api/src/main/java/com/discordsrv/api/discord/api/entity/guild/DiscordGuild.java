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

package com.discordsrv.api.discord.api.entity.guild;

import com.discordsrv.api.discord.api.entity.Snowflake;
import com.discordsrv.api.placeholder.annotation.Placeholder;

import java.util.Optional;

/**
 * A Discord server.
 */
public interface DiscordGuild extends Snowflake {

    /**
     * Gets the name of this Discord guild.
     * @return the guild's name
     */
    @Placeholder("server_name")
    String getName();

    /**
     * Gets the member count of the guild.
     * @return the guild's member count
     */
    @Placeholder("server_member_count")
    int getMemberCount();

    /**
     * Gets a Discord guild member by id from the cache, the provided entity can be cached and will not update if it changes on Discord.
     * @param id the id for the Discord guild member
     * @return the Discord guild member from the cache
     */
    Optional<DiscordGuildMember> getMemberById(long id);

    /**
     * Gets a Discord role by id from the cache, the provided entity can be cached and will not update if it changes on Discord.
     * @param id the id for the Discord role
     * @return the Discord role from the cache
     */
    Optional<DiscordRole> getRoleById(long id);

}
