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

package com.discordsrv.common.config.main;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class DiscordInviteConfig {

    @Comment("Manually enter a invite url here, if this isn't set this is ignored and the options below will take effect")
    public String inviteUrl = "";

    @Comment("If the bot is only in one Discord server, it will attempt to get its vanity url")
    public boolean attemptToUseVanityUrl = true;

    @Comment("If the bot is only in one Discord server, it will attempt to automatically create a invite for it.\n"
            + "The bot will only attempt to do so if it has permission to \"Create Invite\"\n"
            + "The server must also have a rules channel (available for community servers) or default channel (automatically determined by Discord)")
    public boolean autoCreateInvite = true;

}
