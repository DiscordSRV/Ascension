/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.discord.api.entity.DiscordUserImpl;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DiscordGuildMemberImpl extends DiscordUserImpl implements DiscordGuildMember {

    private final Member member;
    private final DiscordGuild guild;
    private final List<DiscordRole> roles;
    private final Color color;

    public DiscordGuildMemberImpl(DiscordSRV discordSRV, Member member) {
        super(discordSRV, member.getUser());
        this.member = member;
        this.guild = new DiscordGuildImpl(discordSRV, member.getGuild());

        List<DiscordRole> roles = new ArrayList<>();
        for (Role role : member.getRoles()) {
            roles.add(new DiscordRoleImpl(discordSRV, role));
        }
        this.roles = roles;
        this.color = new Color(member.getColorRaw());
    }

    @Override
    public @NotNull DiscordGuild getGuild() {
        return guild;
    }

    @Override
    public @NotNull Optional<String> getNickname() {
        return Optional.ofNullable(member.getNickname());
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
    public CompletableFuture<Void> addRole(@NotNull DiscordRole role) {
        return discordSRV.discordAPI().mapExceptions(() ->
                guild.getAsJDAGuild().addRoleToMember(member, role.getAsJDARole()).submit()
        );
    }

    @Override
    public CompletableFuture<Void> removeRole(@NotNull DiscordRole role) {
        return discordSRV.discordAPI().mapExceptions(() ->
                guild.getAsJDAGuild().removeRoleFromMember(member, role.getAsJDARole()).submit()
        );
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
    public Member getAsJDAMember() {
        return member;
    }

    @Override
    public User getAsJDAUser() {
        return member.getUser();
    }

    //
    // Placeholders
    //

    @Placeholder(value = "user_highest_role", relookup = "role")
    public DiscordRole _highestRole() {
        return !roles.isEmpty() ? roles.get(0) : null;
    }

    @Placeholder(value = "user_hoisted_role", relookup = "role")
    public DiscordRole _hoistedRole() {
        for (DiscordRole role : roles) {
            if (role.isHoisted()) {
                return role;
            }
        }
        return null;
    }

    @Placeholder("user_roles")
    public Component _allRoles(@PlaceholderRemainder String suffix) {
        if (suffix.startsWith("_")) {
            suffix = suffix.substring(1);
        } else if (!suffix.isEmpty()) {
            return null;
        }

        List<Component> components = new ArrayList<>();
        for (DiscordRole role : getRoles()) {
            components.add(Component.text(role.getName()).color(TextColor.color(role.getColor().rgb())));
        }

        return ComponentUtil.join(Component.text(suffix), components);
    }

    @Override
    public String toString() {
        return "ServerMember:" + super.toString() + "/" + getGuild();
    }
}
