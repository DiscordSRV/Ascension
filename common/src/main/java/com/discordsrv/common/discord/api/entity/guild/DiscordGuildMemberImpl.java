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

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@PlaceholderPrefix("user_")
public class DiscordGuildMemberImpl implements DiscordGuildMember {

    private final DiscordSRV discordSRV;
    private final Member member;
    private final DiscordUser user;
    private final DiscordGuild guild;
    private final List<DiscordRole> roles;
    private final Color color;

    public DiscordGuildMemberImpl(DiscordSRV discordSRV, Member member) {
        this.discordSRV = discordSRV;
        this.member = member;
        this.user = discordSRV.discordAPI().getUser(member.getUser());
        this.guild = discordSRV.discordAPI().getGuild(member.getGuild());

        List<DiscordRole> roles = new ArrayList<>();
        for (Role role : member.getRoles()) {
            roles.add(discordSRV.discordAPI().getRole(role));
        }
        this.roles = roles;
        this.color = new Color(member.getColorRaw());
    }

    @Override
    public @NotNull DiscordUser getUser() {
        return user;
    }

    @Override
    public @NotNull DiscordGuild getGuild() {
        return guild;
    }

    @Override
    public boolean isOwner() {
        return member.isOwner();
    }

    @Override
    public @Nullable String getNickname() {
        return member.getNickname();
    }

    @Override
    public @NotNull List<DiscordRole> getRoles() {
        return roles;
    }

    @Override
    public boolean hasRole(@NotNull DiscordRole role) {
        return roles.stream().anyMatch(role::equals);
    }

    @Override
    public boolean canInteract(@NotNull DiscordRole role) {
        return member.canInteract(role.asJDA());
    }

    @Override
    public Task<Void> addRole(@NotNull DiscordRole role) {
        return discordSRV.discordAPI().toTask(() -> guild.asJDA().addRoleToMember(member, role.asJDA()));
    }

    @Override
    public Task<Void> removeRole(@NotNull DiscordRole role) {
        return discordSRV.discordAPI().toTask(() -> guild.asJDA().removeRoleFromMember(member, role.asJDA()));
    }

    @Override
    public @NotNull String getEffectiveServerAvatarUrl() {
        return member.getEffectiveAvatarUrl();
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public @NotNull OffsetDateTime getTimeJoined() {
        return member.getTimeJoined();
    }

    @Override
    public @Nullable OffsetDateTime getTimeBoosted() {
        return member.getTimeBoosted();
    }

    //
    // Placeholders
    //

    @Placeholder(value = "highest_role", relookup = "role")
    public DiscordRole _highestRole() {
        return !roles.isEmpty() ? roles.get(0) : null;
    }

    @Placeholder(value = "hoisted_role", relookup = "role")
    public DiscordRole _hoistedRole() {
        for (DiscordRole role : roles) {
            if (role.isHoisted()) {
                return role;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ServerMember:" + super.toString() + "/" + getGuild();
    }

    @Override
    public String getAsMention() {
        return member.getAsMention();
    }

    @Override
    public Member asJDA() {
        return member;
    }
}
