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

package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.util.CommandUtil;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.permission.Permission;

import java.util.UUID;

public class LinkedCommand extends CombinedCommand {

    private static LinkedCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static LinkedCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new LinkedCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            LinkedCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("linked")
                    .then(
                            GameCommand.stringGreedy("target")
                                    .requiredPermission(Permission.COMMAND_LINKED_OTHER)
                                    .executor(command)
                    )
                    .requiredPermission(Permission.COMMAND_LINKED)
                    .executor(command);
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            LinkedCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "linked"), "linked", "Check the linking status of accounts")
                    .addOption(CommandOption.builder(
                            CommandOption.Type.USER,
                            "user",
                            "The Discord user to check the linking status of"
                    ).setRequired(false).build())
                    .addOption(CommandOption.builder(
                            CommandOption.Type.STRING,
                            "player",
                            "The Minecraft player username or UUID to check the linking status of"
                    ).setRequired(false).build())
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    private final Logger logger;

    public LinkedCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.logger = new NamedLogger(discordSRV, "LINKED_COMMAND");
    }

    @Override
    public void execute(CommandExecution execution) {
        execution.setEphemeral(true);

        execution.runAsync(() -> CommandUtil.lookupTarget(discordSRV, logger, execution, true, Permission.COMMAND_LINKED_OTHER)
                .whenComplete((result, t) -> {
                    if (t != null) {
                        logger.error("Failed to execute linked command", t);
                        return;
                    }
                    if (result.isValid()) {
                        processResult(result, execution);
                    }
                })
        );
    }

    private void processResult(CommandUtil.TargetLookupResult result, CommandExecution execution) {
        if (result.isPlayer()) {
            UUID playerUUID = result.getPlayerUUID();

            discordSRV.linkProvider().getUserId(playerUUID).whenComplete((optUserId, t) -> {
                if (t != null) {
                    logger.error("Failed to check linking status during linked command", t);
                    execution.messages().unableToCheckLinkingStatus(execution);
                    return;
                }
                if (!optUserId.isPresent()) {
                    execution.messages().minecraftPlayerUnlinked(discordSRV, execution, playerUUID);
                    return;
                }

                long userId = optUserId.get();
                execution.messages().minecraftPlayerLinkedTo(discordSRV, execution, playerUUID, userId);
            });
        } else {
            long userId = result.getUserId();

            discordSRV.linkProvider().getPlayerUUID(userId).whenComplete((optPlayerUUID, t) -> {
                if (t != null) {
                    logger.error("Failed to check linking status during linked command", t);
                    execution.send(
                            execution.messages().minecraft.unableToCheckLinkingStatus.asComponent(),
                            execution.messages().discord.unableToCheckLinkingStatus
                    );
                    return;
                }
                if (!optPlayerUUID.isPresent()) {
                    execution.messages().discordUserUnlinked(discordSRV, execution, userId);
                    return;
                }

                UUID playerUUID = optPlayerUUID.get();
                execution.messages().discordUserLinkedTo(discordSRV, execution, playerUUID, userId);
            });
        }
    }
}
