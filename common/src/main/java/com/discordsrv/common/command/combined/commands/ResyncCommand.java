package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.discord.entity.interaction.command.Command;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.GameCommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.groupsync.GroupSyncModule;
import com.discordsrv.common.groupsync.enums.GroupSyncCause;
import com.discordsrv.common.groupsync.enums.GroupSyncResult;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ResyncCommand extends CombinedCommand {

    private static ResyncCommand INSTANCE;
    private static GameCommand GAME;
    private static Command DISCORD;

    private static ResyncCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new ResyncCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            ResyncCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("resync")
                    .requiredPermission("discordsrv.admin.resync")
                    .executor(command);
        }

        return GAME;
    }

    public static Command getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            ResyncCommand command = getInstance(discordSRV);
            DISCORD = Command.chatInput(ComponentIdentifier.of("DiscordSRV", "resync"), "resync", "Perform group resync for online players")
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

            CompletableFutureUtil.combine(resyncOnlinePlayers(module))
                    .whenComplete((results, t) -> {
                        EnumMap<GroupSyncResult, AtomicInteger> resultCounts = new EnumMap<>(GroupSyncResult.class);
                        int total = 0;
                        for (List<GroupSyncResult> result : results) {
                            for (GroupSyncResult singleResult : result) {
                                total++;
                                resultCounts.computeIfAbsent(singleResult, key -> new AtomicInteger(0)).getAndIncrement();
                            }
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

    private List<CompletableFuture<List<GroupSyncResult>>> resyncOnlinePlayers(GroupSyncModule module) {
        List<CompletableFuture<List<GroupSyncResult>>> futures = new ArrayList<>();
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            futures.add(module.resync(player.uniqueId(), GroupSyncCause.COMMAND));
        }
        return futures;
    }
}
