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

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
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
import com.discordsrv.common.feature.groupsync.GroupSyncModule;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.util.CompletableFutureUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
                    .requiredPermission(Permission.COMMAND_RESYNC)
                    .executor(command);
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            ResyncCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "resync"), "resync", "Perform group resync for online players")
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    public ResyncCommand(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void execute(CommandExecution execution) {
        GroupSyncModule module = discordSRV.getModule(GroupSyncModule.class);
        if (module == null) {
            execution.send(new Text("GroupSync module has not initialized correctly.").withGameColor(NamedTextColor.RED));
            return;
        }

        if (module.noPermissionProvider()) {
            execution.send(new Text("No permission provider available.").withGameColor(NamedTextColor.RED));
            return;
        }

        if (execution instanceof GameCommandExecution) {
            // Acknowledge for in-game runs
            execution.send(new Text("Synchronizing online players").withGameColor(NamedTextColor.GRAY));
        }

        execution.runAsync(() -> {
            long startTime = System.currentTimeMillis();

            List<CompletableFuture<? extends SyncSummary<?>>> futures = resyncOnlinePlayers(module);
            CompletableFutureUtil.combineGeneric(futures).thenCompose(result -> {
                List<CompletableFuture<?>> results = new ArrayList<>();
                for (SyncSummary<?> summary : result) {
                    results.add(summary.resultFuture());
                }
                return CompletableFutureUtil.combineGeneric(results);
            }).whenComplete((__, t) -> {
                Map<ISyncResult, AtomicInteger> resultCounts = new HashMap<>();
                int total = 0;

                List<ISyncResult> results = new ArrayList<>();
                for (CompletableFuture<? extends SyncSummary<?>> future : futures) {
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

    private List<CompletableFuture<? extends SyncSummary<?>>> resyncOnlinePlayers(AbstractSyncModule<?, ?, ?, ?, ?> module) {
        List<CompletableFuture<? extends SyncSummary<?>>> summaries = new ArrayList<>();
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            summaries.add(module.resyncAll(GenericSyncCauses.COMMAND, Someone.of(player)));
        }
        return summaries;
    }
}
