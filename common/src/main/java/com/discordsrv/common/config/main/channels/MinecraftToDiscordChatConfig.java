/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.common.config.annotation.Untranslated;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@ConfigSerializable
public class MinecraftToDiscordChatConfig implements IMessageConfig {

    @Comment("Is Minecraft to Discord chat forwarding enabled")
    public boolean enabled = true;

    @Untranslated(Untranslated.Type.VALUE)
    public SendableDiscordMessage.Builder format = SendableDiscordMessage.builder()
            .setWebhookUsername("%player_meta_prefix|player_prefix%%player_display_name|player_name%%player_meta_suffix|player_suffix%")
            .setWebhookAvatarUrl("%player_avatar_url%")
            .setContent("%message%");

    // TODO: more info on regex pairs (String#replaceAll)
    @Comment("Regex filters for Minecraft message contents (this is the %message% part of the \"format\" option)")
    public Map<Pattern, String> contentRegexFilters = new LinkedHashMap<>();

    @Comment("What mentions should be translated from chat messages to mentions (this does not effect if they will cause a notification or not)")
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

        public boolean roles = true;
        public boolean users = true;
        public boolean channels = true;

    }
    
}
