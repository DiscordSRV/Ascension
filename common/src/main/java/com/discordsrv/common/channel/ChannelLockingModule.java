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

package com.discordsrv.common.channel;

import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.ChannelLockingConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

import java.util.ArrayList;
import java.util.Collections;
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
            OrDefault<ChannelLockingConfig> shutdownConfig = config.map(cfg -> cfg.channelLocking);
            OrDefault<ChannelLockingConfig.Channels> channels = shutdownConfig.map(cfg -> cfg.channels);
            OrDefault<ChannelLockingConfig.Threads> threads = shutdownConfig.map(cfg -> cfg.threads);

            if (threads.get(cfg -> cfg.unarchive, true)) {
                discordSRV.discordAPI().findOrCreateThreads(config, channelConfig, __ -> {}, new ArrayList<>(), false);
            }
            channelPermissions(channelConfig, channels, true);
        });
    }

    @Override
    public void disable() {
        doForAllChannels((config, channelConfig) -> {
            OrDefault<ChannelLockingConfig> shutdownConfig = config.map(cfg -> cfg.channelLocking);
            OrDefault<ChannelLockingConfig.Channels> channels = shutdownConfig.map(cfg -> cfg.channels);
            OrDefault<ChannelLockingConfig.Threads> threads = shutdownConfig.map(cfg -> cfg.threads);

            if (threads.get(cfg -> cfg.archive, true)) {
                for (DiscordThreadChannel thread : discordSRV.discordAPI().findThreads(config, channelConfig)) {
                    thread.getAsJDAThreadChannel().getManager()
                            .setArchived(true)
                            .reason("DiscordSRV shutdown behaviour")
                            .queue();
                }
            }
            channelPermissions(channelConfig, channels, false);
        });
    }

    private void channelPermissions(
            IChannelConfig channelConfig,
            OrDefault<ChannelLockingConfig.Channels> shutdownConfig,
            boolean state
    ) {
        JDA jda = discordSRV.jda().orElse(null);
        if (jda == null) {
            return;
        }

        boolean everyone = shutdownConfig.get(cfg -> cfg.everyone, false);
        List<Long> roleIds = shutdownConfig.get(cfg -> cfg.roleIds, Collections.emptyList());
        if (!everyone && roleIds.isEmpty()) {
            return;
        }

        List<Permission> permissions = new ArrayList<>();
        if (shutdownConfig.get(cfg -> cfg.read, false)) {
            permissions.add(Permission.VIEW_CHANNEL);
        }
        if (shutdownConfig.get(cfg -> cfg.write, true)) {
            permissions.add(Permission.MESSAGE_SEND);
        }
        if (shutdownConfig.get(cfg -> cfg.addReactions, true)) {
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
        PermissionOverride override = channel.getPermissionOverride(holder);
        if (override != null && (state ? override.getAllowed() : override.getDenied()).containsAll(permissions)) {
            // Already correct
            return;
        }

        PermissionOverrideAction action = override != null
                                          ? override.getManager()
                                          : channel.putPermissionOverride(holder);

        if (state) {
            action = action.grant(permissions);
        } else {
            action = action.deny(permissions);
        }
        action.reason("DiscordSRV shutdown behaviour").queue();
    }

    private void doForAllChannels(BiConsumer<OrDefault<BaseChannelConfig>, IChannelConfig> channelConsumer) {
        for (OrDefault<BaseChannelConfig> config : discordSRV.channelConfig().getAllChannels()) {
            IChannelConfig channelConfig = config.get(cfg -> cfg instanceof IChannelConfig ? (IChannelConfig) cfg : null);
            if (channelConfig == null) {
                continue;
            }

            channelConsumer.accept(config, channelConfig);
        }
    }
}
