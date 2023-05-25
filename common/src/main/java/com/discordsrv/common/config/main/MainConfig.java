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

package com.discordsrv.common.config.main;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.common.config.Config;
import com.discordsrv.common.config.annotation.DefaultOnly;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.linking.LinkedAccountConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.*;

@ConfigSerializable
public abstract class MainConfig implements Config {

    public static final String FILE_NAME = "config.yaml";

    @Override
    public final String getFileName() {
        return FILE_NAME;
    }

    public BaseChannelConfig createDefaultChannel() {
        return new ChannelConfig();
    }

    public BaseChannelConfig createDefaultBaseChannel() {
        return new BaseChannelConfig();
    }

    @DefaultOnly(ChannelConfig.DEFAULT_KEY)
    public Map<String, BaseChannelConfig> channels = new LinkedHashMap<String, BaseChannelConfig>() {{
        put(GameChannel.DEFAULT_NAME, createDefaultChannel());
        put(ChannelConfig.DEFAULT_KEY, createDefaultBaseChannel());
    }};

    public LinkedAccountConfig linkedAccounts = new LinkedAccountConfig();

    public MemberCachingConfig memberCaching = new MemberCachingConfig();

    public List<ChannelUpdaterConfig> channelUpdaters = new ArrayList<>(Collections.singletonList(new ChannelUpdaterConfig()));

    @Comment("Configuration options for group-role synchronization")
    public GroupSyncConfig groupSync = new GroupSyncConfig();

    @Comment("Command configuration")
    public CommandConfig command = new CommandConfig();

    @Comment("Configuration for the %discord_invite% placeholder. The below options will be attempted in the order they are in")
    public DiscordInviteConfig invite = new DiscordInviteConfig();

    public abstract PluginIntegrationConfig integrations();
}
