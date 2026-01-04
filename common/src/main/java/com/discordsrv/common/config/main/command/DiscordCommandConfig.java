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

package com.discordsrv.common.config.main.command;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class DiscordCommandConfig {

    @Comment("The Discord server id to limit the /discordsrv command to, disables the use of the command in DMs regardless of the below option\n"
            + "Set to 0 to disable, enabling the command in all servers the bot is in")
    public long managementCommandServerId = 0L;

    @Comment("If the /discordsrv command should be usable in the bot's direct messages")
    public boolean enableManagementCommandGlobally = false;

    @Comment("The alias of the command for users.\n"
            + "1-32 characters of letters, numbers and dashes")
    public String userCommandAlias = "minecraft";

    @Comment("The Discord server id to limit the user command to, disables the use of the command in DMs regardless of the below option\n"
            + "Set to 0 to disable, enabling the command in all servers the bot is in")
    public long userCommandServerId = 0L;

    @Comment("If the user command should be enabled in the bot's direct messages")
    public boolean enableUserCommandGlobally = true;
}
