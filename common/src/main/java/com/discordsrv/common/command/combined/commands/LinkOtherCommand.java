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
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.GameCommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.command.game.commands.subcommand.LinkInitGameCommand;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.linking.LinkStore;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.CommandUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LinkOtherCommand extends CombinedCommand {

    private static LinkOtherCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static LinkOtherCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new LinkOtherCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            GameCommandExecutor initCommand = LinkInitGameCommand.getExecutor(discordSRV);
            LinkOtherCommand otherCommand = getInstance(discordSRV);

            GAME = GameCommand.literal("link")
                    .then(
                            GameCommand.stringWord("player")
                                    .then(
                                            GameCommand.stringWord("user")
                                                    .requiredPermission(Permissions.COMMAND_LINK_OTHER)
                                                    .executor(otherCommand)
                                    )
                    )
                    .requiredPermission(Permissions.COMMAND_LINK)
                    .executor(initCommand);
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            LinkOtherCommand command = getInstance(discordSRV);
            ComponentIdentifier identifier = ComponentIdentifier.of("DiscordSRV", "link-other");

            DISCORD = DiscordCommand.chatInput(identifier, "link", "Link players")
                    .addOption(
                            CommandOption.builder(CommandOption.Type.USER, "user", "The user to link")
                                    .setRequired(true)
                                    .build()
                    )
                    .addOption(
                            CommandOption.builder(CommandOption.Type.STRING, "player", "The player to link")
                                    .setRequired(true)
                                    .build()
                    )
                    .setAutoCompleteHandler(command)
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;

    public LinkOtherCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "LINK_OTHER_COMMAND");
    }

    @Override
    public void execute(CommandExecution execution) {
        LinkProvider linkProvider = discordSRV.linkProvider();
        if (!(linkProvider instanceof LinkStore)) {
            execution.send(new Text("Cannot create links using this link provider").withGameColor(NamedTextColor.DARK_RED));
            return;
        }

        String playerArgument = execution.getArgument("player");
        String userArgument = execution.getArgument("user");
        if (execution instanceof GameCommandExecution) {
            ICommandSender sender = ((GameCommandExecution) execution).getSender();
            if (!sender.hasPermission(Permissions.COMMAND_LINK_OTHER)) {
                sender.sendMessage(discordSRV.messagesConfig(sender).noPermission.asComponent());
                return;
            }
        }

        CompletableFuture<UUID> playerUUIDFuture = CommandUtil.lookupPlayer(discordSRV, logger, execution, false, playerArgument, null);
        CompletableFuture<Long> userIdFuture = CommandUtil.lookupUser(discordSRV, logger, execution, false, userArgument, null);

        playerUUIDFuture.whenComplete((playerUUID, __) -> userIdFuture.whenComplete((userId, ___) -> {
            if (playerUUID == null) {
                execution.messages().playerNotFound(execution);
                return;
            }
            if (userId == null) {
                execution.messages().userNotFound(execution);
                return;
            }

            linkProvider.queryUserId(playerUUID).whenComplete((linkedUser, t) -> {
                if (t != null) {
                    logger.error("Failed to check linking status", t);
                    execution.messages().unableToCheckLinkingStatus(execution);
                    return;
                }
                if (linkedUser.isPresent()) {
                    return;
                }

                linkProvider.queryPlayerUUID(userId).whenComplete((linkedPlayer, t2) -> {
                    if (t2 != null) {
                        logger.error("Failed to check linking status", t2);
                        execution.messages().unableToCheckLinkingStatus(execution);
                        return;
                    }
                    if (linkedPlayer.isPresent()) {
                        return;
                    }

                    ((LinkStore) linkProvider).createLink(playerUUID, userId).whenComplete((v, t3) -> {
                        if (t3 != null) {
                            logger.error("Failed to create link", t3);
                            execution.send(
                                    execution.messages().minecraft.unableToLinkAtThisTime.asComponent(),
                                    execution.messages().discord.unableToCheckLinkingStatus.get()
                            );
                            return;
                        }

                        execution.messages().nowLinked3rd(discordSRV, execution, playerUUID, userId);
                    });
                });
            });
        }));
    }
}
