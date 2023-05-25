/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.channel;

import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.ChannelLockingConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ChannelLockingModule extends AbstractModule<DiscordSRV> {

    public ChannelLockingModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public int shutdownOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void enable() {
        doForAllChannels((config, channelConfig) -> {
            ChannelLockingConfig shutdownConfig = config.channelLocking;
            ChannelLockingConfig.Channels channels = shutdownConfig.channels;
            ChannelLockingConfig.Threads threads = shutdownConfig.threads;

            if (threads.unarchive) {
                discordSRV.discordAPI().findOrCreateThreads(config, channelConfig, __ -> {}, new ArrayList<>(), false);
            }
            channelPermissions(channelConfig, channels, true);
        });
    }

    @Override
    public void disable() {
        doForAllChannels((config, channelConfig) -> {
            ChannelLockingConfig shutdownConfig = config.channelLocking;
            ChannelLockingConfig.Channels channels = shutdownConfig.channels;
            ChannelLockingConfig.Threads threads = shutdownConfig.threads;

            if (threads.archive) {
                for (DiscordThreadChannel thread : discordSRV.discordAPI().findThreads(config, channelConfig)) {
                    thread.asJDA().getManager()
                            .setArchived(true)
                            .reason("DiscordSRV channel locking")
                            .queue();
                }
            }
            channelPermissions(channelConfig, channels, false);
        });
    }

    private void channelPermissions(
            IChannelConfig channelConfig,
            ChannelLockingConfig.Channels shutdownConfig,
            boolean state
    ) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return;
        }

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

        for (Long channelId : channelConfig.channelIds()) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                continue;
            }

            Guild guild = channel.getGuild();
            if (!guild.getSelfMember().hasPermission(channel, Permission.MANAGE_PERMISSIONS)) {
                logger().error("Cannot change permissions of " + channel + ": lacking \"Manage Permissions\" permission");
                continue;
            }

            if (everyone) {
                setPermission(channel, guild.getPublicRole(), permissions, state);
            }
            for (Long roleId : roleIds) {
                Role role = channel.getGuild().getRoleById(roleId);
                if (role == null) {
                    continue;
                }

                setPermission(channel, role, permissions, state);
            }
        }
    }

    private void setPermission(TextChannel channel, IPermissionHolder holder, List<Permission> permissions, boolean state) {
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

    private void doForAllChannels(BiConsumer<BaseChannelConfig, IChannelConfig> channelConsumer) {
        for (BaseChannelConfig config : discordSRV.channelConfig().getAllChannels()) {
            IChannelConfig channelConfig = config instanceof IChannelConfig ? (IChannelConfig) config : null;
            if (channelConfig == null) {
                continue;
            }

            channelConsumer.accept(config, channelConfig);
        }
    }
}
