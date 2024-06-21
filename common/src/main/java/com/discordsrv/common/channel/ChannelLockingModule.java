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

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.ChannelLockingConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.discord.util.DiscordPermissionUtil;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

import java.util.ArrayList;
import java.util.Collection;
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

            discordSRV.destinations()
                    .lookupDestination(((IChannelConfig) config).destination(), false, true)
                    .whenComplete((destinations, t) -> {
                        if (channels.everyone || !channels.roleIds.isEmpty()) {
                            for (DiscordGuildMessageChannel destination : destinations) {
                                channelPermissions(channels, destination, true);
                            }
                        }
                    });
        });
    }

    @Override
    public void disable() {
        doForAllChannels((config, channelConfig) -> {
            if (!(config instanceof IChannelConfig)) {
                return;
            }

            ChannelLockingConfig shutdownConfig = config.channelLocking;
            ChannelLockingConfig.Channels channels = shutdownConfig.channels;
            ChannelLockingConfig.Threads threads = shutdownConfig.threads;

            boolean archive = threads.archive;
            boolean isChannels = channels.everyone || !channels.roleIds.isEmpty();
            if (!threads.archive && !isChannels) {
                return;
            }

            Collection<DiscordGuildMessageChannel> destinations = discordSRV.destinations()
                    .lookupDestination(((IChannelConfig) config).destination(), false, false).join();

            for (DiscordGuildMessageChannel destination : destinations) {
                if (archive && destination instanceof DiscordThreadChannel) {
                    ((DiscordThreadChannel) destination).asJDA().getManager()
                            .setArchived(true)
                            .reason("DiscordSRV channel locking")
                            .queue();
                }
                if (isChannels) {
                    channelPermissions(channels, destination, false);
                }
            }
        });
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
            logger().error("Cannot lock #" + channel.getName() + ": " + missingPermissions);
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
