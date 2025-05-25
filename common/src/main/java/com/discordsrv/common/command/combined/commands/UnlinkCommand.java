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
import com.discordsrv.common.feature.linking.LinkingModule;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.CommandUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class UnlinkCommand extends CombinedCommand {

    private static UnlinkCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD_WITH_OTHER;
    private static DiscordCommand DISCORD_WITHOUT_OTHER;

    private static UnlinkCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new UnlinkCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            UnlinkCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("unlink")
                    .then(
                            GameCommand.stringGreedy("target")
                                    .requiredPermission(Permissions.COMMAND_UNLINK_OTHER)
                                    .executor(command)
                    )
                    .requiredPermission(Permissions.COMMAND_UNLINK)
                    .executor(command);
        }

        return GAME;
    }

    public static DiscordCommand getDiscordWithOther(DiscordSRV discordSRV) {
        if (DISCORD_WITHOUT_OTHER == null) {
            DISCORD_WITHOUT_OTHER = getDiscord(discordSRV, true);
        }
        return DISCORD_WITHOUT_OTHER;
    }

    public static DiscordCommand getDiscordWithoutOther(DiscordSRV discordSRV) {
        if (DISCORD_WITH_OTHER == null) {
            DISCORD_WITH_OTHER = getDiscord(discordSRV, false);
        }
        return DISCORD_WITH_OTHER;
    }

    private static DiscordCommand getDiscord(DiscordSRV discordSRV, boolean withOther) {
        UnlinkCommand command = getInstance(discordSRV);

        DiscordCommand.ChatInputBuilder builder = DiscordCommand.chatInput(
                ComponentIdentifier.of("DiscordSRV", "unlink"),
                "unlink",
                "Unlink accounts"
        );

        if (withOther) {
            builder = builder.addOption(
                    CommandOption.builder(
                            CommandOption.Type.USER,
                            "user",
                            "The Discord user to unlink"
                    ).setRequired(false).build())
                    .addOption(CommandOption.builder(
                            CommandOption.Type.STRING,
                            "player",
                            "The Minecraft player username or UUID to unlink"
                    ).setRequired(false).build());
        }

        return builder.setEventHandler(command).build();
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
        if (linkProvider == null || !linkProvider.usesLocalLinking()) {
            execution.send(new Text("Cannot remove links with this link provider").withGameColor(NamedTextColor.DARK_RED));
            return;
        }

        LinkingModule module = discordSRV.getModule(LinkingModule.class);
        if (module == null) {
            execution.messages().unableToLinkAccountsAtThisTime.sendTo(execution);
            return;
        }

        execution.runAsync(() -> CommandUtil.lookupTarget(discordSRV, logger, execution, true, Permissions.COMMAND_UNLINK_OTHER)
                .whenComplete((result, t) -> {
                    if (t != null) {
                        logger.error("Failed to execute unlink command", t);
                        return;
                    }
                    if (!result.isValid()) {
                        return;
                    }

                    processResult(result, execution, linkProvider, module);
                })
        );
    }

    private void processResult(
            CommandUtil.TargetLookupResult result,
            CommandExecution execution,
            LinkProvider linkProvider,
            LinkingModule module
    ) {
        if (result.isPlayer()) {
            UUID playerUUID = result.getPlayerUUID();
            linkProvider.query(playerUUID)
                    .whenComplete((link, t) -> {
                        if (t != null) {
                            logger.error("Failed to query user", t);
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

                        handleUnlinkForPair(playerUUID, link.get().userId(), execution, module);
                    });
        } else {
            long userId = result.getUserId();
            linkProvider.query(result.getUserId())
                    .whenComplete((link, t) -> {
                        if (t != null) {
                            logger.error("Failed to query player", t);
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

                        handleUnlinkForPair(link.get().playerUUID(), userId, execution, module);
                    });
        }
    }

    private void handleUnlinkForPair(UUID player, Long user, CommandExecution execution, LinkingModule module) {
        module.unlink(player, user).whenComplete((v, t) -> {
            if (t != null) {
                logger.error("Failed to remove link", t);
                execution.messages().unableToLinkAccountsAtThisTime.sendTo(execution);
                return;
            }

            execution.messages().unlinkSuccess.sendTo(execution);
        });
    }
}
