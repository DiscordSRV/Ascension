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

import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.DefaultOnly;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@ConfigSerializable
public class MinecraftToDiscordChatConfig implements IMessageConfig {

    public MinecraftToDiscordChatConfig() {
        ConfigurateConfigManager.nullAllFields(this);
    }

    public Boolean enabled = true;

    @Comment("Suggested placeholders:\n"
            + "%message% - The formatted message content\n"
            + "%player_prefix% - The player's prefix (LuckPerms meta \"discordsrv_prefix\", otherwise their in-game prefix)\n"
            + "%player_meta_prefix% - The player's prefix from the LuckPerms meta \"discordsrv_prefix\" only\n"
            + "%player_suffix% - The player's suffix (LuckPerms meta \"discordsrv_suffix\", otherwise their in-game suffix)\n"
            + "%player_meta_suffix% - The player's suffix from the LuckPerms meta \"discordsrv_suffix\" only\n"
            + "%player_display_name% - The player's display name\n"
            + "%player_name% - The player's username\n"
            + "%player_avatar_url% - The player's avatar url based on the \"avatar-provider\" configuration\n"
            + "More placeholders at %1 (Server, Player, GameChannel)")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    @Untranslated(Untranslated.Type.VALUE)
    public SendableDiscordMessage.Builder format = SendableDiscordMessage.builder()
            .setWebhookUsername("%player_prefix%%player_display_name%%player_suffix%")
            .setWebhookAvatarUrl("%player_avatar_url%")
            .setContent("%message%");

    // TODO: more info on regex pairs (String#replaceAll)
    @Comment("Regex filters for Minecraft message contents (this is the %message% part of the \"format\" option)")
    @Untranslated(Untranslated.Type.VALUE)
    @DefaultOnly
    public Map<Pattern, String> contentRegexFilters = new LinkedHashMap<>();

    public Mentions mentions = new Mentions();

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public SendableDiscordMessage.Builder format() {
        return format;
    }

    @ConfigSerializable
    public static class Mentions {

        @Comment("Should mentions be rendered in Minecraft when sent in Minecraft")
        public boolean renderMentionsInGame = true;

        @Comment("If role mentions should be rendered on Discord\n\n"
                + "The player needs one of the below permission to trigger notifications:\n"
                + "- discordsrv.mention.role.<role id> (to mention a specific role)\n"
                + "- discordsrv.mention.role.mentionable (for roles which have \"Allow anyone to @mention this role\" enabled)\n"
                + "- discordsrv.mention.role.all (to mention ALL roles except @everyone)\n"
                + "The roles need to have \"Allow anyone to @mention this role\" enabled or the bot needs to have the \"Mention @everyone, @here and All Roles\" permission for notifications to be triggered")
        public boolean roles = true;

        @Comment("If channel mentions should be rendered on Discord")
        public boolean channels = true;

        @Comment("If user mentions should be rendered on Discord\n"
                + "The player needs one of the following permissions to trigger notifications:\n"
                + "- discordsrv.mention.user.<user id> (to mention a specific user)\n"
                + "- discordsrv.mention.user.all (to mention ALL user)\n"
                + "Requires the \"Server Members Intent\"")
        public boolean users = true;

        @Comment("If uncached users should be looked up from the Discord API when a mention (\"@something\") occurs in chat.\n"
                + "The player needs the discordsrv.mention.lookup.user permission for uncached members to be looked up\n"
                + "This WILL cause sending messages to be delayed")
        public boolean uncachedUsers = false;

        @Comment("If @everyone and @here mentions should be enabled\n"
                + "The player needs the discordsrv.mention.everyone permission to render the mention and trigger a notification\n"
                + "The bot needs to have the \"Mention @everyone, @here and All Roles\" permission to trigger a notification")
        public boolean everyone = false;

        public boolean any() {
            return roles || channels || users;
        }
    }
    
}
