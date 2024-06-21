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
        EnumSet<Permission> missingPermissions = checkMissingPermissions(channel, permissions);
        return createErrorMessage(missingPermissions, "#" + channel.getName());
    }

    public static EnumSet<Permission> checkMissingPermissions(GuildChannel channel, Collection<Permission> permissions) {
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
        EnumSet<Permission> missingPermissions = checkMissingPermissions(guild, permissions);
        return createErrorMessage(missingPermissions, guild.getName());
    }

    public static EnumSet<Permission> checkMissingPermissions(Guild guild, Collection<Permission> permissions) {
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
