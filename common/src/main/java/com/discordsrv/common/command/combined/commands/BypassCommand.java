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
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.CommandUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BypassCommand {

    private static BypassCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static BypassCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new BypassCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            BypassCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("bypass")
                    .requiredPermission(Permissions.COMMAND_DEBUG) // TODO
                    .then(GameCommand.literal("add").then(GameCommand.stringWord("player").executor(command.add)))
                    .then(GameCommand.literal("remove").then(GameCommand.stringWord("player").executor(command.remove)))
                    .then(GameCommand.literal("list").executor(command.list));
        }

        return GAME;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;
    protected final AddCommand add;
    protected final RemoveCommand remove;
    protected final ListCommand list;

    public BypassCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "BYPASS_COMMAND");
        this.add = new AddCommand(discordSRV);
        this.remove = new RemoveCommand(discordSRV);
        this.list = new ListCommand(discordSRV);
    }

    private RequiredLinkingModule<?> module(CommandExecution execution) {
        RequiredLinkingModule<?> module = discordSRV.getModule(RequiredLinkingModule.class);
        if (module == null) {
            execution.send(new Text("Not using required linking").withGameColor(NamedTextColor.RED));
        }
        return module;
    }

    private void mutate(CommandExecution execution, boolean add) {
        RequiredLinkingModule<?> module = module(execution);
        if (module == null) {
            return;
        }

        String player = execution.getArgument("player");
        CommandUtil.lookupPlayer(discordSRV, logger, execution, false, player, null)
                .whenFailed(t -> {
                    logger.error("Failed to lookup player", t);
                    execution.send(new Text("Failed to lookup player").withGameColor(NamedTextColor.RED));
                })
                .whenSuccessful(uuid -> {
                    if (module.isBypassingLinking(uuid) == add) {
                        execution.send(new Text(add ? "Already bypassing" : "Not bypassing").withGameColor(NamedTextColor.RED));
                        return;
                    }

                    if (add) {
                        module.addLinkingBypass(uuid);
                    } else {
                        module.removeLinkingBypass(uuid);
                    }
                    execution.send(new Text("Success").withGameColor(NamedTextColor.GREEN));
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
            RequiredLinkingModule<?> module = module(execution);
            if (module == null) {
                return;
            }

            Set<UUID> playerUuids = module.getBypassingPlayers();
            if (playerUuids.isEmpty()) {
                execution.send(new Text("None").withGameColor(NamedTextColor.RED));
                return;
            }

            List<Task<String>> tasks = new ArrayList<>();
            for (UUID playerUUID : playerUuids) {
                tasks.add(
                        discordSRV.playerProvider().lookupOfflinePlayer(playerUUID)
                                .thenApply(IOfflinePlayer::username)
                                .mapException(t -> playerUUID.toString())
                );
            }
            Task.allOf(tasks).whenSuccessful(players -> execution.send(new Text(String.join(", ", players))));
        }
    }
}
