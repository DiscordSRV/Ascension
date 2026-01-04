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

package com.discordsrv.common.config.main.sync;


import com.discordsrv.common.abstraction.sync.enums.mutes.MuteSyncDiscordAction;
import com.discordsrv.common.abstraction.sync.enums.mutes.MuteSyncDiscordTrigger;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.util.Game;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MuteSyncConfig extends AbstractSyncConfig<MuteSyncConfig, Game, Long> {

    public MuteSyncConfig() {
        // Changed default
        unlinkBehaviour = UnlinkBehaviour.DO_NOTHING;
    }

    @Comment("The id for the Discord server where the mutes should be synced from/to")
    @Order(-10)
    public long serverId = 0L;

    @Comment("Role id that will be used for role related actions, if they are configured below")
    public Long mutedRoleId = 0L;

    @Comment("Options for syncing mutes from Minecraft to Discord")
    public MinecraftToDiscordConfig minecraftToDiscord = new MinecraftToDiscordConfig();

    @Comment("Options for syncing mutes from Discord to Minecraft")
    public DiscordToMinecraftConfig discordToMinecraft = new DiscordToMinecraftConfig();

    @Override
    public boolean isSet() {
        return serverId != 0;
    }

    @Override
    public Game gameId() {
        return Game.INSTANCE;
    }

    @Override
    public Long discordId() {
        return serverId;
    }

    @Override
    public boolean isSameAs(MuteSyncConfig config) {
        return false;
    }

    @Override
    public String describe() {
        return Long.toUnsignedString(serverId);
    }

    @ConfigSerializable
    public static class DiscordToMinecraftConfig {

        @Comment("What action(s) on Discord should trigger a mute/unmute in Minecraft. Valid options:\n"
                + "timeout:  A timeout/untimeout on the Discord Server\n"
                + "role:     Addition/removal of the muted role (specified above) to the user on Discord\n"
                + "either:   Either of the above\n"
                + "BEWARE: Settings of 'role' or 'either' can be exploited to remove mutes from players if %1 is set to 'discord'")
        @Constants.Comment("tie-breaker")
        public MuteSyncDiscordTrigger trigger = MuteSyncDiscordTrigger.TIMEOUT;

        @Comment("The reason used when creating new mutes in Minecraft")
        public String muteReasonFormat = "Muted on Discord: %punishment_reason%";

        @Comment("The punisher shown when creating new mutes in Minecraft")
        public String punisherFormat = "%user_color%@%user_name%";

        @Comment("The message sent to the online player when they are muted in Minecraft due to a Discord mute")
        public String muteNotificationMessage = "&cYou have been muted on Discord for &f%punishment_reason|text:'Unknown'% &cby &f%punishment_punisher%";

        @Comment("The message sent to the online player when they are unmuted in Minecraft due to a Discord unmute")
        public String unmuteNotificationMessage = "&aYou have been unmuted on Discord.";
    }

    @ConfigSerializable
    public static class MinecraftToDiscordConfig {

        @Comment("What action(s) to perform on the linked Discord account when a player is muted in Minecraft. Can be configured in more detail below\n"
                + "Valid options:\n"
                + "timeout:  Apply a Discord timeout on the user. (Must be 28 days or less)\n"
                + "role:     Add the muted role (specified above) to the user on Discord"
        )
        public MuteSyncDiscordAction action = MuteSyncDiscordAction.TIMEOUT;

        @Comment("When a mute duration is longer than 28 days (the maximum allowed in Discord), the muted role will be assigned instead of a timeout in such cases.\n"
                + "Otherwise, no action will be taken on Discord for mutes longer than 28 days."
        )
        public boolean fallbackToRoleIfTimeoutTooLong = true;

        @Comment("The reason used when creating new mutes in Discord or adding the muted role to users")
        public String muteReasonFormat = "Muted by %punishment_punisher% in Minecraft for %punishment_reason|text:'Unknown'%, ends: %punishment_until:'YYYY-MM-dd HH:mm:ss zzz'|text:'Never'%";

        @Comment("The reason used when removing mutes in Discord or removing the muted role from users")
        public String unmuteReasonFormat = "Unmuted in Minecraft";
    }
}
