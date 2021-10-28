/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@ConfigSerializable
public class DiscordToMinecraftChatConfig {

    @Comment("The Discord to Minecraft message format for regular users")
    public String format = "[&#5865F2Discord&r] [hover:show_text:Tag: %user_tag%&r\\nRoles: %user_roles_, |text_&7&oNone%%]%user_color%%user_effective_name%&r » %message%";

    @Comment("The Discord to Minecraft message format for webhook messages (if enabled)")
    public String webhookFormat = "[&#5865F2Discord&r] [hover:show_text:Webhook message]%user_name%&r » %message%";

    @Comment("Users, bots and webhooks to ignore")
    public Ignores ignores = new Ignores();

    // TODO: more info on regex pairs (String#replaceAll)
    @Comment("Regex filters for Discord message contents (this is the %message% part of the \"format\" option)")
    public Map<Pattern, String> contentRegexFilters = new LinkedHashMap<>();

    @ConfigSerializable
    public static class Ignores {
        @Comment("User, bot and webhook ids to ignore")
        public IDs usersAndWebhookIds = new IDs();

        @Comment("Role ids for users/bots to ignore")
        public IDs roleIds = new IDs();

        @Comment("If bots (webhooks not included) should be ignored")
        public boolean bots = false;

        @Comment("If webhooks should be ignored")
        public boolean webhooks = false;

        @ConfigSerializable
        public static class IDs {

            public List<Long> ids = new ArrayList<>();

            @Comment("true for whitelisting the provided ids, false for blacklisting them")
            public boolean whitelist = false;
        }
    }

}
