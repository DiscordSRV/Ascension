/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.entity.guild;

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.Mentionable;
import com.discordsrv.api.discord.entity.Snowflake;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;

/**
 * A Discord server role.
 */
@PlaceholderPrefix("role_")
public interface DiscordRole extends JDAEntity<Role>, Snowflake, Mentionable {

    /**
     * The default {@link DiscordRole} color.
     */
    Color DEFAULT_COLOR = new Color(0xFFFFFF);

    /**
     * The Discord server this role is from.
     * @return the Discord server
     */
    @Placeholder(value = "server", relookup = "server")
    @NotNull
    DiscordGuild getGuild();

    /**
     * Gets the name of the Discord role.
     * @return the role name
     */
    @Placeholder("name")
    @NotNull
    String getName();

    /**
     * Does this role have a color.
     * @return true if this role has a set color
     */
    default boolean hasColor() {
        return !DEFAULT_COLOR.equals(getColor());
    }

    /**
     * The color of this rule.
     * @return the color of this role, or {@link #DEFAULT_COLOR} if there is no color set
     * @see #hasColor()
     */
    @Placeholder(value = "color", relookup = "color")
    @NotNull
    Color getColor();

    /**
     * Is this role hoisted.
     * @return true if this role is displayed separately in the member list
     */
    boolean isHoisted();

}
