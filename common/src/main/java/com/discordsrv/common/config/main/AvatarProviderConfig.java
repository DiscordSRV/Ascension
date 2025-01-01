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

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class AvatarProviderConfig {

    @Comment("Whether to let DiscordSRV decide an appropriate avatar URL automatically\n" +
            "This will result in appropriate head renders being provided for Bedrock players (when using Floodgate) and Offline Mode players (via username).")
    public boolean autoDecideAvatarUrl = true;

    @Untranslated(Untranslated.Type.VALUE)
    @Comment("The template for URLs of player avatars\n" +
            "This will be used for official Java players only if %1 is set to true\n" +
            "This will be used ALWAYS if %1 is set to false")
    @Constants.Comment("auto-decide-avatar-url")
    public String avatarUrlTemplate = "https://crafatar.com/avatars/%player_uuid_short%.png?size=128&overlay#%player_skin_texture_id%";
}
