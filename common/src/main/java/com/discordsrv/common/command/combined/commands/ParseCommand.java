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
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.CommandUtil;

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
                    .then(GameCommand.stringWord("target")
                                  .then(GameCommand.stringGreedy("input").executor(command))
                    );
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            ParseCommand command = getInstance(discordSRV);

            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "parse"), "parse", "Parses input through DiscordSRV's PlaceholderService")
                    .addOption(CommandOption.builder(CommandOption.Type.STRING, "input", "The input to parse").setRequired(true).build())
                    .addOption(CommandOption.builder(CommandOption.Type.STRING, "player", "Context player (only one of player or user)").setRequired(false).build())
                    .addOption(CommandOption.builder(CommandOption.Type.USER, "user", "Context user (only one of player or user)").setRequired(false).build())
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    private final Logger logger;

    public ParseCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.logger = new NamedLogger(discordSRV, "PARSE_COMMAND");
    }

    @Override
    public void execute(CommandExecution execution) {
        String input = execution.getString("input");

        Task<Object> result = CommandUtil.lookupTarget(discordSRV, logger, execution, true, null, true)
                .then(lookup -> {
                    if (!lookup.isValid()) {
                        return Task.completed(null);
                    } else if (lookup.isPlayer()) {
                        IPlayer player = discordSRV.playerProvider().player(lookup.getPlayerUUID());
                        if (player != null) {
                            return Task.completed(player);
                        }

                        return discordSRV.playerProvider().lookupOfflinePlayer(lookup.getPlayerUUID()).thenApply(offlinePlayer -> offlinePlayer);
                    } else {
                        return discordSRV.discordAPI().retrieveUserById(lookup.getUserId()).thenApply(user -> user);
                    }
                });
        execution.runAsync(() -> result
                .whenComplete((context, t) -> {
                    if (t != null) {
                        logger.error("Failed to lookup target", t);
                    }

                    String output = discordSRV.placeholderService().replacePlaceholders(input, context);
                    execution.send(new Text(output));
                })
        );
    }
}
