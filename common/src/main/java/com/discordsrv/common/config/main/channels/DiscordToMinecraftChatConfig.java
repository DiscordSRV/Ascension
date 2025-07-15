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

package com.discordsrv.common.config.main.channels;

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.DefaultOnly;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import com.discordsrv.common.config.main.generic.DiscordIgnoresConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.*;
import java.util.regex.Pattern;

@ConfigSerializable
public class DiscordToMinecraftChatConfig {

    public DiscordToMinecraftChatConfig() {
        ConfigurateConfigManager.nullAllFields(this);
    }

    @Comment("Requires the \"Message Content Intent\"")
    public Boolean enabled = true;

    @Comment("The Discord to Minecraft message format for regular users and bots\n"
            + "\n"
            + "Suggested placeholders:\n"
            + "%message% - The content of the Discord message after %4 are applied\n"
            + "%user_effective_name% - The Discord user's display name\n"
            + "%user_name% - The Discord user's username (the one below their display name when you click in their profile)\n"
            + "%user_tag% - The Discord user's username and possible discriminator (mostly useful with bots)\n"
            + "%user_selected_roles% - The Discord user's roles\n"
            + "%message_reply% - The value from %2 if the Discord message is in reply to another message\n"
            + "%message_attachments% - The value from %3 for each attachment the Discord message\n"
            + "%gamechannel_name% - The name of the in-game chat channel the message was sent to\n"
            + "\n"
            + "More placeholders at %1 (User, User (Server Member), Server, Channel, GameChannel)")
    @Constants.Comment({
            DocumentationURLs.PLACEHOLDERS,
            "reply-format",
            "attachment-format",
            "content-regex-filters"
    })
    public String format = "[[color:#5865F2]Discord[color]] "
            + "[hover:show_text:Username: @%user_tag% [italics:on][color:gray](Shift+Click to mention)[color][italics:off]\nRoles: %user_selected_roles:', '|text:'[color:gray][italics:on]None[color][italics]'%]"
            + "[insert:@%user_tag%]%user_color%%user_effective_name%[color][insert]"
            + "[hover]%message_reply% » %message%%message_attachments%";

    @Comment("The Discord to Minecraft message format for webhook messages (if enabled)\n"
            + "The same placeholders as the regular \"%1\" are available, apart from User (Server Member) placeholders")
    @Constants.Comment("format")
    public String webhookFormat = "[[color:#5865F2]Discord[color]] [hover:show_text:Bot message]%user_effective_name%[hover] » %message%%message_attachments%";

    @Comment("Format for a single attachment in the %message_attachments% placeholder\n"
            + "Placeholders:\n"
            + "%file_name% - The name of the attachment file\n"
            + "%file_url% - The link to the attachment file\n"
            + "\n"
            + "More placeholders at %1 (User, User (Server Member), Server, Channel, GameChannel)")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    public String attachmentFormat = " [hover:show_text:Open %file_name% in browser][click:open_url:%file_url%][color:green][[color:white]%file_name%[color:green]][color][click][hover]";

    @Comment("Format for the %message_reply% placeholder, when the message is a reply to another message")
    public String replyFormat = " [hover:show_text:%message%][click:open_url:%message_jump_url%]replying to %user_color|text:''%%user_effective_name%[color][click][hover]";

    // TODO: more info on regex pairs (String#replaceAll)
    @Comment("Regex filters for Discord message contents (this is the %message% part of the \"format\" option)")
    @Untranslated(Untranslated.Type.VALUE)
    @DefaultOnly
    public Map<Pattern, String> contentRegexFilters = new LinkedHashMap<Pattern, String>() {{
        put(Pattern.compile("\\n{2,}"), "\n");
    }};

    @Comment("Users, bots, roles and webhooks to ignore")
    public DiscordIgnoresConfig ignores = new DiscordIgnoresConfig();

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

    @Comment("The number of milliseconds by which to delay processing Discord messages, if the message is deleted in that time it will not be processed.\n"
            + "This can be used together with Discord moderation bots, to filter forwarded messages")
    public Long delayMillis = 0L;

    @Comment("If messages from Discord should be logged to the console\n"
            + "\n"
            + "Suggested placeholders:\n"
            + "%formatted_message% - The entire message sent in-game as configured above\n"
            + "%message% - The content of the Discord message\n"
            + "%user_name% - The username of the Discord user\n"
            + "%gamechannel_name% - The name of the in-game chat channel the message was sent to\n"
            + "More placeholders at %1 (User, User (Server Member), Server, Channel, GameChannel)")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    public boolean logToConsole = true;
    public String consoleFormat = "[%gamechannel_name%] %formatted_message%";

}
