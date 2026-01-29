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
import com.discordsrv.common.feature.groupsync.GroupSyncModule;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.permission.game.Permissions;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ResyncCommand extends CombinedCommand {

    private static final String LABEL = "resync";
    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "resync");
    private static final String TYPE_LABEL = "type";

    private static ResyncCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static ResyncCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new ResyncCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            ResyncCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal(LABEL)
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.resyncCommandDescription.minecraft()))
                    .requiredPermission(Permissions.COMMAND_RESYNC)
                    .then(
                            GameCommand.stringWord(TYPE_LABEL)
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

            CommandOption.Builder optionBuilder = CommandOption.builder(CommandOption.Type.STRING, TYPE_LABEL, "")
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.resyncSyncParameterDescription.discord().content()));
            for (Map.Entry<String, AbstractSyncModule<?, ?, ?, ?, ?>> sync : command.modulesByCommand.entrySet()) {
                optionBuilder.addChoice(sync.getValue().syncName(), sync.getKey());
            }

            DISCORD = DiscordCommand.chatInput(IDENTIFIER, LABEL, "")
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.resyncCommandDescription.discord().content()))
                    .addOption(optionBuilder.build())
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    private final Map<String, AbstractSyncModule<?, ?, ?, ?, ?>> modulesByCommand;

    public ResyncCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.modulesByCommand = getModulesByCommand(discordSRV);
    }

    private static Map<String, AbstractSyncModule<?, ?, ?, ?, ?>> getModulesByCommand(DiscordSRV discordSRV) {
        Map<String, AbstractSyncModule<?, ?, ?, ?, ?>> modulesByCommand = new HashMap<>();
        for (AbstractSyncModule<?, ?, ?, ?, ?> module : discordSRV.getModules(AbstractSyncModule.class, true)) {
            String command = module.syncCommand();
            modulesByCommand.put(command, module);
        }
        return modulesByCommand;
    }

    @Override
    public List<String> suggest(CommandExecution execution, @NotNull String input) {
        return modulesByCommand.keySet().stream()
                .filter(command -> command.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    @Override
    public void execute(CommandExecution execution) {
        String subCommand = execution.getString(TYPE_LABEL);
        AbstractSyncModule<?, ?, ?, ?, ?> module = modulesByCommand.get(subCommand);
        if (module == null) {
            execution.send(new Text("Unknown sync"));
            return;
        }
        if (module.isCurrentlyDisabled()) {
            execution.send(new Text("Sync is disabled"));
            return;
        }
        if (module instanceof GroupSyncModule && ((GroupSyncModule) module).noPermissionProvider()) {
            execution.send(new Text("No permission provider available.").withGameColor(NamedTextColor.RED));
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
