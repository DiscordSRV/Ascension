package com.discordsrv.common.permission.util;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.HashSet;
import java.util.Set;

public class DiscordPermissionUtil {
    public static boolean requiredPermissionsCheck(AbstractModule<DiscordSRV> module, String action, GuildChannel channel, Permission ...permissions) {
        Logger logger = module.logger();
        Guild guild = channel.getGuild();
        Member selfMember = guild.getSelfMember();

        Set<String> missingPermissions = new HashSet<>();
        for (Permission permission : permissions) {
            if (!selfMember.hasPermission(channel, permission)) missingPermissions.add(permission.getName());
        }

        if (missingPermissions.size() > 0) {
            logger.error("Could not " + action + " because the bot does not have the following permissions in \"" + channel.getName() + "\": " + String.join(", ", missingPermissions));
            return false;
        }
        return true;
    }
}
