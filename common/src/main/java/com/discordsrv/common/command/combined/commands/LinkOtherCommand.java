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

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.GameCommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.discord.DiscordCommandOptions;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.command.game.commands.subcommand.LinkInitGameCommand;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.profile.ProfileImpl;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.linking.LinkingModule;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.CommandUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class LinkOtherCommand extends CombinedCommand {

    private static final String LABEL = "link";
    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "link-other");

    private static LinkOtherCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static LinkOtherCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new LinkOtherCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            LinkProvider linkProvider = discordSRV.linkProvider();
            GameCommandExecutor initCommand = LinkInitGameCommand.getExecutor(discordSRV);

            GAME = GameCommand.literal(LABEL)
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.linkCommandDescription.minecraft()))
                    .requiredPermission(Permissions.COMMAND_LINK)
                    .executor(initCommand);

            if (linkProvider != null && linkProvider.usesLocalLinking()) {
                GAME = GAME.then(
                        GameCommand.player(discordSRV, CommandUtil.targetSuggestions(discordSRV, false, true, false))
                                .then(
                                        GameCommand.user(discordSRV, CommandUtil.targetSuggestions(discordSRV, true, false, false))
                                                .requiredPermission(Permissions.COMMAND_LINK_OTHER)
                                                .executor(getInstance(discordSRV))
                                )
                );
            }
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            LinkOtherCommand command = getInstance(discordSRV);

            DISCORD = DiscordCommand.chatInput(IDENTIFIER, LABEL, "")
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.linkOtherCommandDescription.content()))
                    .addOption(DiscordCommandOptions.user(discordSRV).setRequired(true).build())
                    .addOption(DiscordCommandOptions.player(discordSRV, player -> {
                        ProfileImpl profile = discordSRV.profileManager().getCachedProfile(player.uniqueId());
                        return profile == null || !profile.isLinked();
                    }).setRequired(true).build())
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
        if (linkProvider == null || !linkProvider.usesLocalLinking()) {
            execution.send(new Text("Cannot create links using this link provider").withGameColor(NamedTextColor.DARK_RED));
            return;
        }

        LinkingModule module = discordSRV.getModule(LinkingModule.class);
        if (module == null) {
            execution.messages().unableToLinkAccountsAtThisTime.sendTo(execution);
            return;
        }

        String playerArgument = execution.getString("player");
        String userArgument = execution.getString("user");
        if (execution instanceof GameCommandExecution) {
            ICommandSender sender = ((GameCommandExecution) execution).getSender();
            if (!sender.hasPermission(Permissions.COMMAND_LINK_OTHER)) {
                execution.messages().noPermission.sendTo(execution);
                return;
            }
        }

        Task<UUID> playerUUIDFuture = CommandUtil.lookupPlayer(discordSRV, logger, execution, false, playerArgument, null, false);
        Task<Long> userIdFuture = CommandUtil.lookupUser(discordSRV, logger, execution, false, userArgument, null, false);

        playerUUIDFuture.whenComplete((playerUUID, __) -> userIdFuture.whenComplete((userId, ___) -> {
            if (playerUUID == null) {
                return;
            }
            if (userId == null) {
                return;
            }

            linkProvider.query(playerUUID).whenComplete((existingPlayerLink, t1) -> {
                if (t1 != null) {
                    logger.error("Failed to check linking status", t1);
                    execution.messages().unableToCheckLinkingStatus.sendTo(execution);
                    return;
                }
                if (existingPlayerLink.isPresent()) {
                    execution.messages().playerAlreadyLinked3rd.sendTo(execution);
                    return;
                }

                linkProvider.query(userId).whenComplete((existingUserLink, t2) -> {
                    if (t2 != null) {
                        logger.error("Failed to check linking status", t2);
                        execution.messages().unableToCheckLinkingStatus.sendTo(execution);
                        return;
                    }
                    if (existingUserLink.isPresent()) {
                        execution.messages().userAlreadyLinked3rd.sendTo(execution);
                        return;
                    }

                    module.link(playerUUID, userId).whenComplete((v, t3) -> {
                        if (t3 != null) {
                            logger.error("Failed to create link", t3);
                            execution.messages().unableToLinkAccountsAtThisTime.sendTo(execution);
                            return;
                        }

                        execution.messages().nowLinked3rd.sendTo(execution, discordSRV, userId, playerUUID);
                    });
                });
            });
        }));
    }
}
