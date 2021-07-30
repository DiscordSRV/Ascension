/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.common.discord.api.user;

import com.discordsrv.api.discord.api.entity.user.DiscordUser;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

public class DiscordUserImpl implements DiscordUser {

    private final String id;
    private final String username;
    private final String discriminator;

    public DiscordUserImpl(User user) {
        this.id = user.getId();
        this.username = user.getName();
        this.discriminator = user.getDiscriminator();
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull String getUsername() {
        return username;
    }

    @Override
    public @NotNull String getDiscriminator() {
        return discriminator;
    }
}
