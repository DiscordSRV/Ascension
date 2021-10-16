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

package com.discordsrv.common.discord.api.guild;

import com.discordsrv.api.discord.api.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.api.entity.guild.DiscordRole;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Optional;

public class DiscordGuildImpl implements DiscordGuild {

    private final DiscordSRV discordSRV;
    private final long id;
    private final String name;
    private final int memberCount;

    public DiscordGuildImpl(DiscordSRV discordSRV, Guild guild) {
        this.discordSRV = discordSRV;
        this.id = guild.getIdLong();
        this.name = guild.getName();
        this.memberCount = guild.getMemberCount();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMemberCount() {
        return memberCount;
    }

    private Optional<Guild> guild() {
        return discordSRV.jda()
                .map(jda -> jda.getGuildById(id));
    }

    @Override
    public Optional<DiscordGuildMember> getMemberById(long id) {
        return guild()
                .map(guild -> guild.getMemberById(id))
                .map(member -> new DiscordGuildMemberImpl(discordSRV, member));
    }

    @Override
    public Optional<DiscordRole> getRoleById(long id) {
        return guild()
                .map(guild -> guild.getRoleById(id))
                .map(DiscordRoleImpl::new);
    }
}
