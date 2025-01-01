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
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.linking.LinkStore;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.util.CommandUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class UnlinkCommand extends CombinedCommand {

    private static UnlinkCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static UnlinkCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new UnlinkCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            UnlinkCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("unlink")
                    .then(
                            GameCommand.stringGreedy("target")
                                    .requiredPermission(Permission.COMMAND_UNLINK_OTHER)
                                    .executor(command)
                    )
                    .requiredPermission(Permission.COMMAND_UNLINK)
                    .executor(command);
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            UnlinkCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "unlink"), "unlink", "Unlink accounts")
                    .addOption(CommandOption.builder(
                            CommandOption.Type.USER,
                            "user",
                            "The Discord user to unlink"
                    ).setRequired(false).build())
                    .addOption(CommandOption.builder(
                            CommandOption.Type.STRING,
                            "player",
                            "The Minecraft player username or UUID to unlink"
                    ).setRequired(false).build())
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    private final Logger logger;

    public UnlinkCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.logger = new NamedLogger(discordSRV, "UNLINK_COMMAND");
    }

    @Override
    public void execute(CommandExecution execution) {
        execution.setEphemeral(true);

        LinkProvider linkProvider = discordSRV.linkProvider();
        if (!(linkProvider instanceof LinkStore)) {
            execution.send(new Text("Cannot remove links with this link provider").withGameColor(NamedTextColor.DARK_RED));
            return;
        }

        execution.runAsync(() -> CommandUtil.lookupTarget(discordSRV, logger, execution, true, Permission.COMMAND_UNLINK_OTHER)
                .whenComplete((result, t) -> {
                    if (t != null) {
                        logger.error("Failed to execute linked command", t);
                        return;
                    }
                    if (result.isValid()) {
                        processResult(result, execution, (LinkStore) linkProvider);
                    } else {
                        execution.send(new Text("Invalid target"));
                    }
                })
        );
    }

    private void processResult(CommandUtil.TargetLookupResult result, CommandExecution execution, LinkStore linkStore) {
        if (result.isPlayer()) {
            UUID playerUUID = result.getPlayerUUID();
            discordSRV.linkProvider().queryUserId(playerUUID)
                    .whenComplete((user, t) -> {
                        if (t != null) {
                            logger.error("Failed to query user", t);
                            execution.messages().unableToCheckLinkingStatus(execution);
                            return;
                        }
                        if (!user.isPresent()) {
                            execution.messages().minecraftPlayerUnlinked(discordSRV, execution, playerUUID);
                            return;
                        }

                        handleUnlinkForPair(playerUUID, user.get(), execution, linkStore);
                    });
        } else {
            long userId = result.getUserId();
            discordSRV.linkProvider().queryPlayerUUID(result.getUserId())
                    .whenComplete((player, t) -> {
                        if (t != null) {
                            logger.error("Failed to query player", t);
                            execution.messages().unableToCheckLinkingStatus(execution);
                            return;
                        }
                        if (!player.isPresent()) {
                            execution.messages().discordUserUnlinked(discordSRV, execution, userId);
                            return;
                        }

                        handleUnlinkForPair(player.get(), userId, execution, linkStore);
                    });
        }
    }

    private void handleUnlinkForPair(UUID player, Long user, CommandExecution execution, LinkStore linkStore) {
        linkStore.removeLink(player, user).whenComplete((v, t2) -> {
            if (t2 != null) {
                logger.error("Failed to remove link", t2);
                execution.send(
                        execution.messages().minecraft.unableToLinkAtThisTime.asComponent(),
                        execution.messages().discord.unableToCheckLinkingStatus.get()
                );
                return;
            }

            execution.messages().unlinked(execution);
        });
    }
}
