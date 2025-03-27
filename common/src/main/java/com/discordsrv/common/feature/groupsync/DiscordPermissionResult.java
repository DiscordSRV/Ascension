package com.discordsrv.common.feature.groupsync;

import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.util.DiscordPermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.EnumSet;

public class DiscordPermissionResult implements ISyncResult {

    public static DiscordPermissionResult of(Guild guild, EnumSet<Permission> permissions) {
        return new DiscordPermissionResult(guild, null, permissions);
    }

    public static DiscordPermissionResult of(GuildChannel channel, EnumSet<Permission> permissions) {
        return new DiscordPermissionResult(null, channel, permissions);
    }

    private final Guild guild;
    private final GuildChannel channel;
    private final EnumSet<Permission> missingPermissions;

    private DiscordPermissionResult(
            Guild guild,
            GuildChannel channel,
            EnumSet<Permission> missingPermissions
    ) {
        this.guild = guild;
        this.channel = channel;
        this.missingPermissions = missingPermissions;
    }

    @Override
    public boolean isError() {
        return true;
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public String getFormat() {
        return DiscordPermissionUtil.createErrorMessage(channel, guild, missingPermissions);
    }
}
