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
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.SyncSummary;
import com.discordsrv.common.abstraction.sync.cause.GenericSyncCauses;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.GameCommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.feature.bansync.BanSyncModule;
import com.discordsrv.common.feature.groupsync.GroupSyncModule;
import com.discordsrv.common.feature.nicknamesync.NicknameSyncModule;
import com.discordsrv.common.feature.onlinerole.OnlineRoleModule;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.permission.game.Permissions;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResyncCommand extends CombinedCommand {

    private static ResyncCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static ResyncCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new ResyncCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            ResyncCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("resync")
                    .requiredPermission(Permissions.COMMAND_RESYNC)
                    .then(
                            GameCommand.stringWord("type")
                                    .requiredPermission(Permissions.COMMAND_RESYNC)
                                    .executor(command)
                                    .suggester(command)
                    );
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            ResyncCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(
                        ComponentIdentifier.of("DiscordSRV", "resync"),
                        "resync",
                        "Perform group resync for online players"
                    )
                    .addOption(
                            CommandOption.builder(CommandOption.Type.STRING, "type", "The type of sync to run")
                                    .addChoice("Group Sync", "group")
                                    .addChoice("Ban Sync", "ban")
                                    .addChoice("Nickname Sync", "nickname")
                                    .addChoice("Online Role Sync", "onlinerole")
                                    .build()
                    )
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    public ResyncCommand(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public List<String> suggest(CommandExecution execution, @Nullable String input) {
        if (input == null) {
            return Collections.emptyList();
        }

        return Stream.of("ban", "group", "nickname", "onlinerole")
                .filter(command -> command.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    @Override
    public void execute(CommandExecution execution) {
        AbstractSyncModule<?, ?, ?, ?, ?> module;
        switch (execution.getArgument("type")) {
            case "group":
                GroupSyncModule groupSyncModule = discordSRV.getModule(GroupSyncModule.class);
                if (groupSyncModule != null && groupSyncModule.noPermissionProvider()) {
                    execution.send(new Text("No permission provider available.").withGameColor(NamedTextColor.RED));
                    return;
                }

                module = groupSyncModule;
                break;
            case "ban":
                module = discordSRV.getModule(BanSyncModule.class);
                break;
            case "nickname":
                module = discordSRV.getModule(NicknameSyncModule.class);
                break;
            case "onlinerole":
                module = discordSRV.getModule(OnlineRoleModule.class);
                break;
            default:
                execution.send(new Text("Unexpected type"));
                return;
        }
        if (module == null) {
            execution.send(new Text("Module has not initialized correctly.").withGameColor(NamedTextColor.RED));
            return;
        }

        if (module.disabledOnAllConfigs(config -> config.tieBreakers.resyncCommand)) {
            execution.send(new Text("Command disabled for this sync type").withGameColor(NamedTextColor.RED));
            return;
        }

        if (execution instanceof GameCommandExecution) {
            // Acknowledge for in-game runs
            execution.send(new Text("Synchronizing online players").withGameColor(NamedTextColor.GRAY));
        }

        execution.runAsync(() -> {
            long startTime = System.currentTimeMillis();

            List<Task<? extends SyncSummary<?>>> futures = resyncOnlinePlayers(module);
            Task.allOf(futures).then(result -> {
                List<Task<?>> results = new ArrayList<>();
                for (SyncSummary<?> summary : result) {
                    results.add(summary.resultFuture());
                }
                return Task.allOf(results);
            }).whenComplete((__, t) -> {
                Map<ISyncResult, AtomicInteger> resultCounts = new HashMap<>();
                int total = 0;

                List<ISyncResult> results = new ArrayList<>();
                for (Task<? extends SyncSummary<?>> future : futures) {
                    SyncSummary<?> summary = future.join();
                    ISyncResult allFailResult = summary.allFailReason();
                    if (allFailResult != null) {
                        results.add(allFailResult);
                        continue;
                    }

                    results.addAll(summary.resultFuture().join().values());
                }

                for (ISyncResult result : results) {
                    total++;
                    resultCounts.computeIfAbsent(result, key -> new AtomicInteger(0)).getAndIncrement();
                }
                String resultHover = resultCounts.entrySet().stream()
                                    .map(entry -> entry.getKey().toString() + ": " + entry.getValue().get())
                                    .collect(Collectors.joining("\n"));

                long time = System.currentTimeMillis() - startTime;
                execution.send(
                        Arrays.asList(
                                new Text("Synchronization completed in ").withGameColor(NamedTextColor.GRAY),
                                new Text(time + "ms").withGameColor(NamedTextColor.GREEN).withFormatting(Text.Formatting.BOLD),
                                new Text(" (").withGameColor(NamedTextColor.GRAY),
                                new Text(total + " result" + (total == 1 ? "" : "s"))
                                        .withGameColor(NamedTextColor.GREEN)
                                        .withDiscordFormatting(Text.Formatting.BOLD),
                                new Text(")").withGameColor(NamedTextColor.GRAY)
                        ),
                        total > 0
                            ? Collections.singletonList(new Text(resultHover))
                            : (execution instanceof GameCommandExecution ? Collections.singletonList(new Text("Nothing done")) : Collections.emptyList())
                );
            });
        });
    }

    private List<Task<? extends SyncSummary<?>>> resyncOnlinePlayers(AbstractSyncModule<?, ?, ?, ?, ?> module) {
        List<Task<? extends SyncSummary<?>>> summaries = new ArrayList<>();
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            summaries.add(module.resyncAll(GenericSyncCauses.COMMAND, Someone.of(discordSRV, player), config -> config.tieBreakers.resyncCommand));
        }
        return summaries;
    }
}
