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
