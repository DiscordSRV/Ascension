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

package com.discordsrv.common.config.main.linking;

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.DefaultOnly;
import com.discordsrv.common.config.connection.ConnectionConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class RequirementsConfig {

    @Setting("bypass-uuids")
    @Comment("A list of uuids that are allowed to bypass these requirements")
    @DefaultOnly
    public List<String> bypassUUIDs = new ArrayList<>(Collections.singletonList("6c983d46-0631-48b8-9baf-5e33eb5ffec4"));

    @Comment("Requirements players must meet to be pass requirements\n"
            + "Only one option has to pass, for example [\"TwitchSubscriber()\", \"DiscordRole(...)\"] allows twitch subscribers and users with the specified role to play\n"
            + "while [\"TwitchSubscriber() && DiscordRole(...)\"] only allows twitch subscribers with the specified role to play\n"
            + "\n"
            + "Valid values are:\n"
            + "DiscordServer(Server ID)\n"
            + "DiscordBoosting(Server ID)\n"
            + "DiscordRole(Role ID)\n"
            + "\n"
            + "The following are available if you're using MinecraftAuth.me for linked accounts and a MinecraftAuth.me token is specified in the %1:\n"
            + "PatreonSubscriber() or PatreonSubscriber(Tier Title)\n"
            + "GlimpseSubscriber() or GlimpseSubscriber(Level Name)\n"
            + "TwitchFollower()\n"
            + "TwitchSubscriber() or TwitchSubscriber(Tier)\n"
            + "YouTubeSubscriber()\n"
            + "YouTubeMember() or YouTubeMember(Tier)\n"
            + "\n"
            + "The following operators are available:\n"
            + "&& = and, for example: \"DiscordServer(...) && TwitchFollower()\"\n"
            + "|| = or, for example \"DiscordBoosting(...) || YouTubeMember()\"\n"
            + "You can also use brackets () to clear ambiguity, for example: \"DiscordServer(...) && (TwitchSubscriber() || PatreonSubscriber())\"\n"
            + "allows a member of the specified Discord server that is also a twitch or patreon subscriber to join the server")
    @Constants.Comment({ConnectionConfig.FILE_NAME})
    public List<String> additionalRequirements = new ArrayList<>();
}
