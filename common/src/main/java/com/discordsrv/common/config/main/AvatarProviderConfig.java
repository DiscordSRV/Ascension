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

package com.discordsrv.common.config.main;

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import com.discordsrv.common.feature.AvatarProviderModule;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class AvatarProviderConfig {

    @Comment("""
            How DiscordSRV's recommended avatar service(s) should be used.
            You may review and disable the service(s) being used in the %1
            
            - auto: Uses recommended services, with scaled avatars, for URLs that aren't provided below
            - auto_unscaled: Uses recommended services, with unscaled avatars, for URLs that aren't provided below
            - config_only: Only uses the services provided below""")
    @Constants.Comment({AvatarProviderModule.HEADS_DOMAIN})
    public AvatarServiceMode avatarServiceMode = AvatarServiceMode.AUTO;

    public enum AvatarServiceMode {
        AUTO,
        AUTO_UNSCALED,
        CONFIG_ONLY
    }

    @Comment("""
            Bring your own avatar url templates, empty templates will be skipped
            Each of the below options will be tried in order, allowing you to specify different URLs for different scenarios (using the same URL in each option makes no sense)
            
            Suggested Placeholders:
            %player_skin_texture_id% - The texture ID for the player
            %player_skin_model% - The skin model (classic, slim) for the player
            %player_uuid% - Full UUID for the player
            %player_uuid_short% - The UUID for the player without dashes
            %player_name% - The player's username
            %player_skin_parts_hat:'helm;head''% - Use to change the url when the player has their hat turned off in their settings
            
            More placeholders at %1""")
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

        @Comment("Default avatar URL if none of the other options apply")
        public String defaultUrl = "%bot_user_avatar_url%";

        public Services() {}

        private Services(Services other) {
            this.textureTemplate = other.textureTemplate;
            this.onlineUuidTemplate = other.onlineUuidTemplate;
            this.floodgateTemplate = other.floodgateTemplate;
            this.offlineTemplate = other.offlineTemplate;
            this.defaultUrl = other.defaultUrl;
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod") // Don't care
        @Override
        public Services clone() {
            return new Services(this);
        }
    }

}
