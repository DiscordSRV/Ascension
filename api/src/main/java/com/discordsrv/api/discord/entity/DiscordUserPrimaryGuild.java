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

package com.discordsrv.api.discord.entity;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import net.dv8tion.jda.api.entities.User;

/**
 * A Discord user's primary server.
 */
@PlaceholderPrefix("primary_server_")
public interface DiscordUserPrimaryGuild extends JDAEntity<User.PrimaryGuild>, Snowflake {

    /**
     * Gets the Discord user associated with this primary server.
     *
     * @return The Discord user.
     */
    @Placeholder("user")
    DiscordUser getUser();

    /**
     * Checks if the user has identity enabled for this primary server.
     *
     * @return True if identity is enabled, false otherwise.
     */
    @Placeholder("identity_enabled")
    boolean identityEnabled();

    /**
     * Gets the tag of the primary server.
     *
     * @return The primary server tag
     */
    @Placeholder("tag")
    String getTag();

    /**
     * Gets the badge of the primary server.
     *
     * @return The primary server badge URL.
     */
    @Placeholder("badge_url")
    String getBadgeUrl();

}
