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

package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.command.SubCommandGroup;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.DiscordCommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.discord.DiscordCommandOptions;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.ignore.IgnoreModule;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.CommandUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class IgnoreCommand {

    private static final String LABEL = "ignore";
    private static final ComponentIdentifier IDENTIFIER_ADD = ComponentIdentifier.of("DiscordSRV", "ignore-add");
    private static final ComponentIdentifier IDENTIFIER_REMOVE = ComponentIdentifier.of("DiscordSRV", "ignore-remove");
    private static final ComponentIdentifier IDENTIFIER_LIST = ComponentIdentifier.of("DiscordSRV", "ignore-list");
    private static final String ADD_LABEL = "add";
    private static final String REMOVE_LABEL = "remove";
    private static final String LIST_LABEL = "list";

    private static IgnoreCommand INSTANCE;
    private static GameCommand GAME;
    private static SubCommandGroup DISCORD;

    private static IgnoreCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new IgnoreCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            IgnoreCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal(LABEL)
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.ignoreCommandDescription.minecraft()))
                    .requiredPermission(Permissions.COMMAND_IGNORE)
                    .then(GameCommand.literal(ADD_LABEL)
                                  .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.ignoreAddCommandDescription.minecraft()))
                                  .then(GameCommand.target(discordSRV, null).executor(command.add)))
                    .then(GameCommand.literal(REMOVE_LABEL)
                                  .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.ignoreRemoveCommandDescription.minecraft()))
                                  .then(GameCommand.target(discordSRV, null).executor(command.remove)))
                    .then(GameCommand.literal(LIST_LABEL)
                                  .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.ignoreListCommandDescription.minecraft()))
                                  .executor(command.list));
        }

        return GAME;
    }

    public static SubCommandGroup getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            IgnoreCommand command = getInstance(discordSRV);
            DISCORD = SubCommandGroup.builder(LABEL, "")
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.ignoreCommandDescription.discord().content()))
                    .addCommand(DiscordCommand.chatInput(IDENTIFIER_ADD, ADD_LABEL, "")
                                        .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.ignoreAddCommandDescription.discord().content()))
                                        .addOption(DiscordCommandOptions.user(discordSRV).setRequired(false).build())
                                        .addOption(DiscordCommandOptions.player(discordSRV, player -> !module(discordSRV).getIgnoredPlayers().contains(player.uniqueId())).setRequired(false).build())
                                        .setEventHandler(command.add)
                                        .build())
                    .addCommand(DiscordCommand.chatInput(IDENTIFIER_REMOVE, REMOVE_LABEL, "")
                                        .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.ignoreRemoveCommandDescription.discord().content()))
                                        .addOption(DiscordCommandOptions.user(discordSRV).setRequired(false).build())
                                        .addOption(DiscordCommandOptions.player(discordSRV, player -> module(discordSRV).getIgnoredPlayers().contains(player.uniqueId())).setRequired(false).build())
                                        .setEventHandler(command.remove)
                                        .build())
                    .addCommand(DiscordCommand.chatInput(IDENTIFIER_LIST, LIST_LABEL, "")
                                        .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.ignoreListCommandDescription.discord().content()))
                                        .setEventHandler(command.list)
                                        .build())
                    .build();
        }

        return DISCORD;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;

    protected final AddCommand add;
    protected final RemoveCommand remove;
    protected final ListCommand list;

    public IgnoreCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "IGNORE_COMMAND");

        this.add = new AddCommand(discordSRV);
        this.remove = new RemoveCommand(discordSRV);
        this.list = new ListCommand(discordSRV);
    }

    private static IgnoreModule module(DiscordSRV discordSRV) {
        return discordSRV.getModule(IgnoreModule.class);
    }

    private IgnoreModule module(CommandExecution execution) {
        IgnoreModule module = module(discordSRV);
        if (module == null) {
            execution.send(new Text("Ignore module is not enabled").withGameColor(NamedTextColor.RED));
        }
        return module;
    }

    private void mutate(CommandExecution execution, boolean add) {
        IgnoreModule module = module(execution);
        if (module == null) {
            return;
        }

        CommandUtil.lookupTarget(discordSRV, logger, execution, false, null, false)
                .whenSuccessful(lookupResult -> {
                    if (!lookupResult.isValid()) {
                        // Executor already notified via lookupTarget
                        return;
                    }

                    if (lookupResult.isPlayer()) {
                        UUID playerUUID = lookupResult.getPlayerUUID();
                        boolean wasIgnored = module.getIgnoredPlayers().contains(playerUUID);

                        if (add) {
                            if (wasIgnored) {
                                execution.messages().alreadyIgnoring.sendTo(execution);
                                return;
                            }
                            execution.messages().playerIgnoreAdded.sendTo(execution, discordSRV, null, playerUUID);
                            module.getIgnoredPlayers().add(playerUUID);
                        } else {
                            if (!wasIgnored) {
                                execution.messages().notIgnoring.sendTo(execution);
                                return;
                            }
                            execution.messages().playerIgnoreRemoved.sendTo(execution, discordSRV, null, playerUUID);
                            module.getIgnoredPlayers().remove(playerUUID);
                        }
                    } else if (lookupResult.isUser()) {
                        long userId = lookupResult.getUserId();
                        boolean wasIgnored = module.getIgnoredDiscordUsers().contains(userId);

                        if (add) {
                            if (wasIgnored) {
                                execution.messages().alreadyIgnoring.sendTo(execution);
                                return;
                            }
                            execution.messages().userIgnoreAdded.sendTo(execution, discordSRV, userId, null);
                            module.getIgnoredDiscordUsers().add(userId);
                        } else {
                            if (!wasIgnored) {
                                execution.messages().notIgnoring.sendTo(execution);
                                return;
                            }
                            execution.messages().userIgnoreRemoved.sendTo(execution, discordSRV, userId, null);
                            module.getIgnoredDiscordUsers().remove(userId);
                        }
                    }
                });
    }

    public class AddCommand extends CombinedCommand {

        public AddCommand(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        @Override
        public void execute(CommandExecution execution) {
            mutate(execution, true);
        }
    }

    public class RemoveCommand extends CombinedCommand {

        public RemoveCommand(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        @Override
        public void execute(CommandExecution execution) {
            mutate(execution, false);
        }
    }

    public class ListCommand extends CombinedCommand {

        public ListCommand(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        @Override
        public void execute(CommandExecution execution) {
            IgnoreModule module = module(execution);
            if (module == null) {
                return;
            }

            if (module.getIgnoredPlayers().isEmpty() && module.getIgnoredDiscordUsers().isEmpty()) {
                execution.send(new Text("Ignore list is empty").withGameColor(NamedTextColor.YELLOW));
                return;
            }

            List<String> ignoredItems = new ArrayList<>();
            Set<UUID> playerUuids = module.getIgnoredPlayers();
            List<Task<String>> playerTasks = new ArrayList<>();
            for (UUID playerUUID : playerUuids) {
                playerTasks.add(
                        discordSRV.playerProvider().lookupOfflinePlayer(playerUUID)
                                .thenApply(IOfflinePlayer::username)
                                .mapException(t -> playerUUID.toString())
                );
            }

            Task.allOf(playerTasks).whenComplete((playerNames, __) -> {
                if (playerNames != null && !playerNames.isEmpty()) {
                    ignoredItems.add("Minecraft players:\n" + String.join("\n", playerNames));
                }
                if (!module.getIgnoredDiscordUsers().isEmpty()) {
                    ignoredItems.add("Discord users:\n" + module.getIgnoredDiscordUsers().stream()
                            .map(id -> {
                                if (execution instanceof DiscordCommandExecution) {
                                    DiscordUser user = discordSRV.discordAPI().getUserById(id);
                                    return (user != null ? user.getAsTag() + " - " : "") + id;
                                } else {
                                    return id.toString();
                                }
                            }).collect(Collectors.joining("\n")));
                }

                execution.send(new Text(String.join("\n\n", ignoredItems)));
            });
        }
    }
}
