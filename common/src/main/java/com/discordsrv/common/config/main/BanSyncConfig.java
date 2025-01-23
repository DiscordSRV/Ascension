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

import com.discordsrv.common.abstraction.sync.enums.BanSyncAction;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.feature.bansync.BanSyncModule;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class BanSyncConfig extends AbstractSyncConfig<BanSyncConfig, BanSyncModule.Game, Long> {

    @Comment("The id for the Discord server where the bans should be synced from/to")
    @Order(-10)
    public long serverId = 0L;

    @Comment("Options for syncing bans from Minecraft to Discord")
    public MinecraftToDiscordConfig minecraftToDiscord = new MinecraftToDiscordConfig();

    @Comment("Options for syncing bans from Discord to Minecraft")
    public DiscordToMinecraftConfig discordToMinecraft = new DiscordToMinecraftConfig();

    @Comment("Resync upon linking")
    public boolean resyncUponLinking = true;

    @Override
    public boolean isSet() {
        return serverId != 0;
    }

    @Override
    public BanSyncModule.Game gameId() {
        return BanSyncModule.Game.INSTANCE;
    }

    @Override
    public Long discordId() {
        return serverId;
    }

    @Override
    public boolean isSameAs(BanSyncConfig config) {
        return false;
    }

    @Override
    public String describe() {
        return Long.toUnsignedString(serverId);
    }

    @ConfigSerializable
    public static class DiscordToMinecraftConfig {

        @Comment("The reason applied when creating new bans in Minecraft")
        public String banReasonFormat = "%punishment_reason%";

        @Comment("The punisher applied when creating new bans in Minecraft")
        public String punisherFormat = "%user_color%@%user_name%";

        @Comment("The kick reason when a ban is applied to a online player")
        public String kickReason = "&cYou have been banned for &f%punishment_reason% &cby &f%punishment_punisher%";
    }

    @ConfigSerializable
    public static class MinecraftToDiscordConfig {

        @Comment("What action(s) to perform on the linked Discord account when a player is banned in Minecraft. Can be configured in more detail below\n"
                + "Valid options: %1, %2")
        @Constants.Comment({"ban", "role"})
        public BanSyncAction action = BanSyncAction.BAN;

        @Comment("Config for banning users on Discord when they are banned in Minecraft")
        public BanConfig ban = new BanConfig();

        @Comment("Config for adding a Discord role to users when they are banned in Minecraft")
        public RoleConfig role = new RoleConfig();

        @ConfigSerializable
        public static class BanConfig {

            @Comment("The reason applied when creating new bans in Discord")
            public String banReasonFormat = "Banned by %punishment_punisher% in Minecraft for %punishment_reason%, ends: %punishment_until:'YYYY-MM-dd HH:mm:ss zzz'|text:'Never'%";

            @Comment("The reason applied when removing bans in Discord")
            public String unbanReasonFormat = "Unbanned in Minecraft";

            @Comment("The number of hours of Discord messages to delete, when syncing bans from Minecraft to Discord")
            public int messageHoursToDelete = 0;
        }

        @ConfigSerializable
        public static class RoleConfig {

            @Comment("Discord role id to add to the user when they are banned in Minecraft.")
            public Long roleId = 0L;
        }
    }
}
