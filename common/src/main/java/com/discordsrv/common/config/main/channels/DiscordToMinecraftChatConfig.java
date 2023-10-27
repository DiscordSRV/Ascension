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

import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.main.generic.DiscordIgnoresConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@ConfigSerializable
public class DiscordToMinecraftChatConfig {

    public boolean enabled = true;

    @Comment("The Discord to Minecraft message format for regular users and bots")
    public String format = "[[color:#5865F2]Discord[color]] [hover:show_text:Username: @%user_tag%\nRoles: %user_roles:', '|text:'[color:gray][italics:on]None[color][italics]'%]%user_color%%user_effective_server_name%[color][hover]%message_reply% » %message%%message_attachments%";

    @Comment("The Discord to Minecraft message format for webhook messages (if enabled)")
    public String webhookFormat = "[[color:#5865F2]Discord[color]] [hover:show_text:Bot message]%user_effective_name%[hover] » %message%%message_attachments%";

    @Comment("Format for a single attachment in the %message_attachments% placeholder")
    public String attachmentFormat = " [hover:show_text:Open %file_name% in browser][click:open_url:%file_url%][color:green][[color:white]%file_name%[color:green]][color][click][hover]";

    @Comment("Format for the %message_reply% placeholder, when the message is a reply to another message")
    public String replyFormat = " [hover:show_text:%message%][click:open_url:%message_jump_url%]replying to %user_color|text:''%%user_effective_server_name|user_effective_name%[color][click][hover]";

    // TODO: more info on regex pairs (String#replaceAll)
    @Comment("Regex filters for Discord message contents (this is the %message% part of the \"format\" option)")
    @Untranslated(Untranslated.Type.VALUE)
    public Map<Pattern, String> contentRegexFilters = new LinkedHashMap<Pattern, String>() {{
        put(Pattern.compile("\\n{2,}"), "\n");
    }};

    @Comment("Users, bots, roles and webhooks to ignore")
    public DiscordIgnoresConfig ignores = new DiscordIgnoresConfig();

    @Comment("The representations of Discord mentions in-game")
    public Mentions mentions = new Mentions();

    @ConfigSerializable
    public static class Mentions {

        public Format role = new Format("%role_color%@%role_name%", "[color:#5865F2]@deleted-role");
        public Format channel = new Format("[hover:show_text:Click to go to channel][click:open_url:%channel_jump_url%][color:#5865F2]#%channel_name%", "[color:#5865F2]#Unknown");
        public Format user = new Format("[hover:show_text:Username: @%user_tag%\nRoles: %user_roles:', '|text:'[color:gray][italics:on]None[color][italics]'%][color:#5865F2]@%user_effective_server_name|user_effective_name%", "[color:#5865F2]@Unknown user");

        public String messageUrl = "[hover:show_text:Click to go to message][click:open_url:%jump_url%][color:#5865F2]#%channel_name% > ...";

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

    @Comment("How should unicode emoji be shown in-game:\n"
            + "- hide: hides emojis in-game\n"
            + "- show: shows emojis in-game as is (emojis may not be visible without resource packs)\n"
            //+ "- name: shows the name of the emoji in-game (for example :smiley:)"
            )
    public EmojiBehaviour unicodeEmojiBehaviour = EmojiBehaviour.HIDE;

    public enum EmojiBehaviour {
        HIDE,
        SHOW
        // TODO: add and implement name
    }

    @Comment("How should custom emoji be shown in-game:\n"
            + "- hide: custom emoji will not be shown in-game\n"
            + "- blank: custom emoji will only be shown in-game if it is rendered by a 3rd party plugin\n"
            + "- name: shows the name of the custom emoji in-game (for example :discordsrv:), unless rendered by a 3rd party plugin")
    public EmoteBehaviour customEmojiBehaviour = EmoteBehaviour.BLANK;

    public enum EmoteBehaviour {
        HIDE,
        BLANK,
        NAME
    }

    @Comment("The amount of milliseconds to delay processing Discord messages, if the message is deleted in that time it will not be processed.\n"
            + "This can be used together with Discord moderation bots, to filter forwarded messages")
    public long delayMillis = 0L;

}
