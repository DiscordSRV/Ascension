/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.command.discord.commands;

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.commands.*;
import com.discordsrv.common.command.discord.commands.subcommand.ExecuteCommand;
import com.discordsrv.common.config.main.DiscordCommandConfig;
import com.discordsrv.common.feature.linking.LinkStore;

public class DiscordSRVDiscordCommand {

    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "discordsrv");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            DiscordCommandConfig config = discordSRV.config().discordCommand;

            DiscordCommand.ChatInputBuilder builder = DiscordCommand.chatInput(IDENTIFIER, "discordsrv", "DiscordSRV related commands")
                    .addSubCommand(DebugCommand.getDiscord(discordSRV))
                    .addSubCommand(VersionCommand.getDiscord(discordSRV))
                    .addSubCommand(ResyncCommand.getDiscord(discordSRV))
                    .addSubCommand(LinkedCommand.getDiscord(discordSRV));

            if (config.execute.enabled) {
                builder = builder.addSubCommand(ExecuteCommand.get(discordSRV));
            }
            if (discordSRV.linkProvider() instanceof LinkStore) {
                builder = builder
                        .addSubCommand(LinkInitCommand.getDiscord(discordSRV))
                        .addSubCommand(UnlinkCommand.getDiscord(discordSRV));
            }

            INSTANCE = builder
                    .setGuildOnly(false)
                    .setDefaultPermission(DiscordCommand.DefaultPermission.ADMINISTRATOR)
                    .build();
        }

        return INSTANCE;
    }
}
