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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;

@PlaceholderPrefix("user_")
public class DiscordGuildMemberContext {

    private boolean isFiltered(BaseChannelConfig config, DiscordRole role) {
        if (config == null) {
            return false;
        }
        BaseChannelConfig.RoleSelection roleSelection = config.roleSelection;
        return roleSelection.ids.contains(role.getId()) == roleSelection.blacklist;
    }

    private Component allRoles(DiscordGuildMember member, String suffix, BaseChannelConfig config) {
        List<Component> components = new ArrayList<>();
        for (DiscordRole role : member.getRoles()) {
            if (isFiltered(config, role)) {
                continue;
            }
            components.add(Component.text(role.getName()).color(TextColor.color(role.getColor().rgb())));
        }

        return Component.join(JoinConfiguration.separator(Component.text(suffix)), components);
    }

    @Placeholder("roles")
    public Component allRoles(DiscordGuildMember member, @PlaceholderRemainder(supportsNoValue = true) String suffix) {
        return allRoles(member, suffix, null);
    }

    @Placeholder("selected_roles")
    public Component selectedAllRoles(
            DiscordGuildMember member,
            BaseChannelConfig config,
            @PlaceholderRemainder(supportsNoValue = true) String suffix
    ) {
        return allRoles(member, suffix, config);
    }

    @Placeholder("selected_highest_role")
    public DiscordRole highestRole(DiscordGuildMember member, BaseChannelConfig config) {
        for (DiscordRole role : member.getRoles()) {
            if (isFiltered(config, role)) {
                continue;
            }
            return role;
        }
        return null;
    }

    @Placeholder("selected_hoisted_role")
    public DiscordRole selectedHoistedRole(DiscordGuildMember member, BaseChannelConfig config) {
        for (DiscordRole role : member.getRoles()) {
            if (isFiltered(config, role)) {
                continue;
            }
            return role;
        }
        return null;
    }
}
