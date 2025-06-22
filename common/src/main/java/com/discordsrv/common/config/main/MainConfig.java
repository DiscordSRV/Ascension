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

package com.discordsrv.common.config.main;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.common.config.Config;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.DefaultOnly;
import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.command.CustomCommandConfig;
import com.discordsrv.common.config.main.command.ExecuteCommandConfig;
import com.discordsrv.common.config.main.command.GameCommandConfig;
import com.discordsrv.common.config.main.command.PlayerListConfig;
import com.discordsrv.common.config.main.linking.LinkedAccountConfig;
import com.discordsrv.common.config.main.sync.BanSyncConfig;
import com.discordsrv.common.config.main.sync.GroupSyncConfig;
import com.discordsrv.common.config.main.sync.NicknameSyncConfig;
import com.discordsrv.common.config.main.sync.OnlineRoleConfig;
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

    private static final String PLAYERS_NEED_TO_BE_LINKED = "Requires players to be linked";

    // File header

    @Constants({
            ConnectionConfig.FILE_NAME,
            DocumentationURLs.ELT_FORMAT,
            DocumentationURLs.DISCORD_MARKDOWN,
            DocumentationURLs.PLACEHOLDERS
    })
    public static final String HEADER = String.join("\n", Arrays.asList(
            "Welcome to the DiscordSRV configuration file",
            "",
            "Looking for the \"BotToken\" option? It has been moved into the %1 file",
            "Need help with the format for Minecraft messages? %2",
            "Need help with Discord markdown? %3",
            "List of placeholders %4"
    ));

    // Automatic migration toggle

    @Comment("Automatically upgrade configuration files on startup or reload if any values are missing")
    @Order(100)
    public boolean automaticConfigurationUpgrade = true;

    // Channels

    public BaseChannelConfig createDefaultChannel() {
        return new ChannelConfig();
    }
    public BaseChannelConfig createDefaultBaseChannel() {
        return new BaseChannelConfig();
    }

    @DefaultOnly(ChannelConfig.DEFAULT_KEY)
    @Comment("Channels configuration\n\n"
            + "This is where everything related to in-game chat channel synchronization to Discord is configured.\n"
            + "The key of this option is the in-game channel name (the default keys are \"%1\" and \"%2\")\n"
            + "%3 and %4 can be configured for all channels except \"%2\"\n"
            + "\"%2\" is a special section which has the default values for all channels unless they are specified (overridden) under the channel's own section\n"
            + "So if you don't specify a certain option under a channel's own section, the option will take its value from the \"%2\" section")
    @Constants.Comment({GameChannel.DEFAULT_NAME, ChannelConfig.DEFAULT_KEY, "channel-ids", "threads"})
    @Order(200)
    public Map<String, BaseChannelConfig> channels = new LinkedHashMap<String, BaseChannelConfig>() {{
        put(GameChannel.DEFAULT_NAME, createDefaultChannel());
        put(ChannelConfig.DEFAULT_KEY, createDefaultBaseChannel());
    }};

    // Presence Updater & Invite
    @Order(300)
    public PresenceUpdaterConfig presenceUpdater = defaultPresenceUpdater();
    protected PresenceUpdaterConfig defaultPresenceUpdater() {
        return new PresenceUpdaterConfig();
    }

    @Comment("Configuration for the %1 placeholder. The below options will be attempted in the order they are in")
    @Constants.Comment("%discord_invite%")
    @Order(310)
    public DiscordInviteConfig invite = new DiscordInviteConfig();

    // Linked accounts, required linking & rewards
    @Comment("Options for linking Discord and Minecraft accounts together")
    @Order(400)
    public LinkedAccountConfig linkedAccounts = new LinkedAccountConfig();

    // RequiredLinking goes here

    @Comment("Rewards granted for linking accounts and boosting")
    @Order(420)
    public RewardsConfig rewards = new RewardsConfig();

    // Console
    @Comment("Allows for creating channels and/or threads that act like the Minecraft server console\n"
            + "Multiple configurations are allowed for forwarding different portions of logs into different places,\n"
            + "configuring the entire console output to be forwarded to multiple places is discouraged.\n"
            + "\n"
            + "Using this feature as your primary way to view log history is not recommended.\n"
            + "The default configuration uses thread rotation, where a new thread will be created every week, and only 3 weeks are kept.\n"
            + "\n"
            + "Be careful of who you let view and run commands in your console channels!\n"
            + "Configuring this incorrectly can lead to sensitive information being exposed and your server being hacked!")
    @Order(500)
    public List<ConsoleConfig> console = new ArrayList<>(Collections.singleton(new ConsoleConfig()));

    // "Sync" features

    @Comment("Configuration options for Minecraft group and Discord role synchronization\n"
            + "\n"
            + PLAYERS_NEED_TO_BE_LINKED + "\n"
            + "For Minecraft to Discord synchronization:\n"
            + "- The bot needs a role above all roles that are synchronized\n"
            + "- The bot needs the \"Manage Roles\" permission")
    @Order(610)
    public GroupSyncConfig groupSync = new GroupSyncConfig();

    @Comment("Configuration options for nickname synchronization\n"
            + "\n"
            + PLAYERS_NEED_TO_BE_LINKED + "\n"
            + "For Minecraft to Discord synchronization:\n"
            + "- The bot needs a role above all users that you want to synchronize, the Discord server owner cannot be synchronized. "
            + "- The bot needs the \"Manage Nicknames\" permission")
    @Order(620)
    public NicknameSyncConfig nicknameSync = new NicknameSyncConfig();

    @Comment("Configuration options for ban synchronization\n"
            + "\n"
            + PLAYERS_NEED_TO_BE_LINKED + "\n"
            + "For Minecraft to Discord synchronization:\n"
            + "- The bot needs a role above all users that you want to synchronize, the Discord server owner cannot be synchronized.\n"
            + "- The bot needs the \"Ban Members\" permission")
    @Order(630)
    public BanSyncConfig banSync = new BanSyncConfig();

    @Comment("Options for granting players that are currently online a role in Discord\n"
            + "\n"
            + PLAYERS_NEED_TO_BE_LINKED + "\n"
            + "The bot needs to have a role above the online role\n"
            + "The bot needs the \"Manage Roles\" permission")
    @Order(640)
    public OnlineRoleConfig onlineRole = new OnlineRoleConfig();

    // Commands
    @Comment("In-game command configuration")
    @Order(700)
    public GameCommandConfig gameCommand = new GameCommandConfig();

    @Comment("Configuration for the /discordsrv execute Discord command")
    @Order(710)
    public ExecuteCommandConfig executeCommand = new ExecuteCommandConfig();

    @Comment("Configuration for the /minecraft playerlist Discord command and %playerlist% placeholder")
    @Order(720)
    public PlayerListConfig playerList = new PlayerListConfig();

    @Comment("Custom commands that can trigger console commands and provide a customized output when executed in Discord")
    @Order(730)
    public List<CustomCommandConfig> customCommands = new ArrayList<>(Arrays.asList(
            CustomCommandConfig.defaultIp(),
            CustomCommandConfig.defaultHelloWorld()
    ));

    // Channel updater, Discord doesn't like these very much so they're quite low in the config to discourage usage

    @Comment("Timed updating for channel names and/or topics")
    @Order(900)
    public ChannelUpdaterConfig channelUpdater = new ChannelUpdaterConfig();

    // "One-time" configuration options

    @Comment("Configuration for the %1 placeholder")
    @Constants.Comment("%player_avatar_url%")
    @Order(5000)
    public AvatarProviderConfig avatarProvider = new AvatarProviderConfig();

    @Comment("Configuration for internationalization and localization (i18n/l10n)")
    @Order(5001)
    public MessagesMainConfig messages = new MessagesMainConfig();

    // "Fine-tuning" and debugging options

    @Order(6000)
    public PluginIntegrationConfig integrations = defaultIntegrations();
    protected PluginIntegrationConfig defaultIntegrations() {
        return new PluginIntegrationConfig();
    }

    @Order(6001)
    @Comment("These options are for fine-tuning, only touch them if you know what you're doing")
    public MemberCachingConfig memberCaching = new MemberCachingConfig();

    @Order(100_000)
    @Comment("Options for diagnosing DiscordSRV, you do not need to touch these options during normal operation")
    public DebugConfig debug = new DebugConfig();
}
