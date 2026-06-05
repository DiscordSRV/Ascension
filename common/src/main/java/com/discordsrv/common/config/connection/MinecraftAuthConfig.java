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

package com.discordsrv.common.config.connection;

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.feature.linking.impl.MinecraftAuthenticationLinker;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MinecraftAuthConfig {

    @Comment("If %1 connections are allowed for Discord linking (when linked-accounts.provider is \"auto\" or \"minecraftauth\").\n"
            + "Requires a connection to: %1\n"
            + "Privacy Policy: %2")
    @Constants.Comment({MinecraftAuthenticationLinker.DOMAIN, "https://" + MinecraftAuthenticationLinker.DOMAIN + "/privacy"})
    public boolean allow = true;

    @Comment("%1 token for checking subscription, following and membership statuses for required linking\n"
            + "You can get the token from %2 whilst logged in (please keep in mind that the token resets every time you visit that page)")
    @Constants.Comment({MinecraftAuthenticationLinker.DOMAIN, "https://" + MinecraftAuthenticationLinker.DOMAIN + "/api/token"})
    public String token = "";

}
