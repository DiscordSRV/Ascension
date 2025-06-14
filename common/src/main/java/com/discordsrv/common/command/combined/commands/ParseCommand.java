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

package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.*;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.permission.game.Permissions;

public class ParseCommand extends CombinedCommand {

    private static ParseCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static ParseCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new ParseCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            ParseCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("parse")
                    .requiredPermission(Permissions.COMMAND_PARSE)
                    .then(
                            GameCommand.booleanArgument("self")
                                            .then(
                                                    GameCommand.stringGreedy("input")
                                                            .executor(command)
                                            )
                    );
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            ParseCommand command = getInstance(discordSRV);

            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "parse"), "parse", "Parses input through DiscordSRV's PlaceholderService")
                    .addOption(CommandOption.builder(CommandOption.Type.BOOLEAN, "self", "Include self as context").setRequired(true).build())
                    .addOption(CommandOption.builder(CommandOption.Type.STRING, "input", "The input to parse").setRequired(true).build())
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    public ParseCommand(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void execute(CommandExecution execution) {
        boolean self = execution.getBoolean("self");
        String input = execution.getString("input");

        Object context;
        if (self) {
            context = execution instanceof GameCommandExecution
                      ? ((GameCommandExecution) execution).getSender()
                      : ((DiscordCommandExecution) execution).getUser();
        } else {
            context = null;
        }

        execution.runAsync(() -> {
            String output = discordSRV.placeholderService().replacePlaceholders(input, context);
            execution.send(new Text(output));
        });
    }
}
