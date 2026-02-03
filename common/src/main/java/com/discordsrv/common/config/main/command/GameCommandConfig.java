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

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class GameCommandConfig {

    @Comment("If the %1 command should be set by DiscordSRV")
    @Constants.Comment("/discord")
    public boolean useDiscordCommand = true;

    @Comment("If %1 should be used as an alias for %2")
    @Constants.Comment({"/link", "/discord link"})
    public boolean useLinkAlias = false;

    @Comment("The Discord command response format (%1)\n"
            + "Suggested placeholders:\n"
            + "%discord_invite% - The Discord server invite, as configured in the \"%2\" section of the config\n"
            + "More placeholders at %3 (Player)")
    @Constants.Comment({"/discord", "invite", DocumentationURLs.PLACEHOLDERS})
    public String discordFormat = "[click:open_url:%discord_invite%][color:aqua][bold:on]Click here [color][bold][color:green]to join our Discord server!";
}
