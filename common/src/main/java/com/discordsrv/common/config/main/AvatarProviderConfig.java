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
import com.discordsrv.common.config.documentation.DocumentationURLs;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class AvatarProviderConfig {

    @Comment("How the %1 placeholder provider\n"
            + "Available values:\n"
            + "off - Always use the default url\n"
            + "url - Use the url from the template in Discord directly")
    @Constants.Comment("%player_avatar_url%")
    public Provider provider = Provider.OFF;

    public enum Provider {
        OFF,
        URL
    }

    @Comment("Bring your own avatar url templates, empty templates will be skipped\n"
            + "Suggested Placeholders:\n"
            + "%player_skin_texture_id% - The texture ID for the player\n"
            + "%player_skin_model% - The skin model (classic, slim) for the player\n"
            + "%player_uuid% - Full UUID for the player\n"
            + "%player_uuid_short% - The UUID for the player without dashes\n"
            + "%player_name% - The player's username\n"
            + "More placeholders at %1")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    public Services services = new Services();

    public static class Services {

        @Comment("The url template, when the player's texture id and model is available.")
        public String textureTemplate = "";

        @Comment("The url template, when the player has a online mode UUID")
        public String onlineUuidTemplate = "";

        @Comment("The url template, for Bedrock players when the player has a Floodgate (Geyser) UUID")
        public String floodgateTemplate = "";

        @Comment("The url template, when the player has a offline mode UUID")
        public String offlineTemplate = "";

    }

    @Comment("Default avatar URL")
    public String defaultUrl = "%bot_user_avatar_url%";
}
