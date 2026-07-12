/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessageTemplate;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.config.main.ChannelUpdaterConfig;
import com.discordsrv.common.config.main.ConsoleConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.RewardsConfig;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.config.main.channels.JoinMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.command.CustomCommandConfig;
import com.discordsrv.common.config.main.generic.DiscordOutputMode;
import com.discordsrv.common.config.main.generic.DiscordUserFilterConfig;
import com.discordsrv.common.config.main.generic.FilterMode;
import com.discordsrv.common.config.main.generic.GameCommandExecutionConditionConfig;
import com.discordsrv.common.config.main.sync.GroupSyncConfig;
import com.discordsrv.common.core.storage.StorageType;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class V1ConfigMigration {

    /// Migration is split by Ascension configuration object, and ordered roughly in order.
    /// v1 options and their status are listed here.
    /// Most "messages" are skipped, other than some of the more prominent ones that are very often customized.
    /// voice, alerts and watchdog options are also skipped
    ///
    /// ** config.yml **
    /// Removed: ConfigVersion (replaced by automatic-configuration-upgrade and /discordsrv reload config_upgrade)
    ///
    ///     BotToken (Connections)
    ///
    ///     Channels
    ///
    ///     DiscordConsoleChannelId
    ///
    ///     DiscordInviteLink
    ///
    ///     Experiment_JdbcAccountLinkBackend (Connections)
    ///     Experiment_JdbcTablePrefix (Connections)
    ///     Experiment_JdbcUsername (Connections)
    ///     Experiment_JdbcPassword (Connections)
    ///
    ///     Experiment_WebhookChatMessageDelivery
    ///     Experiment_WebhookChatMessageFormat
    ///     Experiment_WebhookChatMessageUsernameFromDiscord
    ///     Experiment_WebhookChatMessageAvatarFromDiscord
    /// Missing: Experiment_WebhookChatMessageUsernameFilters
    ///
    /// Missing: AvatarUrl
    ///
    /// Removed: Experiment_MCDiscordReserializer_ToDiscord (default behaviour)
    /// Removed: Experiment_MCDiscordReserializer_ToMinecraft (default behaviour)
    /// Removed: Experiment_MCDiscordReserializer_InBroadcast (Replaced by dedicated command, /discordsrv broadcastd)
    ///
    /// Removed with no replacement: CancelConsoleCommandIfLoggingFailed
    ///     ForcedLanguage
    /// Removed with no replacement: ForceTLSv12
    /// Removed with no replacement: NoopHostnameVerifier
    /// Removed with no replacement: MaximumAttemptsForSystemDNSBeforeUsingFallbackDNS
    ///     TimestampFormat
    /// Migrated into placeholder: DateFormat
    /// Migrated into placeholder: Timezone
    /// Missing: MinecraftMentionSound
    ///
    ///     DisabledPluginHooks
    /// Removed with no replacement: VentureChatBungee
    /// Removed with no replacement: EnablePresenceInformation
    /// Removed with no replacement: UseModernPaperChatEvent (default behaviour)
    ///
    /// Missing: DiscordGameStatus
    /// Missing: DiscordOnlineStatus
    /// Missing: StatusUpdateRateInMinutes
    ///
    ///     DiscordChatChannelDiscordToMinecraft
    ///     DiscordChatChannelMinecraftToDiscord
    ///     DiscordChatChannelTruncateLength
    ///     DiscordChatChannelTranslateMentions
    ///     DiscordChatChannelAllowedMentions
    /// Partial: DiscordChatChannelEmojiBehavior ("name" missing)
    /// Missing: DiscordChatChannelEmoteBehavior
    ///     DiscordChatChannelPrefixRequiredToProcessMessage
    ///     DiscordChatChannelPrefixActsAsBlacklist
    ///     DiscordChatChannelRolesAllowedToUseColorCodesInChat
    ///     DiscordChatChannelBroadcastDiscordMessagesToConsole
    /// Missing: DiscordChatChannelRequireLinkedAccount
    ///     DiscordChatChannelBlockBots
    ///     DiscordChatChannelBlockWebhooks
    ///     DiscordChatChannelBlockedIds
    ///     DiscordChatChannelBlockedRolesAsWhitelist
    ///     DiscordChatChannelBlockedRolesIds
    ///     DiscordChatChannelRolesSelectionAsWhitelist
    ///     DiscordChatChannelRolesSelection
    /// Missing: DiscordChatChannelRoleAliases
    ///
    /// Removed with no replacement: DiscordConsoleChannelLogRefreshRateInSeconds
    /// Removed with no replacement: DiscordConsoleChannelUsageLog
    ///     DiscordConsoleChannelBlacklistActsAsWhitelist
    ///     DiscordConsoleChannelBlacklistedCommands
    ///     DiscordConsoleChannelFilters
    ///     DiscordConsoleChannelLevels
    ///     DiscordConsoleChannelUseCodeBlocks
    ///     DiscordConsoleChannelBlockBots
    ///
    /// Missing: DiscordChatChannelConsoleCommandEnabled
    /// Missing: DiscordChatChannelConsoleCommandNotifyErrors
    /// Missing: DiscordChatChannelConsoleCommandPrefix
    /// Missing: DiscordChatChannelConsoleCommandRolesAllowed
    /// Missing: DiscordChatChannelConsoleCommandWhitelist
    /// Missing: DiscordChatChannelConsoleCommandWhitelistBypassRoles
    /// Missing: DiscordChatChannelConsoleCommandWhitelistActsAsBlacklist
    /// Missing: DiscordChatChannelConsoleCommandExpiration
    /// Missing: DiscordChatChannelConsoleCommandExpirationDeleteRequest
    ///
    /// Missing: DiscordChatChannelListCommandEnabled
    /// Missing: DiscordChatChannelListCommandMessage
    /// Missing: DiscordChatChannelListCommandExpiration
    /// Missing: DiscordChatChannelListCommandExpirationDeleteRequest
    ///
    ///     DiscordChatChannelGameFilters
    ///     DiscordChatChannelDiscordFilters
    ///
    ///     ChannelTopicUpdaterChannelTopicsAtShutdownEnabled
    ///     ChannelTopicUpdaterRateInMinutes
    ///
    ///     ChannelUpdater
    ///     - ChannelId
    ///       Format
    ///       ShutdownFormat
    ///       UpdateInterval
    ///
    ///     DiscordCannedResponses
    ///
    ///     MinecraftDiscordAccountLinkedConsoleCommands
    ///     MinecraftDiscordAccountUnlinkedConsoleCommands
    ///     MinecraftDiscordAccountLinkedRoleNameToAddUserTo
    /// Removed with no replacement: MinecraftDiscordAccountLinkedAllowRelinkBySendingANewCode
    /// Removed with no replacement: MinecraftDiscordAccountLinkedUsePM
    /// Removed with no replacement: MinecraftDiscordAccountLinkedMessageDeleteSeconds
    ///
    /// * watchdog options skipped *
    ///
    ///     ProxyHost (Connections)
    ///     ProxyPort (Connections)
    ///     ProxyUser (Connections)
    ///     ProxyPassword (Connections)
    ///
    /// Removed: Debug (partially handled in the background by default, partially replaced by debug.log-to-console and debug.additional-levels)
    ///
    /// ** linking.yml **
    /// Missing: Require linked account to play.Enabled
    /// Missing: Require linked account to play.Listener priority
    /// Missing: Require linked account to play.Listener event
    /// Missing: Require linked account to play.Bypass names
    /// Missing: Require linked account to play.Whitelisted players bypass check
    /// Missing: Require linked account to play.Check banned players
    /// Missing: Require linked account to play.Only check banned players
    /// Missing: Require linked account to play.Not linked message
    /// Missing: Require linked account to play.Must be in Discord server
    /// Missing: Require linked account to play.Subscriber role.Require subscriber role to join
    /// Missing: Require linked account to play.Subscriber role.Subscriber roles
    /// Missing: Require linked account to play.Subscriber role.Require all of the listed roles
    /// Missing: Require linked account to play.Subscriber role.Kick message
    ///
    /// ** synchronization.yml **
    /// Missing: NicknameSynchronizationEnabled
    ///     NicknameSynchronizationCycleTime
    ///     NicknameSynchronizationFormat
    ///
    ///     GroupRoleSynchronizationGroupsAndRolesToSync
    ///     GroupRoleSynchronizationMinecraftIsAuthoritative
    ///     GroupRoleSynchronizationOneWay
    /// Removed with no replacement: GroupRoleSynchronizationEnableDenyPermission
    /// Removed with no replacement: GroupRoleSynchronizationPrimaryGroupOnly
    /// Missing: GroupRoleSynchronizationOnLink
    /// Removed with no replacement: GroupRoleSynchronizationCycleCompletely
    ///
    ///     BanSynchronizationDiscordToMinecraft
    /// Missing: BanSynchronizationDiscordToMinecraftReason
    ///     BanSynchronizationMinecraftToDiscord
    ///
    /// ** messages.yml **
    /// Missing: DiscordToMinecraftChatMessageFormat_<channel>
    /// Missing: DiscordToMinecraftChatMessageFormatNoRole_<channel>
    /// Missing: DiscordToMinecraftChatMessageFormat
    /// Missing: DiscordToMinecraftChatMessageFormatNoRole
    /// Migrated into placeholder: DiscordToMinecraftAllRolesSeparator
    /// Missing: DiscordToMinecraftMessageReplyFormat
    ///
    ///     MinecraftChatToDiscordMessageFormat
    ///     MinecraftChatToDiscordMessageFormatNoPrimaryGroup
    ///
    /// Missing: ChatChannelHookMessageFormat
    ///
    /// Missing: DynmapNameFormat
    /// Missing: DynmapChatFormat
    /// Missing: DynmapDiscordFormat
    ///
    /// Migrated into placeholder: DiscordConsoleChannelTimestampFormat
    /// Missing: DiscordConsoleChannelPrefix
    /// Missing: DiscordConsoleChannelSuffix
    /// Missing: DiscordConsoleChannelPadding
    ///
    /// * skip DiscordChatChannelConsoleCommandNotifyErrorsFormat *
    ///
    /// Missing: DiscordChatChannelListCommandFormatOnlinePlayers
    /// Missing: DiscordChatChannelListCommandFormatNoOnlinePlayers
    /// Missing: DiscordChatChannelListCommandPlayerFormat
    /// Missing: DiscordChatChannelListCommandAllPlayersSeparator
    ///
    /// Missing: MinecraftPlayerJoinMessage
    /// Missing: MinecraftPlayerFirstJoinMessage
    /// Missing: MinecraftPlayerLeaveMessage
    /// Missing: MinecraftPlayerDeathMessage
    /// Missing: MinecraftPlayerAchievementMessage
    ///
    ///     ChannelTopicUpdaterChatChannelTopicFormat
    ///     ChannelTopicUpdaterConsoleChannelTopicFormat
    ///     ChannelTopicUpdaterChatChannelTopicAtServerShutdownFormat
    ///     ChannelTopicUpdaterConsoleChannelTopicAtServerShutdownFormat
    ///
    /// Missing: DiscordCommandFormat
    ///
    /// * skip NoPermissionMessage *
    /// * skip UnknownCommandMessage *
    ///
    /// Missing: DiscordChatChannelServerStartupMessage
    /// Missing: DiscordChatChannelServerShutdownMessage
    ///
    /// * skip ServerWatchdogMessage *

    private enum PlaceholderField {
        /// DiscordGameStatus
        DISCORD_GAME_STATUS,
        /// ChannelUpdater
        CHANNEL_UPDATER,
        /// MinecraftDiscordAccountLinkedConsoleCommands / MinecraftDiscordAccountUnlinkedConsoleCommands
        MINECRAFT_DISCORD_ACCOUNT_LINKING,
        /// DiscordToMinecraftChatMessageFormat / DiscordToMinecraftChatMessageFormatNoRole
        DISCORD_TO_MINECRAFT_MESSAGE_FORMAT,
        /// DiscordToMinecraftMessageReplyFormat
        DISCORD_TO_MINECRAFT_REPLY_FORMAT,
        /// MinecraftChatToDiscordMessageFormat / MinecraftChatToDiscordMessageFormatNoPrimaryGroup
        MINECRAFT_TO_DISCORD_MESSAGE_FORMAT,
        /// ChatChannelHookMessageFormat
        CHAT_CHANNEL_HOOK_MESSAGE_FORMAT,
        /// DynmapDiscordFormat
        DYNMAP_DISCORD_FORMAT,
        /// DiscordConsoleChannelPrefix / DiscordConsoleChannelSuffix
        DISCORD_CONSOLE_CHANNEL_AFFIX,
        /// DiscordChatChannelListCommandPlayerFormat
        PLAYERLIST_COMMAND_FORMAT,
        /// MinecraftPlayerJoinMessage / MinecraftPlayerFirstJoinMessage
        JOIN_MESSAGE_FORMAT,
        /// MinecraftPlayerLeaveMessage
        LEAVE_MESSAGE_FORMAT,
        /// MinecraftPlayerDeathMessage
        DEATH_MESSAGE_FORMAT,
        /// MinecraftPlayerAchievementMessage
        ACHIEVEMENT_MESSAGE_FORMAT,
        /// ChannelTopicUpdaterChatChannelTopicFormat / ChannelTopicUpdaterConsoleChannelTopicFormat
        TOPIC_FORMAT,
        /// ChannelTopicUpdaterChatChannelTopicAtServerShutdownFormat / ChannelTopicUpdaterConsoleChannelTopicAtServerShutdownFormat
        TOPIC_SHUTDOWN_FORMAT,
        /// DiscordCommandFormat
        DISCORD_COMMAND_FORMAT;

        boolean isEmbedMessageFormat() {
            return this == JOIN_MESSAGE_FORMAT
                    || this == LEAVE_MESSAGE_FORMAT
                    || this == DEATH_MESSAGE_FORMAT
                    || this == ACHIEVEMENT_MESSAGE_FORMAT;
        }
    }

    private List<Pair<String, String>> makePlaceholderMapping(PlaceholderField placeholderField) {
        Map<String, String> placeholderMapping = new LinkedHashMap<>();

        String timestampFormat = config.node("TimestampFormat").getString();
        String rawTimezone = config.node("Timezone").getString("default");
        if (rawTimezone.equalsIgnoreCase("default")) {
            rawTimezone = null;
        }
        TimeZone timeZone = rawTimezone != null ? TimeZone.getTimeZone(rawTimezone) : null;

        String $TimeUtil_timeStamp = "%now_date"
                + (timeZone != null ? "_at_zone:'" + timeZone.getID() + "'" : "")
                + ":'" + timestampFormat + "'"
                + "%";

        String $PlayerUtil_getOnlinePlayers = "%playerlist_count%";

        if (placeholderField == PlaceholderField.DISCORD_GAME_STATUS) {
            placeholderMapping.put("%online%", $PlayerUtil_getOnlinePlayers);
        }

        if (placeholderField == PlaceholderField.CHANNEL_UPDATER || placeholderField == PlaceholderField.TOPIC_FORMAT) {
            placeholderMapping.put("%playercount%", $PlayerUtil_getOnlinePlayers);
            placeholderMapping.put("%playermax%", ""); // TODO
            placeholderMapping.put("%date%", $TimeUtil_timeStamp);
            placeholderMapping.put("%totalplayers%", ""); // TODO
            placeholderMapping.put("%uptimemins%", "%start_date_relative_to_now:'m'%");
            placeholderMapping.put("%uptimehours%", "%start_date_relative_to_now:'h'%");
            placeholderMapping.put("%motd%", ""); // TODO
            placeholderMapping.put("%serverversion%", ""); // TODO
            placeholderMapping.put("%freememory%", ""); // TODO
            placeholderMapping.put("%usedmemory%", ""); // TODO
            placeholderMapping.put("%totalmemory%", ""); // TODO
            placeholderMapping.put("%maxmemory%", ""); // TODO
            placeholderMapping.put("%freememorygb%", ""); // TODO
            placeholderMapping.put("%usedmemorygb%", ""); // TODO
            placeholderMapping.put("%totalmemorygb%", ""); // TODO
            placeholderMapping.put("%maxmemorygb%", ""); // TODO
            placeholderMapping.put("%tps%", ""); // TODO
            placeholderMapping.put("%time%", $TimeUtil_timeStamp);
            placeholderMapping.put("%timestamp%", "%now_date_to_epoch_seconds%");
        }

        if (placeholderField == PlaceholderField.MINECRAFT_DISCORD_ACCOUNT_LINKING) {
            placeholderMapping.put("%minecraftplayername%", "%profile_offline_player_name%");
            placeholderMapping.put("%minecraftuuid%", "%profile_player_uuid%");
            placeholderMapping.put("%discordid%", "%profile_user_id%");
            placeholderMapping.put("%discordname%", "%profile_user_name%");
            placeholderMapping.put("%discorddisplayname%", "%profile_user_effective_name%");
        }

        if (placeholderField == PlaceholderField.DISCORD_TO_MINECRAFT_MESSAGE_FORMAT) {
            String allRolesSeparator = config.node("DiscordToMinecraftAllRolesSeparator").getString();

            placeholderMapping.put("%allroles%", "%user_roles:'" + allRolesSeparator + "'%");
            // %message% remains the same
            placeholderMapping.put("%toprole%", recommendedUpgrades ? "%user_hoisted_role_name%" : "%user_highest_role_name%");
            placeholderMapping.put("%toprolealias%", ""); // TODO
            placeholderMapping.put("%toproleinitial%", recommendedUpgrades ? "%user_hoisted_role_name:'%.1s'%" : "%user_highest_role_name:'%.1s'%");
            placeholderMapping.put("%toprolecolor%", recommendedUpgrades ? "%user_color%" : "%user_highest_role_color%");
            placeholderMapping.put("%name%", "%user_effective_name%");
            placeholderMapping.put("%username%", "%user_name%");
            placeholderMapping.put("%userid%", "%user_id%");
            placeholderMapping.put("%channelname%", "%channel_name%");
            placeholderMapping.put("%reply%", "%message_reply%");
        }

        if (placeholderField == PlaceholderField.DISCORD_TO_MINECRAFT_REPLY_FORMAT) {
            placeholderMapping.put("%name%", "%user_effective_name%");
            placeholderMapping.put("%username%", "%user_name%");
            placeholderMapping.put("%userid%", "%user_id%");
            placeholderMapping.put("%message%", "%message%");
        }

        if (placeholderField == PlaceholderField.MINECRAFT_TO_DISCORD_MESSAGE_FORMAT) {
            placeholderMapping.put("%username%", "%player_name%");
            placeholderMapping.put("%displayname%", "%player_display_name%");
            // Escaping is handled without a need for another placeholder
            placeholderMapping.put("%usernamenoescapes%", "%player_name%");
            placeholderMapping.put("%displaynamenoescapes%", "%player_display_name%");

            // %message% stays the same
            placeholderMapping.put("%primarygroup%", ""); // TODO
            placeholderMapping.put("%world%", "%player_world_name%");
            placeholderMapping.put("%worldalias%", ""); // TODO
            placeholderMapping.put("%date%", $TimeUtil_timeStamp);
            placeholderMapping.put("%time%", $TimeUtil_timeStamp);
            placeholderMapping.put("%channelname%", "%channel_name%"); // TODO: first letter upper-case
        }

        // TODO: figure out how to determine if should be used
        if (placeholderField == PlaceholderField.CHAT_CHANNEL_HOOK_MESSAGE_FORMAT) {
            placeholderMapping.put("%channelcolor%", ""); // TODO
            placeholderMapping.put("%channelname%", ""); // TODO
            placeholderMapping.put("%channelnickname%", ""); // TODO
            placeholderMapping.put("%message%", ""); // TODO
        }

        if (placeholderField == PlaceholderField.DYNMAP_DISCORD_FORMAT) {
            placeholderMapping.put("%message%", ""); // TODO
            placeholderMapping.put("%name%", ""); // TODO
        }

        if (placeholderField == PlaceholderField.DISCORD_CONSOLE_CHANNEL_AFFIX) {
            int padding = Math.max(config.node("DiscordConsoleChannelPadding").getInt(0), 0);
            String consoleTimestampFormat = config.node("DiscordConsoleChannelTimestampFormat").getString();
            String timestamp = "%log_time"
                    + (timeZone != null ? "_at_zone:'" + timeZone.getID() + "'" : "")
                    + ":'" + consoleTimestampFormat + "'%";

            placeholderMapping.put("{level}", "%log_level%");
            placeholderMapping.put("{name}", "%logger_name:' \\%" + (padding > 0 ? "-" + padding : "") + "s'%");
            placeholderMapping.put("{datetime}", timestamp);
            placeholderMapping.put("{date}", timestamp);
        }

        if (placeholderField == PlaceholderField.PLAYERLIST_COMMAND_FORMAT) {
            placeholderMapping.put("%username%", "%player_name%");
            placeholderMapping.put("%displayname%", "%player_display_name%");
            placeholderMapping.put("%primarygroup%", ""); // TODO
            placeholderMapping.put("%world%", "%player_world_name%");
            placeholderMapping.put("%worldalias%", ""); // TODO
        }

        if (placeholderField.isEmbedMessageFormat()) {
            placeholderMapping.put("%displayname%", "%player_display_name%");
            placeholderMapping.put("%username%", "%player_name%");
            // Escaping is handled without a need for another placeholder
            placeholderMapping.put("%displaynamenoescapes%", "%player_display_name%");
            placeholderMapping.put("%usernamenoescapes%", "%player_name%");

            placeholderMapping.put("%date%", $TimeUtil_timeStamp);
            placeholderMapping.put("%time%", $TimeUtil_timeStamp);
            placeholderMapping.put("%embedavatarurl%", "%player_avatar_url%");
            placeholderMapping.put("%botavatarurl%", "%bot_user_effective_avatar_url%");
            placeholderMapping.put("%botname%", "%bot_user_effective_name%");
        }

        // %message% is kept as-is for join and leave
        if (placeholderField == PlaceholderField.DEATH_MESSAGE_FORMAT) {
            placeholderMapping.put("%deathmessage%", "%message%");
            placeholderMapping.put("%world%", "%player_world_name%");
        }
        if (placeholderField == PlaceholderField.ACHIEVEMENT_MESSAGE_FORMAT) {
            placeholderMapping.put("%achievement%", "%advancement_display_name%");
            placeholderMapping.put("%world%", "%player_world_name%");
        }

        if (placeholderField == PlaceholderField.TOPIC_SHUTDOWN_FORMAT) {
            placeholderMapping.put("%totalplayers%", ""); // TODO
            placeholderMapping.put("%serverversion%", ""); // TODO
            placeholderMapping.put("%date%", $TimeUtil_timeStamp);
            placeholderMapping.put("%time%", $TimeUtil_timeStamp);
        }

        if (placeholderField == PlaceholderField.DISCORD_COMMAND_FORMAT) {
            placeholderMapping.put("{INVITE}", "%discord_invite%");
        }

        return placeholderMapping
                .entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
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

    private final DiscordSRV discordSRV;
    private final ConfigurationNode config;
    private final ConfigurationNode messages;
    private final ConfigurationNode linking;
    private final ConfigurationNode synchronization;

    /**
     * If some options shouldn't be carried over in favour of better defaults.
     * {@code false} will attempt to migrate everything as closely as possible.
     */
    private final boolean recommendedUpgrades;

    public V1ConfigMigration(DiscordSRV discordSRV, boolean recommendedUpgrades) {
        this.discordSRV = discordSRV;
        this.recommendedUpgrades = recommendedUpgrades;
        this.config = loadNode(discordSRV, "config.yml");
        this.messages = loadNode(discordSRV, "messages.yml");
        this.linking = loadNode(discordSRV, "linking.yml");
        this.synchronization = loadNode(discordSRV, "synchronization.yml");
    }

    @Nullable
    private Long convertRoleNameToId(String roleName) {
        try {
            return MiscUtil.parseLong(roleName);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long convertRoleNameToId(ConfigurationNode node) {
        String roleName = node.getString();
        if (roleName == null) {
            return null;
        }

        return convertRoleNameToId(roleName);
    }

    private List<Long> convertRoleNameListToIds(ConfigurationNode node) throws SerializationException {
        return node
                .getList(String.class, Collections.emptyList())
                .stream()
                .map(this::convertRoleNameToId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<Pattern, String> convertToRegexReplacements(ConfigurationNode node) throws SerializationException {
        Map<Pattern, String> patterns = new HashMap<>();
        node.childrenMap().forEach((key, value) -> patterns.put(Pattern.compile((String) key), value.getString()));
        return patterns;
    }

    public void migrate(MainConfig mainConfig) throws SerializationException {
        mainConfig.channels.remove("global");
        BaseChannelConfig defaultChannel = mainConfig.channels.get("default");
        if (defaultChannel != null) {
            defaultChannel.discordToMinecraft.enabled = config.node("DiscordChatChannelDiscordToMinecraft").getBoolean(true);
            defaultChannel.minecraftToDiscord.enabled = config.node("DiscordChatChannelMinecraftToDiscord").getBoolean(true);

            Map<Pattern, String> oldDiscordToMinecraftFilters = convertToRegexReplacements(config.node("DiscordChatChannelDiscordFilters"));
            if (recommendedUpgrades) {
                defaultChannel.discordToMinecraft.contentRegexFilters.putAll(oldDiscordToMinecraftFilters);
            } else {
                defaultChannel.discordToMinecraft.contentRegexFilters = oldDiscordToMinecraftFilters;
            }

            if (!recommendedUpgrades) {
                boolean useWebhookDelivery = config.node("Experiment_WebhookChatMessageDelivery").getBoolean();
                if (useWebhookDelivery) {
                    String username = config.node("Experiment_WebhookChatMessageUsernameFromDiscord").getBoolean()
                                      ? "%player_linked_user_effective_name|user_effective_name%"
                                      : config.node("Experiment_WebhookChatMessageUsernameFormat").getString(); // TODO: placeholders

                    String avatarUrl = config.node("Experiment_WebhookChatMessageAvatarFromDiscord").getBoolean()
                                       ? "%player_linked_user_effective_avatar_url|player_avatar_url%"
                                       : "%player_avatar_url%";

                    defaultChannel.minecraftToDiscord.format = new SendableDiscordMessageTemplate(
                            SendableDiscordMessage.builder()
                                    .setWebhookUsername(username)
                                    .setWebhookAvatarUrl(avatarUrl)
                                    .setContent(config.node("Experiment_WebhookChatMessageFormat").getString()) // TODO: placeholders
                    );
                } else {
                    String format = messages.node("MinecraftChatToDiscordMessageFormat").getString();
                    String noPrimaryGroupFormat = messages.node("MinecraftChatToDiscordMessageFormatNoPrimaryGroup").getString();

                    if ("**%primarygroup%** %displayname% » %message%".equals(format)
                            && "%displayname% » %message%".equals(noPrimaryGroupFormat)) {
                        // TODO: primary group?
                        defaultChannel.minecraftToDiscord.format = new SendableDiscordMessageTemplate(
                                SendableDiscordMessage.builder().setContent("%player_display_name% » %message%")
                        );
                    } else {
                        defaultChannel.minecraftToDiscord.format = new SendableDiscordMessageTemplate(
                                SendableDiscordMessage.builder().setContent(noPrimaryGroupFormat) // TODO: placeholders
                        );
                    }
                }

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

            Map<Pattern, String> oldMinecraftToDiscordFilters = convertToRegexReplacements(config.node("DiscordChatChannelGameFilters"));
            if (recommendedUpgrades) {
                defaultChannel.minecraftToDiscord.contentRegexFilters.putAll(oldMinecraftToDiscordFilters);
            } else {
                defaultChannel.minecraftToDiscord.contentRegexFilters = oldMinecraftToDiscordFilters;
            }

            if (!recommendedUpgrades) {
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

            defaultChannel.discordToMinecraft.formattingLimit.filters = convertRoleNameListToIds(config.node("DiscordChatChannelRolesAllowedToUseColorCodesInChat"))
                    .stream()
                    .map(roleId -> new DiscordUserFilterConfig.SingleFilter(roleId, FilterMode.WHITELIST))
                    .collect(Collectors.toList());

            defaultChannel.discordToMinecraft.logToConsole = config.node("DiscordChatChannelBroadcastDiscordMessagesToConsole").getBoolean(true);

            defaultChannel.discordToMinecraft.ignores.bots = config.node("DiscordChatChannelBlockBots").getBoolean(false)
                                                             ? FilterMode.BLACKLIST : FilterMode.WHITELIST;
            defaultChannel.discordToMinecraft.ignores.webhooks = config.node("DiscordChatChannelBlockWebhooks").getBoolean(true)
                                                                 ? FilterMode.BLACKLIST : FilterMode.WHITELIST;
            for (Long blockedId : config.node("DiscordChatChannelBlockedIds").getList(Long.class, Collections.emptyList())) {
                defaultChannel.discordToMinecraft.ignores.filters
                        .add(new DiscordUserFilterConfig.SingleFilter(blockedId, FilterMode.BLACKLIST));
            }
            FilterMode discordToMinecraftIgnoreRoleFilterMode = config.node("DiscordChatChannelBlockedRolesAsWhitelist").getBoolean(false)
                                                                ? FilterMode.WHITELIST : FilterMode.BLACKLIST;
            for (Long roleId : config.node("DiscordChatChannelBlockedRolesIds").getList(Long.class, Collections.emptyList())) {
                defaultChannel.discordToMinecraft.ignores.filters
                        .add(new DiscordUserFilterConfig.SingleFilter(roleId, discordToMinecraftIgnoreRoleFilterMode));
            }

            defaultChannel.roleSelection.blacklist = !config.node("DiscordChatChannelRolesSelectionAsWhitelist").getBoolean(false);
            defaultChannel.roleSelection.ids = convertRoleNameListToIds(config.node("DiscordChatChannelRolesSelection"));

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

        List<Long> consoleChannelIds = new ArrayList<>();
        String consoleChannelId = config.node("DiscordConsoleChannelId").getString("");
        if (!consoleChannelId.replace("0", "").isEmpty()) {
            ConsoleConfig consoleConfig = new ConsoleConfig();
            consoleConfig.channel.channelId = MiscUtil.parseLong(consoleChannelId);
            consoleChannelIds.add(consoleConfig.channel.channelId);

            GameCommandExecutionConditionConfig condition = new GameCommandExecutionConditionConfig();
            consoleConfig.commandExecution.executionConditions.clear();
            condition.commands.addAll(config.node("DiscordConsoleChannelBlacklistedCommands").getList(String.class, Collections.emptyList()));
            condition.filterMode = config.node("DiscordConsoleChannelBlacklistActsAsWhitelist").getBoolean(true)
                                   ? FilterMode.WHITELIST : FilterMode.BLACKLIST;

            Map<Pattern, String> oldConsoleFilters = convertToRegexReplacements(config.node("DiscordConsoleChannelFilters"));
            if (recommendedUpgrades) {
                consoleConfig.appender.contentRegexFilters.putAll(oldConsoleFilters);
            } else {
                consoleConfig.appender.contentRegexFilters = oldConsoleFilters;
            }

            consoleConfig.appender.levels.levels = config.node("DiscordConsoleChannelLevels").getList(String.class, Collections.emptyList())
                    .stream().map(level -> level.toUpperCase(Locale.ROOT)).collect(Collectors.toList());
            consoleConfig.appender.levels.blacklist = false;

            if (!recommendedUpgrades) {
                consoleConfig.appender.outputMode = config.node("DiscordConsoleChannelUseCodeBlocks").getBoolean()
                                                    ? DiscordOutputMode.DIFF : DiscordOutputMode.PLAIN;
            }

            // Removed with no replacement: DiscordConsoleChannelBlockBots (Users/roles need to be specifically whitelisted)

            mainConfig.console.clear();
            mainConfig.console.add(consoleConfig);
        }

        String inviteLink = config.node("DiscordInviteLink").getString();
        if (!StringUtils.isEmpty(inviteLink) && !inviteLink.contains("/changethisin")) {
            mainConfig.invite.inviteUrl = inviteLink;
        }

        mainConfig.messages.defaultLanguage = config.node("ForcedLanguage").getString(); // TODO: save configs in this language

        mainConfig.integrations.disabledIntegrations = config.node("DisabledPluginHooks").getList(String.class, Collections.emptyList());

        if (!recommendedUpgrades) {
            mainConfig.channelUpdater.textChannels.clear();
            boolean updateAtShutdown = config.node("ChannelTopicUpdaterChannelTopicsAtShutdownEnabled").getBoolean(true);

            String channelTopicFormat = messages.node("ChannelTopicUpdaterChatChannelTopicFormat").getString();
            if (!StringUtils.isEmpty(channelTopicFormat)) {
                ChannelUpdaterConfig.TextChannelConfig updaterConfig = new ChannelUpdaterConfig.TextChannelConfig();
                updaterConfig.channelIds = channelIds;
                updaterConfig.nameFormat = channelTopicFormat; // TODO: placeholders
                updaterConfig.shutdownNameFormat = updateAtShutdown
                                                   ? messages.node("ChannelTopicUpdaterChatChannelTopicAtServerShutdownFormat").getString() // TODO: placeholders
                                                   : "";
                updaterConfig.timeMinutes = config.node("ChannelTopicUpdaterRateInMinutes").getInt();

                mainConfig.channelUpdater.textChannels.add(updaterConfig);
            }

            String consoleTopicFormat = messages.node("ChannelTopicUpdaterConsoleChannelTopicFormat").getString();
            if (!StringUtils.isEmpty(consoleTopicFormat)) {
                ChannelUpdaterConfig.TextChannelConfig updaterConfig = new ChannelUpdaterConfig.TextChannelConfig();
                updaterConfig.channelIds = consoleChannelIds;
                updaterConfig.nameFormat = consoleTopicFormat; // TODO: placeholders
                updaterConfig.shutdownNameFormat = updateAtShutdown
                                                   ? messages.node("ChannelTopicUpdaterConsoleChannelTopicAtServerShutdownFormat").getString() // TODO: placeholders
                                                   : "";
            }
        }

        mainConfig.channelUpdater.voiceChannels.clear();
        for (ConfigurationNode channelUpdater : config.node("ChannelUpdater").childrenList()) {
            // TODO: ignore defaults
            ChannelUpdaterConfig.VoiceChannelConfig updaterConfig = new ChannelUpdaterConfig.VoiceChannelConfig();
            updaterConfig.channelIds = Collections.singletonList(channelUpdater.node("ChannelId").getLong());
            updaterConfig.nameFormat = channelUpdater.node("Format").getString(); // TODO: placeholders
            updaterConfig.shutdownNameFormat = channelUpdater.node("ShutdownFormat").getString(); // TODO: placeholders
            updaterConfig.timeMinutes = channelUpdater.node("UpdateInterval").getInt();
            mainConfig.channelUpdater.voiceChannels.add(updaterConfig);
        }

        // TODO: ignore defaults if recommendedUpgradePath = false
        mainConfig.customCommands.clear();
        config.node("DiscordCannedResponses").childrenMap()
                .forEach((key, value) -> {
                    Matcher triggerMatcher = DiscordCommand.CHAT_INPUT_NAME_PATTERN.matcher((String) key);
                    if (!triggerMatcher.find()) {
                        return;
                    }

                    CustomCommandConfig customCommandConfig = new CustomCommandConfig();
                    customCommandConfig.enabled = true;
                    customCommandConfig.command = triggerMatcher.group();
                    customCommandConfig.ephemeral = !recommendedUpgrades;
                    customCommandConfig.response = new SendableDiscordMessageTemplate(
                            SendableDiscordMessage.builder()
                                    .setContent(value.getString())
                    );

                    mainConfig.customCommands.add(customCommandConfig);
                });

        mainConfig.rewards.linkingRewards.clear();
        List<String> linkingCommands = config.node("MinecraftDiscordAccountLinkedConsoleCommands").getList(String.class, Collections.emptyList());
        if (!linkingCommands.isEmpty()) {
            RewardsConfig.LinkingReward linkingReward = new RewardsConfig.LinkingReward();
            linkingReward.rewardId = "v1migrated-link";
            linkingReward.type = RewardsConfig.LinkingReward.Type.LINKED;
            linkingReward.grantType = recommendedUpgrades ? RewardsConfig.GrantType.ONCE_PER_BOTH : RewardsConfig.GrantType.ALWAYS;
            linkingReward.consoleCommandsToRun = linkingCommands; // TODO: placeholders
            mainConfig.rewards.linkingRewards.add(linkingReward);
        }

        List<String> unlinkingCommands = config.node("MinecraftDiscordAccountUnlinkedConsoleCommands").getList(String.class, Collections.emptyList());
        if (!unlinkingCommands.isEmpty()) {
            RewardsConfig.LinkingReward linkingReward = new RewardsConfig.LinkingReward();
            linkingReward.rewardId = "v1migrated-unlink";
            linkingReward.type = RewardsConfig.LinkingReward.Type.UNLINKED;
            linkingReward.grantType = recommendedUpgrades ? RewardsConfig.GrantType.ONCE_PER_BOTH : RewardsConfig.GrantType.ALWAYS;
            linkingReward.consoleCommandsToRun = linkingCommands; // TODO: placeholders
            mainConfig.rewards.linkingRewards.add(linkingReward);
        }

        Long linkedRoleId = convertRoleNameToId(config.node("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"));
        if (linkedRoleId != null) {
            mainConfig.linkedRole.roleIds.clear();
            mainConfig.linkedRole.roleIds.add(linkedRoleId);
        }

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

        mainConfig.nicknameSync.timer.cycleTime = synchronization.node("NicknameSynchronizationCycleTime").getInt(3);
        mainConfig.nicknameSync.format = synchronization.node("NicknameSynchronizationFormat").getString(); // TODO: placeholders
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

        migrateJDBC(connectionConfig.storage);

        connectionConfig.update.notificationEnabled = !config.node("UpdateCheckDisabled", false).getBoolean();

        String httpProxyHost = config.node("ProxyHost").getString();
        if (!StringUtils.isEmpty(httpProxyHost) && !httpProxyHost.equals("example.com")) {
            connectionConfig.httpProxy.enabled = true;
            connectionConfig.httpProxy.host = httpProxyHost;
            connectionConfig.httpProxy.port = config.node("ProxyPort").getInt();

            String httpProxyUsername = config.node("ProxyUser").getString();
            if (!StringUtils.isEmpty(httpProxyUsername)) {
                connectionConfig.httpProxy.basicAuth.enabled = true;
                connectionConfig.httpProxy.basicAuth.username = httpProxyUsername;
                connectionConfig.httpProxy.basicAuth.password = config.node("ProxyPassword").getString();
            }
        }
    }

    private void migrateJDBC(StorageConfig storageConfig) {
        String jdbcUrl = config.node("Experiment_JdbcAccountLinkBackend").getString();
        if (StringUtils.isEmpty(jdbcUrl) || jdbcUrl.contains("://HOST:PORT")) {
            return;
        }

        Pattern jdbcPattern = Pattern.compile("jdbc:(mysql|mariadb)://([^/]+)/([^/?]+)\\?(.*)");
        Matcher jdbcMatcher = jdbcPattern.matcher(jdbcUrl);
        if (!jdbcMatcher.matches()) {
            return;
        }

        String driver = jdbcMatcher.group(1);
        switch (driver) {
            case "mysql":
                storageConfig.backend = StorageType.MYSQL;
                break;
            case "mariadb":
                storageConfig.backend = StorageType.MARIADB;
                break;
            default:
                return;
        }

        storageConfig.remote.databaseAddress = jdbcMatcher.group(2);
        storageConfig.remote.databaseName = jdbcMatcher.group(3);
        storageConfig.remote.username = config.node("Experiment_JdbcUsername").getString();
        storageConfig.remote.username = config.node("Experiment_JdbcPassword").getString();

        storageConfig.driverProperties.clear();
        for (String properties : jdbcMatcher.group(4).split("&")) {
            String[] property = properties.split("=", 2);
            if (property.length != 2) {
                continue;
            }

            storageConfig.driverProperties.put(property[0], property[1]);
        }

        storageConfig.sqlTablePrefix = config.node("Experiment_JdbcTablePrefix").getString();
    }
}
