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

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessageTemplate;
import com.discordsrv.api.placeholder.util.PlaceholderReplacer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.abstraction.sync.enums.SyncSide;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.config.main.*;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.config.main.channels.JoinMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.command.CustomCommandConfig;
import com.discordsrv.common.config.main.generic.*;
import com.discordsrv.common.config.main.sync.GroupSyncConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.storage.StorageType;
import net.dv8tion.jda.api.OnlineStatus;
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
import java.util.stream.Stream;

public class V1ConfigMigration {

    /// Migration is split by Ascension configuration object, and ordered roughly in order (of the Ascension configs, not v1's).
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
    ///     DiscordGameStatus
    ///     DiscordOnlineStatus
    ///     StatusUpdateRateInMinutes
    ///
    ///     DiscordChatChannelDiscordToMinecraft
    ///     DiscordChatChannelMinecraftToDiscord
    ///     DiscordChatChannelTruncateLength
    ///     DiscordChatChannelTranslateMentions
    ///     DiscordChatChannelAllowedMentions
    /// Partial: DiscordChatChannelEmojiBehavior ("name" missing)
    ///     DiscordChatChannelEmoteBehavior
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
    ///     DiscordChatChannelConsoleCommandEnabled
    /// Removed with no replacement: DiscordChatChannelConsoleCommandNotifyErrors
    /// Removed with no replacement: DiscordChatChannelConsoleCommandPrefix
    ///     DiscordChatChannelConsoleCommandRolesAllowed
    ///     DiscordChatChannelConsoleCommandWhitelist
    ///     DiscordChatChannelConsoleCommandWhitelistBypassRoles
    ///     DiscordChatChannelConsoleCommandWhitelistActsAsBlacklist
    /// Migrated to ephemeral option: DiscordChatChannelConsoleCommandExpiration
    /// Migrated to ephemeral option: DiscordChatChannelConsoleCommandExpirationDeleteRequest
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
    ///     GroupRoleSynchronizationOnLink
    /// Removed with no replacement: GroupRoleSynchronizationCycleCompletely
    ///
    ///     BanSynchronizationDiscordToMinecraft
    /// Missing: BanSynchronizationDiscordToMinecraftReason
    ///     BanSynchronizationMinecraftToDiscord
    ///
    /// ** messages.yml **
    ///     DiscordToMinecraftChatMessageFormat_<channel>
    /// Removed with no replacement: DiscordToMinecraftChatMessageFormatNoRole_<channel>
    ///     DiscordToMinecraftChatMessageFormat
    /// Removed with no replacement: DiscordToMinecraftChatMessageFormatNoRole
    /// Migrated into placeholder: DiscordToMinecraftAllRolesSeparator
    ///     DiscordToMinecraftMessageReplyFormat
    ///
    ///     MinecraftChatToDiscordMessageFormat
    ///     MinecraftChatToDiscordMessageFormatNoPrimaryGroup
    ///
    ///     ChatChannelHookMessageFormat
    ///
    /// Missing: DynmapNameFormat
    /// Missing: DynmapChatFormat
    /// Missing: DynmapDiscordFormat
    ///
    /// Migrated into placeholder: DiscordConsoleChannelTimestampFormat
    /// Missing: DiscordConsoleChannelPrefix
    /// Missing: DiscordConsoleChannelSuffix
    ///     DiscordConsoleChannelPadding
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

    private final Set<String> placeholderAPISuggestionsDone = new HashSet<>();

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final ConfigurationNode config;
    private final ConfigurationNode messages;
    private final ConfigurationNode linking;
    private final ConfigurationNode synchronization;

    private final List<String> disabledIntegrations;

    /**
     * If some options shouldn't be carried over in favour of better defaults.
     * {@code false} will attempt to migrate everything as closely as possible.
     */
    private final boolean recommendedUpgrades;

    public V1ConfigMigration(DiscordSRV discordSRV, boolean recommendedUpgrades) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "V1_CONFIG_MIGRATION");
        this.config = loadNode(discordSRV, "config.yml");
        this.messages = loadNode(discordSRV, "messages.yml");
        this.linking = loadNode(discordSRV, "linking.yml");
        this.synchronization = loadNode(discordSRV, "synchronization.yml");

        List<String> disabledIntegrations;
        try {
            disabledIntegrations = config == null
                                   ? Collections.emptyList()
                                   : config.node("DisabledPluginHooks").getList(String.class, Collections.emptyList());
        } catch (ConfigurateException e) {
            disabledIntegrations = Collections.emptyList();
        }
        this.disabledIntegrations = disabledIntegrations;

        this.recommendedUpgrades = recommendedUpgrades;
    }

    private boolean isIntegrationEnabled(String pluginName) {
        return disabledIntegrations.stream().noneMatch(pluginName::equalsIgnoreCase)
                && discordSRV.pluginManager().isPluginEnabled(pluginName);
    }

    private enum PlaceholderField {
        /// DiscordGameStatus
        DISCORD_GAME_STATUS,
        /// Experiment_WebhookChatMessageUsernameFormat
        WEBHOOK_USERNAME_FORMAT,
        /// Experiment_WebhookChatMessageFormat
        WEBHOOK_MESSAGE_FORMAT,
        /// ChannelUpdater.Format
        CHANNEL_UPDATER_FORMAT,
        /// ChannelUpdater.ShutdownFormat
        CHANNEL_UPDATER_SHUTDOWN_FORMAT,
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
        DISCORD_COMMAND_FORMAT,
        /// NicknameSynchronizationFormat
        NICKNAME_FORMAT,
        ;

        boolean isEmbedMessageFormat() {
            return this == JOIN_MESSAGE_FORMAT
                    || this == LEAVE_MESSAGE_FORMAT
                    || this == DEATH_MESSAGE_FORMAT
                    || this == ACHIEVEMENT_MESSAGE_FORMAT;
        }
    }

    private static class PlaceholderAPIReplacement {

        private final String placeholderAPIPlaceholder;
        private final String expansion;
        private final boolean integratedExpansion;

        public PlaceholderAPIReplacement(String placeholderAPIPlaceholder, String expansion, boolean integratedExpansion) {
            this.placeholderAPIPlaceholder = placeholderAPIPlaceholder;
            this.expansion = expansion;
            this.integratedExpansion = integratedExpansion;
        }

        public String getPlaceholderAPIPlaceholder() {
            return placeholderAPIPlaceholder;
        }

        public String getExpansion() {
            return expansion;
        }

        public boolean isIntegratedExpansion() {
            return integratedExpansion;
        }

        @Override
        public String toString() {
            return getPlaceholderAPIPlaceholder();
        }
    }

    private List<Pair<Pattern, Object>> makePlaceholderMapping(PlaceholderField placeholderField) {
        Map<Object, Object> placeholderMapping = new LinkedHashMap<>();

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

        String $PlayerUtil_getOnlinePlayers_size = "%playerlist_count%";

        PlaceholderAPIReplacement $MultiverseCore_worldAlias = new PlaceholderAPIReplacement("%multiverse-core_alias%", "Multiverse-Core", true);
        PlaceholderAPIReplacement $Server_version = new PlaceholderAPIReplacement("%server_version%", "Server", false);
        PlaceholderAPIReplacement $Server_maxPlayers = new PlaceholderAPIReplacement("%server_max_players%", "Server", false);
        PlaceholderAPIReplacement $Server_uniqueJoins = new PlaceholderAPIReplacement("%server_unique_joins%", "Server", false);
        PlaceholderAPIReplacement $Spark_tps10s = new PlaceholderAPIReplacement("%spark_tps_10s%", "spark", true);

        if (placeholderField == PlaceholderField.DISCORD_GAME_STATUS) {
            placeholderMapping.put("%online%", $PlayerUtil_getOnlinePlayers_size);
        }

        if (placeholderField == PlaceholderField.WEBHOOK_USERNAME_FORMAT || placeholderField == PlaceholderField.WEBHOOK_MESSAGE_FORMAT) {
            placeholderMapping.put("%displayname%", "%player_display_name%");
            placeholderMapping.put("%username%", "%player_name%");
            // For the message format: %message% is kept as is
        }

        if (placeholderField == PlaceholderField.CHANNEL_UPDATER_FORMAT || placeholderField == PlaceholderField.TOPIC_FORMAT) {
            placeholderMapping.put("%playercount%", $PlayerUtil_getOnlinePlayers_size);
            placeholderMapping.put("%playermax%", $Server_maxPlayers);
            placeholderMapping.put("%totalplayers%", $Server_uniqueJoins);
            placeholderMapping.put("%uptimemins%", "%start_date_relative_to_now:'m'%");
            placeholderMapping.put("%uptimehours%", "%start_date_relative_to_now:'h'%");
            placeholderMapping.put("%motd%", ""); // TODO
            placeholderMapping.put("%serverversion%", $Server_version);
            if (recommendedUpgrades) {
                placeholderMapping.put("%usedmemorygb%GB used/%freememorygb%GB free/%maxmemorygb%GB max", "%memory_used%/%memory_available%");
            }
            placeholderMapping.put("%freememory%", "%memory_free_megabytes%");
            placeholderMapping.put("%usedmemory%", "%memory_used_megabytes%");
            placeholderMapping.put("%totalmemory%", "%memory_total_megabytes%");
            placeholderMapping.put("%maxmemory%", "%memory_max_megabytes%");
            placeholderMapping.put("%freememorygb%", "%memory_free_gigabytes%");
            placeholderMapping.put("%usedmemorygb%", "%memory_used_gigabytes%");
            placeholderMapping.put("%totalmemorygb%", "%memory_total_gigabytes%");
            placeholderMapping.put("%maxmemorygb%", "%memory_max_gigabytes%");
            placeholderMapping.put("%tps%", $Spark_tps10s);
            placeholderMapping.put(Pattern.compile("%date%|%time%"), $TimeUtil_timeStamp);

            placeholderMapping.put(Pattern.compile("<t:%timestamp%(:[tTdDfFsSR])?>"), "%now_date:'timestamp$1'%");
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
            placeholderMapping.put("%toprolealias%", ""); // TODO: DiscordChatChannelRoleAliases
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
            // Escaping is handled without a need for another placeholder
            placeholderMapping.put(Pattern.compile("%username%|%usernamenoescapes%"), "%player_name%");
            placeholderMapping.put(Pattern.compile("%displayname%|%displaynamenoescapes%"), "%player_display_name%");

            // %message% stays the same
            placeholderMapping.put("%primarygroup%", "%player_primary_group%");
            placeholderMapping.put("%world%", "%player_world_name%");
            placeholderMapping.put("%worldalias%", $MultiverseCore_worldAlias);
            placeholderMapping.put(Pattern.compile("%date%|%time%"), $TimeUtil_timeStamp);
            // Note: first letter is now lower case (but that makes more sense tbh)
            placeholderMapping.put("%channelname%", "%channel_name%");
        }

        if (placeholderField == PlaceholderField.CHAT_CHANNEL_HOOK_MESSAGE_FORMAT) {
            placeholderMapping.put("%channelname%", "%gamechannel_name%");

            boolean chatty = isIntegrationEnabled("Chatty");
            boolean lunaChat = isIntegrationEnabled("LunaChat");
            boolean townyChat = isIntegrationEnabled("TownyChat");
            boolean ventureChat = isIntegrationEnabled("VentureChat");

            String color = "";
            if (lunaChat || ventureChat) {
                color = "%gamechannel_color%";
            } else if (townyChat) {
                color = "%gamechannel_message_color%";
            }
            placeholderMapping.put("%channelcolor%", color);

            String nickname = "";
            if (chatty) {
                nickname = "%gamechannel_name%";
            } else if (lunaChat) {
                nickname = "%gamechannel_alias|gamechannel_name%";
            } else if (townyChat) {
                nickname = "%gamechannel_tag%";
            } else if (ventureChat) {
                nickname = "%gamechannel_alias%";
            }
            placeholderMapping.put("%channelnickname%", nickname);

            // %message% is left alone, replaced later
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
            placeholderMapping.put("%primarygroup%", "%player_primary_group%");
            placeholderMapping.put("%world%", "%player_world_name%");
            placeholderMapping.put("%worldalias%", $MultiverseCore_worldAlias);
        }

        if (placeholderField.isEmbedMessageFormat()) {
            // Escaping is handled without a need for another placeholder
            placeholderMapping.put(Pattern.compile("%username%|%usernamenoescapes%"), "%player_name%");
            placeholderMapping.put(Pattern.compile("%displayname%|%displaynamenoescapes%"), "%player_display_name%");

            placeholderMapping.put(Pattern.compile("%date%|%time%"), $TimeUtil_timeStamp);
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

        if (placeholderField == PlaceholderField.TOPIC_SHUTDOWN_FORMAT || placeholderField == PlaceholderField.CHANNEL_UPDATER_SHUTDOWN_FORMAT) {
            placeholderMapping.put("%totalplayers%", $Server_uniqueJoins);
            placeholderMapping.put("%serverversion%", $Server_version);
            placeholderMapping.put(Pattern.compile("%date%|%time%"), $TimeUtil_timeStamp);
        }
        if (placeholderField == PlaceholderField.CHANNEL_UPDATER_SHUTDOWN_FORMAT) {
            placeholderMapping.put("%timestamp%", "%now_date_to_epoch_seconds%");
        }

        if (placeholderField == PlaceholderField.DISCORD_COMMAND_FORMAT) {
            placeholderMapping.put("{INVITE}", "%discord_invite%");
        }

        if (placeholderField == PlaceholderField.NICKNAME_FORMAT) {
            placeholderMapping.put("%displayname%", recommendedUpgrades ? "%nickname%" : "%player_display_name%");
            placeholderMapping.put("%username%", "%player_name%");
            placeholderMapping.put("%discord_name%", recommendedUpgrades ? "%user_effective_name%" : "%user_name%");
            placeholderMapping.put("%discord_discriminator%", "%user_discriminator%");
        }

        return placeholderMapping
                .entrySet()
                .stream()
                .map(entry -> {
                    Object key = entry.getKey();
                    if (key instanceof String) {
                        key = Pattern.compile((String) key, Pattern.LITERAL);
                    }
                    return Pair.of((Pattern) key, entry.getValue());
                })
                .collect(Collectors.toList());
    }

    private String convertPlaceholders(PlaceholderField field, String format) {
        if (StringUtils.isEmpty(format)) {
            return format;
        }

        List<Pair<Pattern, Object>> placeholders = makePlaceholderMapping(field);

        PlaceholderReplacer replacer = new PlaceholderReplacer(format);
        for (Pair<Pattern, Object> pair : placeholders) {
            Object replacement = pair.getValue();
            replacer.replaceAll(pair.getKey(), matcher -> {
                if (replacement instanceof PlaceholderAPIReplacement) {
                    if (placeholderAPISuggestionsDone.add("the plugin itself")) {
                        if (!isIntegrationEnabled("PlaceholderAPI")) {
                            logger.info("Some placeholders are being replaced by alternatives that require PlaceholderAPI. If you wish to keep using them, install PlaceholderAPI");
                        }
                    }
                    if (!((PlaceholderAPIReplacement) replacement).isIntegratedExpansion()) {
                        String expansion = ((PlaceholderAPIReplacement) replacement).getExpansion();
                        if (placeholderAPISuggestionsDone.add(expansion)) {
                            logger.info("Some placeholders require the " + expansion + " PlaceholderAPI expansion, make sure it is installed to use the replacement placeholders");
                        }
                    }
                }

                return replacement.toString();
            });
        }
        return replacer.toString();
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

    private String mapDiscordToMinecraftFormat(String channelName) {
        boolean isUsingAnyChatIntegration = Stream.of(
                "Chatty",
                "LunaChat",
                "TownyChat",
                "VentureChat"
        ).anyMatch(this::isIntegrationEnabled);

        String channelSuffix = channelName != null ? "_" + channelName : "";

        String format = messages.node("DiscordToMinecraftChatMessageFormat" + channelSuffix).getString();
        if (format == null) {
            return null;
        }

        String outputFormat = convertPlaceholders(PlaceholderField.DISCORD_TO_MINECRAFT_MESSAGE_FORMAT, format);
        if (isUsingAnyChatIntegration) {
            outputFormat = convertPlaceholders(PlaceholderField.CHAT_CHANNEL_HOOK_MESSAGE_FORMAT, messages.node("ChatChannelHookMessageFormat").getString())
                    .replace("%message%", outputFormat);
        }
        return outputFormat;
    }

    public void migrate(MainConfig mainConfig) throws SerializationException {
        mainConfig.channels.remove(GameChannel.DEFAULT_NAME);

        List<Long> channelIds = new ArrayList<>();
        config.node("Channels").childrenMap().forEach((key, value) -> {
            String channelId = value.getString();
            if (!(key instanceof String) || channelId == null) {
                return;
            }

            String channelName = (String) key;
            long id = MiscUtil.parseLong(channelId);
            channelIds.add(id);

            ChannelConfig channelConfig = new ChannelConfig();
            channelConfig.destination.channelIds = Collections.singletonList(id);
            channelConfig.destination.threads = Collections.emptyList();

            String channelSpecificFormat = mapDiscordToMinecraftFormat(channelName);
            if (channelSpecificFormat != null) {
                channelConfig.discordToMinecraft = new DiscordToMinecraftChatConfig();
                channelConfig.discordToMinecraft.format = channelSpecificFormat;
            }

            mainConfig.channels.put(channelName, channelConfig);
        });

        BaseChannelConfig defaultChannel = mainConfig.channels.get(ChannelConfig.DEFAULT_KEY);
        if (defaultChannel != null) {
            //
            // Minecraft => Discord
            //
            defaultChannel.minecraftToDiscord.enabled = config.node("DiscordChatChannelMinecraftToDiscord").getBoolean(true);

            if (!recommendedUpgrades) {
                boolean useWebhookDelivery = config.node("Experiment_WebhookChatMessageDelivery").getBoolean();
                if (useWebhookDelivery) {
                    String username = config.node("Experiment_WebhookChatMessageUsernameFromDiscord").getBoolean()
                                      ? "%player_linked_user_effective_name|user_effective_name%"
                                      : convertPlaceholders(PlaceholderField.WEBHOOK_USERNAME_FORMAT, config.node("Experiment_WebhookChatMessageUsernameFormat").getString());

                    String avatarUrl = config.node("Experiment_WebhookChatMessageAvatarFromDiscord").getBoolean()
                                       ? "%player_linked_user_effective_avatar_url|player_avatar_url%"
                                       : "%player_avatar_url%";

                    defaultChannel.minecraftToDiscord.format = new SendableDiscordMessageTemplate(
                            SendableDiscordMessage.builder()
                                    .setWebhookUsername(username)
                                    .setWebhookAvatarUrl(avatarUrl)
                                    .setContent(convertPlaceholders(PlaceholderField.WEBHOOK_MESSAGE_FORMAT, config.node("Experiment_WebhookChatMessageFormat").getString()))
                    );
                } else {
                    String format = messages.node("MinecraftChatToDiscordMessageFormat").getString();
                    String noPrimaryGroupFormat = messages.node("MinecraftChatToDiscordMessageFormatNoPrimaryGroup").getString();

                    if ("**%primarygroup%** %displayname% » %message%".equals(format)
                            && "%displayname% » %message%".equals(noPrimaryGroupFormat)) {
                        // If both the formats are at their default values, we can do a compromise
                        defaultChannel.minecraftToDiscord.format = new SendableDiscordMessageTemplate(
                                SendableDiscordMessage.builder().setContent("%player_primary_group:'**%s** '%%player_display_name% » %message%")
                        );
                    } else {
                        // but otherwise we'll just have to use the no primary group format
                        defaultChannel.minecraftToDiscord.format = new SendableDiscordMessageTemplate(
                                SendableDiscordMessage.builder().setContent(convertPlaceholders(PlaceholderField.MINECRAFT_TO_DISCORD_MESSAGE_FORMAT, noPrimaryGroupFormat))
                        );
                    }
                }
            }

            Map<Pattern, String> oldMinecraftToDiscordFilters = convertToRegexReplacements(config.node("DiscordChatChannelGameFilters"));
            if (recommendedUpgrades) {
                defaultChannel.minecraftToDiscord.contentRegexFilters.putAll(oldMinecraftToDiscordFilters);
            } else {
                defaultChannel.minecraftToDiscord.contentRegexFilters = oldMinecraftToDiscordFilters;
            }

            boolean mentionsEnabled = config.node("DiscordChatChannelTranslateMentions").getBoolean(true);
            List<String> allowedMentions = mentionsEnabled
                                           ? config.node("DiscordChatChannelAllowedMentions").getList(String.class, Collections.emptyList())
                                           : Collections.emptyList();
            defaultChannel.minecraftToDiscord.mentions.channels = allowedMentions.contains("channel");
            defaultChannel.minecraftToDiscord.mentions.everyone = allowedMentions.contains("everyone");
            defaultChannel.minecraftToDiscord.mentions.roles = allowedMentions.contains("role");
            defaultChannel.minecraftToDiscord.mentions.users = allowedMentions.contains("user");

            //
            // Discord => Minecraft
            //
            defaultChannel.discordToMinecraft.enabled = config.node("DiscordChatChannelDiscordToMinecraft").getBoolean(true);
            defaultChannel.discordToMinecraft.format = mapDiscordToMinecraftFormat(null);
            if (!recommendedUpgrades) {
                defaultChannel.discordToMinecraft.replyFormat = convertPlaceholders(PlaceholderField.DISCORD_TO_MINECRAFT_REPLY_FORMAT, messages.node("DiscordToMinecraftMessageReplyFormat").getString());
            }

            Map<Pattern, String> oldDiscordToMinecraftFilters = convertToRegexReplacements(config.node("DiscordChatChannelDiscordFilters"));
            if (recommendedUpgrades) {
                defaultChannel.discordToMinecraft.contentRegexFilters.putAll(oldDiscordToMinecraftFilters);
            } else {
                defaultChannel.discordToMinecraft.contentRegexFilters = oldDiscordToMinecraftFilters;
            }

            int truncateLength = config.node("DiscordChatChannelTruncateLength").getInt(-1);
            if (truncateLength > 0 && truncateLength < 4000) {
                defaultChannel.discordToMinecraft.contentRegexFilters.put(Pattern.compile("(.{" + truncateLength + "}).*"), "$1");
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

            String emojiBehaviour = config.node("DiscordChatChannelEmojiBehavior").getString();
            if ("show".equalsIgnoreCase(emojiBehaviour)) {
                defaultChannel.discordToMinecraft.unicodeEmojiBehaviour = DiscordToMinecraftChatConfig.EmojiBehaviour.SHOW;
            } else if ("hide".equalsIgnoreCase(emojiBehaviour)) {
                defaultChannel.discordToMinecraft.unicodeEmojiBehaviour = DiscordToMinecraftChatConfig.EmojiBehaviour.HIDE;
            }

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

            defaultChannel.discordToMinecraft.formattingLimit.filters = convertRoleNameListToIds(config.node("DiscordChatChannelRolesAllowedToUseColorCodesInChat"))
                    .stream()
                    .map(roleId -> new DiscordUserFilterConfig.SingleFilter(roleId, FilterMode.WHITELIST))
                    .collect(Collectors.toList());

            defaultChannel.discordToMinecraft.logToConsole = config.node("DiscordChatChannelBroadcastDiscordMessagesToConsole").getBoolean(true);

            //
            // Misc. messages
            //
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

            //
            // Role selection
            //
            defaultChannel.roleSelection.blacklist = !config.node("DiscordChatChannelRolesSelectionAsWhitelist").getBoolean(false);
            defaultChannel.roleSelection.ids = convertRoleNameListToIds(config.node("DiscordChatChannelRolesSelection"));

            //
            // Mentions
            //
            String emoteBehaviour = config.node("DiscordChatChannelEmoteBehavior").getString();
            if ("name".equals(emoteBehaviour)) {
                defaultChannel.mentions.customEmojiBehaviour = MentionsConfig.EmoteBehaviour.NAME;
            } else if ("hide".equals(emoteBehaviour)) {
                defaultChannel.mentions.customEmojiBehaviour = recommendedUpgrades
                                                               ? MentionsConfig.EmoteBehaviour.BLANK
                                                               : MentionsConfig.EmoteBehaviour.HIDE;
            }
        }

        //
        // Presence Updater
        //
        ConfigurationNode gameStatusNode = config.node("DiscordGameStatus");
        List<String> gameStatuses = gameStatusNode.getList(String.class, () -> Collections.singletonList(gameStatusNode.getString()));
        String onlineStatusString = config.node("DiscordOnlineStatus").getString();
        OnlineStatus onlineStatus = null;
        try {
            onlineStatus = OnlineStatus.valueOf(onlineStatusString);
        } catch (IllegalArgumentException ignored) {}

        mainConfig.presenceUpdater.enabled = !gameStatuses.isEmpty() || !recommendedUpgrades;
        mainConfig.presenceUpdater.updaterRateInSeconds = config.node("StatusUpdateRateInMinutes").getInt(1) * 60;

        mainConfig.presenceUpdater.presences.clear();
        for (String gameStatus : gameStatuses) {
            PresenceUpdaterConfig.Presence presence = new PresenceUpdaterConfig.Presence();
            presence.status = onlineStatus;
            presence.activity = convertPlaceholders(PlaceholderField.DISCORD_GAME_STATUS, gameStatus);

            mainConfig.presenceUpdater.presences.add(presence);
        }

        //
        // Discord Invite
        //
        String inviteLink = config.node("DiscordInviteLink").getString();
        if (!StringUtils.isEmpty(inviteLink) && !inviteLink.contains("/changethisin")) {
            mainConfig.invite.inviteUrl = inviteLink;
        }

        //
        // Linking Rewards
        //
        mainConfig.rewards.linkingRewards.clear();
        List<String> linkingCommands = config.node("MinecraftDiscordAccountLinkedConsoleCommands").getList(String.class, Collections.emptyList());
        if (!linkingCommands.isEmpty()) {
            RewardsConfig.LinkingReward linkingReward = new RewardsConfig.LinkingReward();
            linkingReward.rewardId = "v1migrated-link";
            linkingReward.type = RewardsConfig.LinkingReward.Type.LINKED;
            linkingReward.grantType = recommendedUpgrades ? RewardsConfig.GrantType.ONCE_PER_BOTH : RewardsConfig.GrantType.ALWAYS;
            linkingReward.consoleCommandsToRun = linkingCommands.stream()
                    .map(command -> convertPlaceholders(PlaceholderField.MINECRAFT_DISCORD_ACCOUNT_LINKING, command))
                    .collect(Collectors.toList());
            mainConfig.rewards.linkingRewards.add(linkingReward);
        }

        List<String> unlinkingCommands = config.node("MinecraftDiscordAccountUnlinkedConsoleCommands").getList(String.class, Collections.emptyList());
        if (!unlinkingCommands.isEmpty()) {
            RewardsConfig.LinkingReward linkingReward = new RewardsConfig.LinkingReward();
            linkingReward.rewardId = "v1migrated-unlink";
            linkingReward.type = RewardsConfig.LinkingReward.Type.UNLINKED;
            linkingReward.grantType = recommendedUpgrades ? RewardsConfig.GrantType.ONCE_PER_BOTH : RewardsConfig.GrantType.ALWAYS;
            linkingReward.consoleCommandsToRun = linkingCommands.stream()
                    .map(command -> convertPlaceholders(PlaceholderField.MINECRAFT_DISCORD_ACCOUNT_LINKING, command))
                    .collect(Collectors.toList());
            mainConfig.rewards.linkingRewards.add(linkingReward);
        }

        //
        // Console
        //
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

        //
        // Group Sync
        //
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
        groupSyncSet.tieBreakers.link = synchronization.node("GroupRoleSynchronizationOnLink").getBoolean()
                                        ? (groupSyncMinecraftIsTieBreaker ? SyncSide.MINECRAFT : SyncSide.DISABLED)
                                        : SyncSide.DISABLED;

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

        //
        // Nickname Sync
        //
        // TODO: determine server id
        if (synchronization.node("NicknameSynchronizationEnabled").getBoolean(false) && !recommendedUpgrades) {
            mainConfig.nicknameSync.direction = SyncDirection.MINECRAFT_TO_DISCORD;
        }
        mainConfig.nicknameSync.timer.cycleTime = synchronization.node("NicknameSynchronizationCycleTime").getInt(3);
        mainConfig.nicknameSync.format = convertPlaceholders(PlaceholderField.NICKNAME_FORMAT, synchronization.node("NicknameSynchronizationFormat").getString());

        //
        // Ban Sync
        //
        // TODO: determine server id
        boolean bansToMinecraft = synchronization.node("BanSynchronizationDiscordToMinecraft").getBoolean(true);
        boolean bansToDiscord = synchronization.node("BanSynchronizationMinecraftToDiscord").getBoolean(true);
        SyncDirection banSyncDirection = SyncDirection.BIDIRECTIONAL;
        if (bansToMinecraft && !bansToDiscord) {
            banSyncDirection = SyncDirection.DISCORD_TO_MINECRAFT;
        } else if (!bansToMinecraft && bansToDiscord) {
            banSyncDirection = SyncDirection.MINECRAFT_TO_DISCORD;
        }

        mainConfig.banSync.direction = banSyncDirection;

        //
        // Linked Role
        //
        Long linkedRoleId = convertRoleNameToId(config.node("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"));
        if (linkedRoleId != null) {
            mainConfig.linkedRole.roleIds.clear();
            mainConfig.linkedRole.roleIds.add(linkedRoleId); // TODO: role name => role id
        }

        //
        // Execute Command
        //
        mainConfig.executeCommand.enabled = recommendedUpgrades || config.node("DiscordChatChannelConsoleCommandEnabled").getBoolean();
        mainConfig.executeCommand.ephemeral = recommendedUpgrades
                || (config.node("DiscordChatChannelConsoleCommandExpiration").getInt() > 0
                 && config.node("DiscordChatChannelConsoleCommandExpirationDeleteRequest").getBoolean());

        mainConfig.executeCommand.executionConditions.clear();

        GameCommandExecutionConditionConfig executeCommandLimitedCondition = new GameCommandExecutionConditionConfig();
        executeCommandLimitedCondition.userFilter.filters.clear();
        convertRoleNameListToIds(config.node("DiscordChatChannelConsoleCommandRolesAllowed"))
                .forEach(allowedRoleId -> executeCommandLimitedCondition.userFilter.filters.add(new DiscordUserFilterConfig.SingleFilter(allowedRoleId, FilterMode.WHITELIST)));
        executeCommandLimitedCondition.filterMode = config.node("DiscordChatChannelConsoleCommandWhitelistActsAsBlacklist").getBoolean()
                                                    ? FilterMode.BLACKLIST : FilterMode.WHITELIST;
        executeCommandLimitedCondition.commands = config.node("DiscordChatChannelConsoleCommandWhitelist").getList(String.class, Collections.emptyList());
        mainConfig.executeCommand.executionConditions.add(executeCommandLimitedCondition);

        GameCommandExecutionConditionConfig executeCommandFullCondition = new GameCommandExecutionConditionConfig();
        executeCommandFullCondition.userFilter.filters.clear();
        convertRoleNameListToIds(config.node("DiscordChatChannelConsoleCommandWhitelistBypassRoles"))
                .forEach(allowedRoleId -> executeCommandFullCondition.userFilter.filters.add(new DiscordUserFilterConfig.SingleFilter(allowedRoleId, FilterMode.WHITELIST)));
        executeCommandFullCondition.filterMode = FilterMode.BLACKLIST;
        executeCommandFullCondition.commands.clear();
        mainConfig.executeCommand.executionConditions.add(executeCommandFullCondition);

        //
        // Custom Commands
        //
        mainConfig.customCommands.clear();
        config.node("DiscordCannedResponses").childrenMap()
                .forEach((key, value) -> {
                    String rawTrigger = (String) key;
                    String response = value.getString();
                    if (recommendedUpgrades) {
                        if (rawTrigger.equals("!ip") && "yourserveripchange.me".equals(response)) {
                            return;
                        }
                        //noinspection HttpUrlsUsage
                        if (rawTrigger.equals("!site") && "http://yoursiteurl.net".equals(response)) {
                            return;
                        }
                    }

                    Matcher triggerMatcher = DiscordCommand.CHAT_INPUT_NAME_PATTERN.matcher(rawTrigger);
                    if (!triggerMatcher.find()) {
                        return;
                    }

                    CustomCommandConfig customCommandConfig = new CustomCommandConfig();
                    customCommandConfig.enabled = true;
                    customCommandConfig.command = triggerMatcher.group();
                    customCommandConfig.ephemeral = !recommendedUpgrades;
                    customCommandConfig.response = new SendableDiscordMessageTemplate(
                            SendableDiscordMessage.builder()
                                    .setContent(response)
                    );

                    mainConfig.customCommands.add(customCommandConfig);
                });

        //
        // Channel Updater
        //
        if (!recommendedUpgrades) {
            mainConfig.channelUpdater.textChannels.clear();
            boolean updateAtShutdown = config.node("ChannelTopicUpdaterChannelTopicsAtShutdownEnabled").getBoolean(true);

            String channelTopicFormat = messages.node("ChannelTopicUpdaterChatChannelTopicFormat").getString();
            if (!StringUtils.isEmpty(channelTopicFormat)) {
                ChannelUpdaterConfig.TextChannelConfig updaterConfig = new ChannelUpdaterConfig.TextChannelConfig();
                updaterConfig.channelIds = channelIds;
                updaterConfig.nameFormat = convertPlaceholders(PlaceholderField.TOPIC_FORMAT, channelTopicFormat);
                updaterConfig.shutdownNameFormat = updateAtShutdown
                                                   ? convertPlaceholders(PlaceholderField.TOPIC_SHUTDOWN_FORMAT, messages.node("ChannelTopicUpdaterChatChannelTopicAtServerShutdownFormat").getString())
                                                   : "";
                updaterConfig.timeMinutes = config.node("ChannelTopicUpdaterRateInMinutes").getInt();

                mainConfig.channelUpdater.textChannels.add(updaterConfig);
            }

            String consoleTopicFormat = messages.node("ChannelTopicUpdaterConsoleChannelTopicFormat").getString();
            if (!StringUtils.isEmpty(consoleTopicFormat)) {
                ChannelUpdaterConfig.TextChannelConfig updaterConfig = new ChannelUpdaterConfig.TextChannelConfig();
                updaterConfig.channelIds = consoleChannelIds;
                updaterConfig.nameFormat = convertPlaceholders(PlaceholderField.TOPIC_FORMAT, consoleTopicFormat);
                updaterConfig.shutdownNameFormat = updateAtShutdown
                                                   ? convertPlaceholders(PlaceholderField.TOPIC_SHUTDOWN_FORMAT, messages.node("ChannelTopicUpdaterConsoleChannelTopicAtServerShutdownFormat").getString())
                                                   : "";
            }
        }

        List<ChannelUpdaterConfig.VoiceChannelConfig> channelUpdaters = config.node("ChannelUpdater").childrenList().stream().map(channelUpdater -> {
            String channelIdRaw = channelUpdater.node("ChannelId").getString();
            if (channelIdRaw == null || (recommendedUpgrades && channelIdRaw.replace("0", "").isEmpty())) {
                return null;
            }
            long channelId;
            try {
                channelId = MiscUtil.parseLong(channelIdRaw);
            } catch (NumberFormatException ignored) {
                return null;
            }

            ChannelUpdaterConfig.VoiceChannelConfig updaterConfig = new ChannelUpdaterConfig.VoiceChannelConfig();
            updaterConfig.channelIds = Collections.singletonList(channelId);
            updaterConfig.nameFormat = convertPlaceholders(PlaceholderField.CHANNEL_UPDATER_FORMAT, channelUpdater.node("Format").getString());
            updaterConfig.shutdownNameFormat = convertPlaceholders(PlaceholderField.CHANNEL_UPDATER_SHUTDOWN_FORMAT, channelUpdater.node("ShutdownFormat").getString());
            updaterConfig.timeMinutes = channelUpdater.node("UpdateInterval").getInt();
            return updaterConfig;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (!channelUpdaters.isEmpty() || !recommendedUpgrades) {
            mainConfig.channelUpdater.voiceChannels.clear();
            mainConfig.channelUpdater.voiceChannels.addAll(channelUpdaters);
        }

        //
        // Messages
        //

        mainConfig.messages.defaultLanguage = config.node("ForcedLanguage").getString(); // TODO: save configs in this language

        //
        // Integrations
        //

        mainConfig.integrations.disabledIntegrations = disabledIntegrations;
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
