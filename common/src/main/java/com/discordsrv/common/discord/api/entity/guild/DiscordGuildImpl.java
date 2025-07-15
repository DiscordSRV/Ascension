/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.api.entity.guild;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DiscordGuildImpl implements DiscordGuild {

    private final DiscordSRV discordSRV;
    private final Guild guild;

    public DiscordGuildImpl(DiscordSRV discordSRV, Guild guild) {
        this.discordSRV = discordSRV;
        this.guild = guild;
    }

    @Override
    public long getId() {
        return guild.getIdLong();
    }

    @Override
    public @NotNull String getName() {
        return guild.getName();
    }

    @Override
    public int getMemberCount() {
        return guild.getMemberCount();
    }

    @Override
    public @NotNull DiscordGuildMember getSelfMember() {
        return discordSRV.discordAPI().getGuildMember(guild.getSelfMember());
    }

    @Override
    public @NotNull Task<DiscordGuildMember> retrieveMemberById(long id) {
        return discordSRV.discordAPI().toTask(() -> guild.retrieveMemberById(id))
                .thenApply(member -> new DiscordGuildMemberImpl(discordSRV, member));
    }

    @Override
    public @Nullable DiscordGuildMember getMemberById(long id) {
        Member member = guild.getMemberById(id);
        if (member == null) {
            return null;
        }

        return discordSRV.discordAPI().getGuildMember(member);
    }

    @Override
    public @NotNull Set<DiscordGuildMember> getCachedMembers() {
        Set<DiscordGuildMember> members = new HashSet<>();
        for (Member member : guild.getMembers()) {
            members.add(discordSRV.discordAPI().getGuildMember(member));
        }
        return members;
    }

    @Override
    public @Nullable DiscordRole getRoleById(long id) {
        Role role = guild.getRoleById(id);
        if (role == null) {
            return null;
        }

        return discordSRV.discordAPI().getRole(role);
    }

    @Override
    public @NotNull List<DiscordRole> getRoles() {
        List<DiscordRole> roles = new ArrayList<>();
        for (Role role : guild.getRoles()) {
            roles.add(discordSRV.discordAPI().getRole(role));
        }
        return roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscordGuildImpl that = (DiscordGuildImpl) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "Guild:" + getName() + "(" + Long.toUnsignedString(getId()) + ")";
    }

    @Override
    public Guild asJDA() {
        return guild;
    }
}
