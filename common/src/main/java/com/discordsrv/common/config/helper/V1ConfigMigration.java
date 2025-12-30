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

package com.discordsrv.common.config.helper;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.ConsoleConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.config.main.channels.JoinMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.generic.DiscordOutputMode;
import com.discordsrv.common.config.main.generic.GameCommandExecutionConditionConfig;
import com.discordsrv.common.config.main.sync.GroupSyncConfig;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class V1ConfigMigration {

    private final DiscordSRV discordSRV;
    private final ConfigurationNode config;
    private final ConfigurationNode messages;
    private final ConfigurationNode linking;
    private final ConfigurationNode synchronization;

    public V1ConfigMigration(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.config = loadNode(discordSRV, "config.yml");
        this.messages = loadNode(discordSRV, "messages.yml");
        this.linking = loadNode(discordSRV, "linking.yml");
        this.synchronization = loadNode(discordSRV, "synchronization.yml");
    }

    private static ConfigurationNode loadNode(DiscordSRV discordSRV, String fileName) {
        try {
            Path path = discordSRV.dataDirectory().resolve(fileName);
            if (!Files.exists(path)) {
                return null;
            }
            return YamlConfigurationLoader.builder().path(path).build().load();
        } catch (ConfigurateException e) {
            discordSRV.logger().warning("Failed to load v1 " + fileName + " for migration", e);
            return null;
        }
    }

    public void migrate(MainConfig mainConfig, boolean recommendedOnly) throws SerializationException {
        mainConfig.channels.remove("global");
        BaseChannelConfig defaultChannel = mainConfig.channels.get("default");
        if (defaultChannel != null) {
            defaultChannel.discordToMinecraft.enabled = config.node("DiscordChatChannelDiscordToMinecraft").getBoolean(true);
            defaultChannel.minecraftToDiscord.enabled = config.node("DiscordChatChannelMinecraftToDiscord").getBoolean(true);

            if (!recommendedOnly) {
                int truncateLength = config.node("DiscordChatChannelTruncateLength").getInt(-1);
                if (truncateLength > 0 && truncateLength < 4000) {
                    defaultChannel.discordToMinecraft.contentRegexFilters.put(Pattern.compile("(.{" + truncateLength + "}).*"), "$1");
                }
            }

            boolean mentionsEnabled = config.node("DiscordChatChannelTranslateMentions").getBoolean(true);
            List<String> allowedMentions = mentionsEnabled
                                           ? config.node("DiscordChatChannelAllowedMentions").getList(String.class, Collections.emptyList())
                                           : Collections.emptyList();
            defaultChannel.minecraftToDiscord.mentions.channels = allowedMentions.contains("channel");
            defaultChannel.minecraftToDiscord.mentions.everyone = allowedMentions.contains("everyone");
            defaultChannel.minecraftToDiscord.mentions.roles = allowedMentions.contains("role");
            defaultChannel.minecraftToDiscord.mentions.users = allowedMentions.contains("user");

            String emojiBehaviour = config.node("DiscordChatChannelEmojiBehavior").getString();
            if ("show".equalsIgnoreCase(emojiBehaviour)) {
                defaultChannel.discordToMinecraft.unicodeEmojiBehaviour = DiscordToMinecraftChatConfig.EmojiBehaviour.SHOW;
            } else if ("hide".equalsIgnoreCase(emojiBehaviour)) {
                defaultChannel.discordToMinecraft.unicodeEmojiBehaviour = DiscordToMinecraftChatConfig.EmojiBehaviour.HIDE;
            }
            // Missing: DiscordChatChannelEmojiBehavior = name
            // Missing: DiscordChatChannelEmoteBehavior


            if (!recommendedOnly) {
                String requiredPrefix = config.node("DiscordChatChannelPrefixRequiredToProcessMessage").getString();
                boolean blacklist = config.node("DiscordChatChannelPrefixActsAsBlacklist").getBoolean(true);
                if (requiredPrefix != null && blacklist) {
                    // Remove messages starting with the prefix
                    defaultChannel.minecraftToDiscord.contentRegexFilters.put(
                            Pattern.compile("^" + Pattern.quote(requiredPrefix) + "[\\w\\W]*"),
                            ""
                    );
                } else if (requiredPrefix != null) {
                    // Remove anything except messages that start with the prefix
                    defaultChannel.minecraftToDiscord.contentRegexFilters.put(
                            Pattern.compile("^(?!" + Pattern.quote(requiredPrefix) + ")[\\w\\W]*"),
                            ""
                    );
                    // Remove the prefix
                    defaultChannel.minecraftToDiscord.contentRegexFilters.put(
                            Pattern.compile("^" + Pattern.quote(requiredPrefix) + "([\\w\\W]*)"),
                            "$1"
                    );
                }
            }

            // Missing: DiscordChatChannelRolesAllowedToUseColorCodesInChat

            defaultChannel.discordToMinecraft.logToConsole = config.node("DiscordChatChannelBroadcastDiscordMessagesToConsole").getBoolean(true);

            // Missing: DiscordChatChannelRequireLinkedAccount

            defaultChannel.discordToMinecraft.ignores.bots = config.node("DiscordChatChannelBlockBots").getBoolean(false);
            defaultChannel.discordToMinecraft.ignores.webhooks = config.node("DiscordChatChannelWebhooks").getBoolean(true);
            defaultChannel.discordToMinecraft.ignores.userBotAndWebhookIds.whitelist = false;
            defaultChannel.discordToMinecraft.ignores.userBotAndWebhookIds.ids = config.node("DiscordChatChannelBlockedIds").getList(Long.class);
            defaultChannel.discordToMinecraft.ignores.roleIds.whitelist = config.node("DiscordChatChannelBlockedRolesAsWhitelist").getBoolean(false);
            defaultChannel.discordToMinecraft.ignores.roleIds.ids = config.node("DiscordChatChannelBlockedRolesIds").getList(Long.class);

            defaultChannel.roleSelection.blacklist = !config.node("DiscordChatChannelRolesSelectionAsWhitelist").getBoolean(false);
            defaultChannel.roleSelection.ids = config.node("DiscordChatChannelRolesSelection").getList(String.class, Collections.emptyList())
                    .stream().map(value -> {
                        try {
                            return MiscUtil.parseLong(value);
                        } catch (NumberFormatException ignored) {
                            return null;
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toList());

            // Missing: DiscordChatChannelRoleAliases

            // Missing: DiscordChatChannelGameFilters
            // Missing: DiscordChatChannelDiscordFilters

            defaultChannel.joinMessages().enabled = config.node("MinecraftPlayerJoinMessage").node("Enabled").getBoolean(true);
            JoinMessageConfig.FirstJoin firstJoinMessages = defaultChannel.joinMessages().firstJoin();
            if (firstJoinMessages != null) {
                firstJoinMessages.enabled = config.node("MinecraftPlayerFirstJoinMessage").node("Enabled").getBoolean(true);
            }

            defaultChannel.leaveMessages.enabled = config.node("MinecraftPlayerLeaveMessage").node("Enabled").getBoolean(true);
            if (defaultChannel instanceof ServerBaseChannelConfig) {
                ((ServerBaseChannelConfig)defaultChannel).deathMessages.enabled = config.node("MinecraftPlayerDeathMessage").node("Enabled").getBoolean(true);
                ((ServerBaseChannelConfig)defaultChannel).advancementMessages.enabled = config.node("MinecraftPlayerAchievementMessage").node("Enabled").getBoolean(true);
            }

            defaultChannel.startMessage.enabled = !config.node("DiscordChatChannelServerStartupMessage").getString("").isEmpty();
            defaultChannel.stopMessage.enabled = !config.node("DiscordChatChannelServerShutdownMessage").getString("").isEmpty();
        }

        List<Long> channelIds = new ArrayList<>();
        config.node("Channels").childrenMap().forEach((key, value) -> {
            String channelId = value.getString();
            if (!(key instanceof String) || channelId == null) {
                return;
            }

            long id = MiscUtil.parseLong(channelId);
            channelIds.add(id);

            ChannelConfig channelConfig = new ChannelConfig();
            channelConfig.destination.channelIds = Collections.singletonList(id);
            channelConfig.destination.threads = Collections.emptyList();
            mainConfig.channels.put((String) key, channelConfig);
        });

        String consoleChannelId = config.node("DiscordConsoleChannelId").getString("");
        if (!consoleChannelId.replace("0", "").isEmpty()) {
            ConsoleConfig consoleConfig = new ConsoleConfig();
            consoleConfig.channel.channelId = MiscUtil.parseLong(consoleChannelId);

            GameCommandExecutionConditionConfig condition = new GameCommandExecutionConditionConfig();
            consoleConfig.commandExecution.executionConditions.clear();
            condition.commands.addAll(config.node("DiscordConsoleChannelBlacklistedCommands").getList(String.class, Collections.emptyList()));
            condition.blacklist = !config.node("DiscordConsoleChannelBlacklistActsAsWhitelist").getBoolean(true);

            // Missing: DiscordConsoleChannelFilters

            consoleConfig.appender.levels.levels = config.node("DiscordConsoleChannelLevels").getList(String.class, Collections.emptyList())
                    .stream().map(level -> level.toUpperCase(Locale.ROOT)).collect(Collectors.toList());
            consoleConfig.appender.levels.blacklist = false;

            consoleConfig.appender.outputMode = config.node("DiscordConsoleChannelUseCodeBlocks").getBoolean()
                                                ? DiscordOutputMode.DIFF : DiscordOutputMode.PLAIN;

            mainConfig.console.clear();
            mainConfig.console.add(consoleConfig);
        }

        // Missing: DiscordChatChannelListCommandEnabled

        mainConfig.channelUpdater.textChannels.clear();
        // Missing: all topic options

        mainConfig.channelUpdater.voiceChannels.clear();
        // Missing: ChannelUpdater

        // Missing: DiscordCannedResponses

        // Missing: MinecraftDiscordAccountLinkedConsoleCommands
        // Missing: MinecraftDiscordAccountUnlinkedConsoleCommands

        try {
            long roleId = config.node("MinecraftDiscordAccountLinkedRoleNameToAddUserTo").getLong(0);
            if (roleId != 0) {
                mainConfig.linkedRole.roleIds.clear();
                mainConfig.linkedRole.roleIds.add(roleId);
            }
        } catch (NumberFormatException ignored) {}

        boolean bansToMinecraft = synchronization.node("BanSynchronizationDiscordToMinecraft").getBoolean(true);
        boolean bansToDiscord = synchronization.node("BanSynchronizationMinecraftToDiscord").getBoolean(true);
        SyncDirection banSyncDirection = SyncDirection.BIDIRECTIONAL;
        if (bansToMinecraft && !bansToDiscord) {
            banSyncDirection = SyncDirection.DISCORD_TO_MINECRAFT;
        } else if (!bansToMinecraft && bansToDiscord) {
            banSyncDirection = SyncDirection.MINECRAFT_TO_DISCORD;
        }

        mainConfig.banSync.direction = banSyncDirection;

        boolean groupSyncMinecraftIsTieBreaker = synchronization.node("GroupRoleSynchronizationMinecraftIsAuthoritative").getBoolean(true);
        boolean groupSyncOneWay = synchronization.node("GroupRoleSynchronizationOneWay").getBoolean(false);
        SyncDirection groupSyncDirection = groupSyncOneWay
                                           ? (groupSyncMinecraftIsTieBreaker ? SyncDirection.MINECRAFT_TO_DISCORD : SyncDirection.DISCORD_TO_MINECRAFT)
                                           : SyncDirection.BIDIRECTIONAL;
        int groupSyncCycleTime = synchronization.node("GroupRoleSynchronizationCycleTime").getInt();

        GroupSyncConfig.SetConfig groupSyncSet = new GroupSyncConfig.SetConfig();
        groupSyncSet.pairs.clear();
        groupSyncSet.direction = groupSyncDirection;
        groupSyncSet.timer.cycleTime = groupSyncCycleTime;

        mainConfig.groupSync.sets.clear();
        mainConfig.groupSync.sets.add(groupSyncSet);

        synchronization.node("GroupRoleSynchronizationGroupsAndRolesToSync").childrenMap().forEach((key, value) -> {
            String roleId = value.getString();
            if (!(key instanceof String) || roleId == null) {
                return;
            }

            GroupSyncConfig.PairConfig pairConfig = new GroupSyncConfig.PairConfig();
            pairConfig.groupName = (String) key;
            pairConfig.roleId = MiscUtil.parseLong(roleId);

            groupSyncSet.pairs.add(pairConfig);
        });

        // Missing: NicknameSynchronizationEnabled
        mainConfig.nicknameSync.timer.cycleTime = config.node("NicknameSynchronizationCycleTime").getInt(3);
        // Missing: NicknameSynchronizationFormat

        // Missing: Require linked account to play
    }

    public void migrate(ConnectionConfig connectionConfig) {
        connectionConfig.bot.token = config.node("BotToken").getString();

        if (!config.node("ProxyHost").getString("").endsWith("example.com")) {
            connectionConfig.httpProxy.enabled = true;
            connectionConfig.httpProxy.host = config.node("ProxyHost").getString();
            connectionConfig.httpProxy.port = config.node("ProxyPort").getInt();
            connectionConfig.httpProxy.basicAuth.enabled = true;
            connectionConfig.httpProxy.basicAuth.username = config.node("ProxyUser").getString();
            connectionConfig.httpProxy.basicAuth.password = config.node("ProxyPassword").getString();
        }
    }
}
