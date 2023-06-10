/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.main.channels;

import com.discordsrv.common.config.annotation.Untranslated;
import com.discordsrv.common.config.main.DiscordIgnoresConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@ConfigSerializable
public class DiscordToMinecraftChatConfig {

    public boolean enabled = true;

    @Comment("The Discord to Minecraft message format for regular users and bots")
    @Untranslated(Untranslated.Type.VALUE)
    public String format = "[&#5865F2Discord&r] [hover:show_text:Tag: %user_tag%&r\nRoles: %user_roles:', '|text:'&7&oNone'%]%user_color%%user_effective_name%&r » %message%%message_attachments%";

    @Comment("The Discord to Minecraft message format for webhook messages (if enabled)")
    @Untranslated(Untranslated.Type.VALUE)
    public String webhookFormat = "[&#5865F2Discord&r] [hover:show_text:Webhook message]%user_name%&r » %message%%message_attachments%";

    @Comment("Attachment format")
    @Untranslated(Untranslated.Type.VALUE)
    public String attachmentFormat = " [hover:show_text:Open %file_name% in browser][click:open_url:%file_url%]&a[&f%file_name%&a]&r";

    // TODO: more info on regex pairs (String#replaceAll)
    @Comment("Regex filters for Discord message contents (this is the %message% part of the \"format\" option)")
    @Untranslated(Untranslated.Type.VALUE)
    public Map<Pattern, String> contentRegexFilters = new LinkedHashMap<>();

    @Comment("Users, bots and webhooks to ignore")
    public DiscordIgnoresConfig ignores = new DiscordIgnoresConfig();

    @Comment("The representations of Discord mentions in-game")
    public Mentions mentions = new Mentions();

    @ConfigSerializable
    public static class Mentions {

        public Format role = new Format("&#5865f2@%role_name%", "&#5865f2@deleted-role");
        public Format channel = new Format("[hover:show_text:Click to go to channel][click:open_url:%channel_jump_url%]&#5865f2#%channel_name%", "&#5865f2#deleted-channel");
        public Format user = new Format("[hover:show_text:Tag: %user_tag%&r\nRoles: %user_roles:', '|text:'&7&oNone'%]&#5865f2@%user_effective_name|user_name%", "&#5865f2@Unknown user");

        public String messageUrl = "[hover:show_text:Click to go to message][click:open_url:%jump_url%]&#5865f2#%channel_name% > ...";

        @ConfigSerializable
        public static class Format {

            @Comment("The format shown in-game")
            @Untranslated(Untranslated.Type.VALUE)
            public String format = "";

            @Comment("The format when the entity is deleted or can't be looked up")
            @Untranslated(Untranslated.Type.VALUE)
            public String unknownFormat = "";

            @SuppressWarnings("unused") // Configurate
            public Format() {}

            public Format(String format, String unknownFormat) {
                this.format = format;
                this.unknownFormat = unknownFormat;
            }
        }

    }

}
