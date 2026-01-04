/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.api.entity;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.DiscordUserPrimaryGuild;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.User;

public class DiscordUserPrimaryGuildImpl implements DiscordUserPrimaryGuild {

    protected final DiscordSRV discordSRV;
    protected final DiscordUser user;
    protected final User.PrimaryGuild primaryGuild;

    public DiscordUserPrimaryGuildImpl(DiscordSRV discordSRV, User user) {
        this.discordSRV = discordSRV;
        this.user = discordSRV.discordAPI().getUser(user);
        this.primaryGuild = user.getPrimaryGuild();
    }

    @Override
    public DiscordUser getUser() {
        return user;
    }

    @Override
    public boolean identityEnabled() {
        return primaryGuild.isIdentityEnabled();
    }

    @Override
    public String getTag() {
        return primaryGuild.getTag();
    }

    @Override
    public String getBadgeUrl() {
        return primaryGuild.getBadgeUrl();
    }

    @Override
    public User.PrimaryGuild asJDA() {
        return primaryGuild;
    }

    @Override
    public long getId() {
        return primaryGuild.getIdLong();
    }

    @Override
    public String toString() {
        return "DiscordUserPrimaryGuild{id=" + getId() + ", user=" + user.getId() + ", tag=" + getTag() + "}";
    }

}
