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
import com.discordsrv.common.config.configurate.annotation.DefaultOnly;
import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.linking.LinkedAccountConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigSerializable
public abstract class MainConfig implements Config {

    public static final String FILE_NAME = "config.yaml";

    public static final String HEADER = String.join("\n", Arrays.asList(
            "Welcome to the DiscordSRV configuration file",
            "",
            "Looking for the \"BotToken\" option? It has been moved into the " + ConnectionConfig.FILE_NAME,
            "Need help with the format for Minecraft messages? https://github.com/Vankka/EnhancedLegacyText/wiki/Format",
            "Need help with Discord markdown? https://support.discord.com/hc/en-us/articles/210298617"
    ));

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
    @Comment("Channels configuration\n\n"
            + "This is where everything related to in-game chat channels is configured.\n"
            + "The key of this option is the in-game channel name (the default keys are \"global\" and \"default\")\n"
            + "channel-ids and threads can be configured for all channels except \"default\"\n"
            + "\"default\" is a special section which has the default values for all channels unless they are specified (overridden) under the channel's own section\n"
            + "So if you don't specify a certain option under a channel's own section, the option will take its value from the \"default\" section")
    public Map<String, BaseChannelConfig> channels = new LinkedHashMap<String, BaseChannelConfig>() {{
        put(GameChannel.DEFAULT_NAME, createDefaultChannel());
        put(ChannelConfig.DEFAULT_KEY, createDefaultBaseChannel());
    }};

    public LinkedAccountConfig linkedAccounts = new LinkedAccountConfig();

    public TimedUpdaterConfig timedUpdater = new TimedUpdaterConfig();

    @Comment("Configuration options for group-role synchronization")
    public GroupSyncConfig groupSync = new GroupSyncConfig();

    @Comment("In-game command configuration")
    public GameCommandConfig gameCommand = new GameCommandConfig();

    @Comment("Discord command configuration")
    public DiscordCommandConfig discordCommand = new DiscordCommandConfig();

    @Comment("Configuration for the %discord_invite% placeholder. The below options will be attempted in the order they are in")
    public DiscordInviteConfig invite = new DiscordInviteConfig();

    public MessagesMainConfig messages = new MessagesMainConfig();

    @Order(10) // To go below required linking config @ 5
    @Comment("Configuration for the %player_avatar_url% placeholder")
    public AvatarProviderConfig avatarProvider = new AvatarProviderConfig();

    public abstract PluginIntegrationConfig integrations();

    @Order(1000)
    public MemberCachingConfig memberCaching = new MemberCachingConfig();

    @Order(5000)
    @Comment("Options for diagnosing DiscordSRV, you do not need to touch these options during normal operation")
    public DebugConfig debug = new DebugConfig();
}
