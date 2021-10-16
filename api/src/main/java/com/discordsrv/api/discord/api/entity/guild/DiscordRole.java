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

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.api.entity.Snowflake;
import com.discordsrv.api.placeholder.Placeholder;
import org.jetbrains.annotations.NotNull;

/**
 * A Discord server role.
 */
public interface DiscordRole extends Snowflake {

    /**
     * The default {@link DiscordRole} color.
     */
    Color DEFAULT_COLOR = new Color(0xFFFFFF);

    /**
     * Gets the name of the Discord role.
     * @return the role name
     */
    @NotNull
    @Placeholder("role_name")
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
    @Placeholder("role_color")
    Color getColor();

    /**
     * Is this role hoisted.
     * @return true if this role is displayed separately in the member list
     */
    boolean isHoisted();
}
