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
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.discord.DiscordCommandOptions;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.profile.ProfileImpl;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.CommandUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class LinkedCommand extends CombinedCommand {

    private static final String LABEL = "linked";
    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "linked");

    private static LinkedCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static LinkedCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new LinkedCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            LinkedCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal(LABEL)
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.linkedCommandDescription.minecraft()))
                    .then(GameCommand.target(discordSRV, CommandUtil.targetSuggestions(discordSRV, true, true, true))
                                  .requiredPermission(Permissions.COMMAND_LINKED_OTHER)
                                  .executor(command))
                    .requiredPermission(Permissions.COMMAND_LINKED)
                    .executor(command);
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            LinkedCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(IDENTIFIER, LABEL, "")
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.linkedCommandDescription.discord().content()))
                    .addOption(DiscordCommandOptions.user(discordSRV).setRequired(false).build())
                    .addOption(DiscordCommandOptions.player(discordSRV, player -> {
                        ProfileImpl profile = discordSRV.profileManager().getProfile(player.uniqueId());
                        return profile == null || profile.isLinked();
                    }).setRequired(false).build())
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

        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider == null) {
            execution.send(new Text("Cannot check linking status using this link provider").withGameColor(NamedTextColor.DARK_RED));
            return;
        }

        execution.runAsync(() -> CommandUtil.lookupTarget(discordSRV, logger, execution, true, Permissions.COMMAND_LINKED_OTHER, false)
                .whenComplete((result, t) -> {
                    if (t != null) {
                        logger.error("Failed to execute linked command", t);
                        return;
                    }
                    if (result.isValid()) {
                        processResult(result, execution, linkProvider);
                    }
                })
        );
    }

    private void processResult(
            CommandUtil.TargetLookupResult result,
            CommandExecution execution,
            LinkProvider linkProvider
    ) {
        if (result.isPlayer()) {
            UUID playerUUID = result.getPlayerUUID();

            linkProvider.get(playerUUID).whenComplete((link, t) -> {
                if (t != null) {
                    logger.error("Failed to check linking status during linked command", t);
                    execution.messages().unableToCheckLinkingStatus.sendTo(execution);
                    return;
                }
                if (!link.isPresent()) {
                    (result.isSelf()
                     ? execution.messages().alreadyUnlinked1st
                     : execution.messages().minecraftPlayerUnlinked3rd
                    ).sendTo(execution, discordSRV, null, playerUUID);
                    return;
                }

                (result.isSelf()
                 ? execution.messages().linkedTo1st
                 : execution.messages().minecraftPlayerLinkedTo3rd
                ).sendTo(execution, discordSRV, link.get().userId(), playerUUID);
            });
        } else {
            long userId = result.getUserId();

            linkProvider.get(userId).whenComplete((link, t) -> {
                if (t != null) {
                    logger.error("Failed to check linking status during linked command", t);
                    execution.messages().unableToCheckLinkingStatus.sendTo(execution);
                    return;
                }
                if (!link.isPresent()) {
                    (result.isSelf()
                     ? execution.messages().alreadyUnlinked1st
                     : execution.messages().discordUserUnlinked3rd
                    ).sendTo(execution, discordSRV, userId, null);
                    return;
                }

                (result.isSelf()
                 ? execution.messages().linkedTo1st
                 : execution.messages().discordUserLinkedTo3rd
                ).sendTo(execution, discordSRV, userId, link.get().playerUUID());
            });
        }
    }
}
