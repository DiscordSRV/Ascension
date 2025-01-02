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
import com.discordsrv.common.config.main.GroupSyncConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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

    public void migrate(MainConfig mainConfig) throws SerializationException {
        mainConfig.channels.remove("global");
        BaseChannelConfig defaultChannel = mainConfig.channels.get("default");
        if (defaultChannel != null) {
            defaultChannel.discordToMinecraft.enabled = config.node("DiscordChatChannelDiscordToMinecraft").getBoolean(true);

            String emojiBehaviour = config.node("DiscordChatChannelEmojiBehavior").getString();
            if ("show".equalsIgnoreCase(emojiBehaviour)) {
                defaultChannel.discordToMinecraft.unicodeEmojiBehaviour = DiscordToMinecraftChatConfig.EmojiBehaviour.SHOW;
            } else if ("hide".equalsIgnoreCase(emojiBehaviour)) {
                defaultChannel.discordToMinecraft.unicodeEmojiBehaviour = DiscordToMinecraftChatConfig.EmojiBehaviour.HIDE;
            }

            defaultChannel.discordToMinecraft.ignores.bots = config.node("DiscordChatChannelBlockBots").getBoolean(false);
            defaultChannel.discordToMinecraft.ignores.webhooks = config.node("DiscordChatChannelWebhooks").getBoolean(true);
            defaultChannel.discordToMinecraft.ignores.userBotAndWebhookIds.whitelist = false;
            defaultChannel.discordToMinecraft.ignores.userBotAndWebhookIds.ids = config.node("DiscordChatChannelBlockedIds").getList(Long.class);
            defaultChannel.discordToMinecraft.ignores.roleIds.whitelist = config.node("DiscordChatChannelBlockedRolesAsWhitelist").getBoolean(false);
            defaultChannel.discordToMinecraft.ignores.roleIds.ids = config.node("DiscordChatChannelBlockedRolesIds").getList(Long.class);

            defaultChannel.minecraftToDiscord.enabled = config.node("DiscordChatChannelMinecraftToDiscord").getBoolean(true);
            List<String> allowedMentions = config.node("DiscordChatChannelAllowedMentions").getList(String.class, Collections.emptyList());
            defaultChannel.minecraftToDiscord.mentions.channels = allowedMentions.contains("channel");
            defaultChannel.minecraftToDiscord.mentions.everyone = allowedMentions.contains("everyone");
            defaultChannel.minecraftToDiscord.mentions.roles = allowedMentions.contains("role");
            defaultChannel.minecraftToDiscord.mentions.users = allowedMentions.contains("user");
        }

        config.node("Channels").childrenMap().forEach((key, value) -> {
            String channelId = value.getString();
            if (!(key instanceof String) || channelId == null) {
                return;
            }

            ChannelConfig channelConfig = new ChannelConfig();
            channelConfig.destination.channelIds = Collections.singletonList(Long.parseUnsignedLong(channelId));
            channelConfig.destination.threads = Collections.emptyList();
            mainConfig.channels.put((String) key, channelConfig);
        });

        String consoleChannelId = config.node("DiscordConsoleChannelId").getString("");
        if (!consoleChannelId.replace("0", "").isEmpty()) {
            ConsoleConfig consoleConfig = new ConsoleConfig();
            consoleConfig.channel.channelId = Long.parseUnsignedLong(consoleChannelId);
            consoleConfig.appender.outputMode = config.node("DiscordConsoleChannelUseCodeBlocks").getBoolean() ? ConsoleConfig.OutputMode.DIFF : ConsoleConfig.OutputMode.PLAIN_CONTENT;
            consoleConfig.appender.levels.levels = config.node("DiscordConsoleChannelLevels").getList(String.class, Collections.emptyList())
                    .stream().map(level -> level.toUpperCase(Locale.ROOT)).collect(Collectors.toList());
            consoleConfig.appender.levels.blacklist = false;

            mainConfig.console.clear();
            mainConfig.console.add(consoleConfig);
        }

        boolean toMinecraft = synchronization.node("BanSynchronizationDiscordToMinecraft").getBoolean(true);
        boolean toDiscord = synchronization.node("BanSynchronizationMinecraftToDiscord").getBoolean(true);
        SyncDirection banSyncDirection = SyncDirection.BIDIRECTIONAL;
        if (toMinecraft && !toDiscord) {
            banSyncDirection = SyncDirection.DISCORD_TO_MINECRAFT;
        } else if (!toMinecraft && toDiscord) {
            banSyncDirection = SyncDirection.MINECRAFT_TO_DISCORD;
        }

        mainConfig.banSync.direction = banSyncDirection;


        boolean minecraftIsTieBreaker = synchronization.node("GroupRoleSynchronizationMinecraftIsAuthoritative").getBoolean(true);
        boolean oneWay = synchronization.node("GroupRoleSynchronizationOneWay").getBoolean(false);
        SyncDirection groupSyncDirection = oneWay
                                           ? (minecraftIsTieBreaker ? SyncDirection.MINECRAFT_TO_DISCORD : SyncDirection.DISCORD_TO_MINECRAFT)
                                           : SyncDirection.BIDIRECTIONAL;
        int groupSyncCycleTime = synchronization.node("GroupRoleSynchronizationCycleTime").getInt();

        mainConfig.groupSync.pairs.clear();
        synchronization.node("GroupRoleSynchronizationGroupsAndRolesToSync").childrenMap().forEach((key, value) -> {
            String roleId = value.getString();
            if (!(key instanceof String) || roleId == null) {
                return;
            }

            GroupSyncConfig.PairConfig pairConfig = new GroupSyncConfig.PairConfig();
            pairConfig.roleId = Long.parseUnsignedLong(roleId);
            pairConfig.groupName = (String) key;
            pairConfig.direction = groupSyncDirection;
            pairConfig.timer.cycleTime = groupSyncCycleTime;

            mainConfig.groupSync.pairs.add(pairConfig);
        });
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
