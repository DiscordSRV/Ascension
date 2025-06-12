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

import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.feature.bansync.BanSyncModule;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class BanSyncConfig extends AbstractSyncConfig<BanSyncConfig, BanSyncModule.Game, Long> {

    @Comment("The id for the Discord server where the bans should be synced from/to")
    public long serverId = 0L;

    @Comment("The reason applied when creating new bans in Minecraft")
    public String gameBanReasonFormat = "%punishment_reason%";

    @Comment("The punisher applied when creating new bans in Minecraft")
    public String gamePunisherFormat = "%user_color%@%user_name%";

    @Comment("The kick reason when a ban is applied to an online player")
    public String gameKickReason = "&cYou have been banned for &f%punishment_reason% &cby &f%punishment_punisher%";

    @Comment("The reason applied when creating new bans in Discord")
    public String discordBanReasonFormat = "Banned by %punishment_punisher% in Minecraft for %punishment_reason%, ends: %punishment_until:'YYYY-MM-dd HH:mm:ss zzz'|text:'Never'%";

    @Comment("The reason applied when removing bans in Discord")
    public String discordUnbanReasonFormat = "Unbanned in Minecraft";

    @Comment("The amount of hours to delete Discord messages, when syncing bans from Minecraft to Discord")
    public int discordMessageHoursToDelete = 0;

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
}
