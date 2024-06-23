/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.util;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Collectors;

public final class DiscordPermissionUtil {

    private DiscordPermissionUtil() {}

    public static String missingPermissionsString(GuildChannel channel, Permission... permissions) {
        return missingPermissionsString(channel, Arrays.asList(permissions));
    }

    public static String missingPermissionsString(GuildChannel channel, Collection<Permission> permissions) {
        if (channel instanceof ThreadChannel) {
            channel = ((ThreadChannel) channel).getParentChannel();
        }
        EnumSet<Permission> missingPermissions = getMissingPermissions(channel, permissions);
        return createErrorMessage(missingPermissions, "#" + channel.getName());
    }

    public static EnumSet<Permission> getMissingPermissions(GuildChannel channel, Collection<Permission> permissions) {
        if (channel instanceof ThreadChannel) {
            channel = ((ThreadChannel) channel).getParentChannel();
        }
        EnumSet<Permission> missingPermissions = EnumSet.noneOf(Permission.class);
        for (Permission permission : permissions) {
            if (!channel.getGuild().getSelfMember().hasPermission(channel, permission)) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }

    public static String missingPermissionsString(Guild guild, Permission... permissions) {
        return missingPermissionsString(guild, Arrays.asList(permissions));
    }

    public static String missingPermissionsString(Guild guild, Collection<Permission> permissions) {
        EnumSet<Permission> missingPermissions = getMissingPermissions(guild, permissions);
        return createErrorMessage(missingPermissions, guild.getName());
    }

    public static EnumSet<Permission> getMissingPermissions(Guild guild, Collection<Permission> permissions) {
        EnumSet<Permission> missingPermissions = EnumSet.noneOf(Permission.class);
        for (Permission permission : permissions) {
            if (!guild.getSelfMember().hasPermission(permission)) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }

    public static String createErrorMessage(EnumSet<Permission> permissions, String where) {
        if (permissions.isEmpty()) {
            return null;
        }

        return "the bot is lacking permissions in " + where + ": "
                + permissions.stream().map(Permission::getName).collect(Collectors.joining(", "));
    }
}
