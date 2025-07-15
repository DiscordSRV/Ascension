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

package com.discordsrv.common.feature.channel;

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.ChannelLockingConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.util.DiscordPermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.managers.channel.concrete.ThreadChannelManager;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

import java.util.ArrayList;
import java.util.List;

public class ChannelLockingModule extends AbstractModule<DiscordSRV> {

    public ChannelLockingModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public int shutdownOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void serverStarted() {
        run(true);
    }

    @Override
    public void serverShuttingDown() {
        run(false);
    }

    private void run(boolean unlocked) {
        for (BaseChannelConfig config : discordSRV.channelConfig().getAllChannels()) {
            IChannelConfig channelConfig = config instanceof IChannelConfig ? (IChannelConfig) config : null;
            if (channelConfig == null) {
                continue;
            }

            ChannelLockingConfig lockingConfig = config.channelLocking;
            ChannelLockingConfig.Channels channels = lockingConfig.channels;
            ChannelLockingConfig.Threads threads = lockingConfig.threads;

            boolean isChannels = channels.everyone || !channels.roleIds.isEmpty();
            boolean isThreads = threads.archive || threads.lock;

            discordSRV.destinations()
                    .lookupDestination(channelConfig.destination(), false, false)
                    .whenComplete((result, t) -> {
                        if (result.anyErrors()) {
                            logger().warning(result.compositeError("Failed to " + (unlocked ? "un" : "") + "lock some channels"));
                        }

                        for (DiscordGuildMessageChannel destination : result.channels()) {
                            if (isThreads && destination instanceof DiscordThreadChannel) {
                                ThreadChannelManager manager = ((DiscordThreadChannel) destination).asJDA().getManager();
                                if (threads.archive) {
                                    manager = manager.setArchived(!unlocked);
                                }
                                if (threads.lock) {
                                    manager = manager.setLocked(!unlocked);
                                }
                                manager.reason("DiscordSRV channel locking").queue();
                            }
                            if (isChannels) {
                                channelPermissions(channels, destination, unlocked);
                            }
                        }
                    });
        }
    }

    private void channelPermissions(
            ChannelLockingConfig.Channels shutdownConfig,
            DiscordGuildMessageChannel channel,
            boolean state
    ) {
        boolean everyone = shutdownConfig.everyone;
        List<Long> roleIds = shutdownConfig.roleIds;
        if (!everyone && roleIds.isEmpty()) {
            return;
        }

        List<Permission> permissions = new ArrayList<>();
        if (shutdownConfig.read) {
            permissions.add(Permission.VIEW_CHANNEL);
        }
        if (shutdownConfig.write) {
            permissions.add(Permission.MESSAGE_SEND);
        }
        if (shutdownConfig.addReactions) {
            permissions.add(Permission.MESSAGE_ADD_REACTION);
        }

        GuildMessageChannel messageChannel = (GuildMessageChannel) channel.getAsJDAMessageChannel();
        if (!(messageChannel instanceof IPermissionContainer)) {
            return;
        }

        Guild guild = messageChannel.getGuild();
        String missingPermissions = DiscordPermissionUtil.missingPermissionsString(messageChannel, Permission.VIEW_CHANNEL, Permission.MANAGE_PERMISSIONS);
        if (missingPermissions != null) {
            logger().error("Cannot lock " + channel + ": " + missingPermissions);
            return;
        }

        if (everyone) {
            setPermission((IPermissionContainer) messageChannel, guild.getPublicRole(), permissions, state);
        }
        for (Long roleId : roleIds) {
            Role role = guild.getRoleById(roleId);
            if (role == null) {
                continue;
            }

            setPermission((IPermissionContainer) messageChannel, role, permissions, state);
        }
    }

    private void setPermission(IPermissionContainer channel, IPermissionHolder holder, List<Permission> permissions, boolean state) {
        PermissionOverrideAction action = channel.upsertPermissionOverride(holder);
        if ((state ? action.getAllowedPermissions() : action.getDeniedPermissions()).containsAll(permissions)) {
            // Already correct
            return;
        }

        if (state) {
            action = action.grant(permissions);
        } else {
            action = action.deny(permissions);
        }
        action.reason("DiscordSRV channel locking").queue();
    }
}
