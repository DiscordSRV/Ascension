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

import com.discordsrv.api.discord.api.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.api.entity.guild.DiscordRole;
import com.discordsrv.common.discord.api.user.DiscordUserImpl;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiscordGuildMemberImpl extends DiscordUserImpl implements DiscordGuildMember {

    private final String nickname;
    private final List<DiscordRole> roles;

    public DiscordGuildMemberImpl(Member member) {
        super(member.getUser());
        this.nickname = member.getNickname();

        List<DiscordRole> roles = new ArrayList<>();
        for (Role role : member.getRoles()) {
            roles.add(new DiscordRoleImpl(role));
        }
        this.roles = roles;
    }

    @Override
    public @NotNull Optional<String> getNickname() {
        return Optional.ofNullable(nickname);
    }

    @Override
    public List<DiscordRole> getRoles() {
        return roles;
    }
}
