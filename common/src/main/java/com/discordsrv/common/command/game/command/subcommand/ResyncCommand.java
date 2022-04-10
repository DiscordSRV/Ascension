/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.command.game.command.subcommand;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutor;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.groupsync.GroupSyncModule;
import com.discordsrv.common.groupsync.enums.GroupSyncCause;
import com.discordsrv.common.groupsync.enums.GroupSyncResult;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ResyncCommand implements GameCommandExecutor {

    private static GameCommand INSTANCE;

    public static GameCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            INSTANCE = GameCommand.literal("resync")
                    .requiredPermission("discordsrv.admin.resync")
                    .executor(new ResyncCommand(discordSRV));
        }

        return INSTANCE;
    }

    private final DiscordSRV discordSRV;

    public ResyncCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments) {
        GroupSyncModule module = discordSRV.getModule(GroupSyncModule.class);
        if (module == null) {
            sender.sendMessage(Component.text("GroupSync module has not initialized correctly.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("Synchronizing online players", NamedTextColor.GRAY));
        long startTime = System.currentTimeMillis();

        CompletableFutureUtil.combine(resyncOnlinePlayers(module))
                .whenComplete((results, t) -> {
                    EnumMap<GroupSyncResult, AtomicInteger> resultCounts = new EnumMap<>(GroupSyncResult.class);
                    int total = 0;
                    for (Set<GroupSyncResult> result : results) {
                        for (GroupSyncResult singleResult : result) {
                            total++;
                            resultCounts.computeIfAbsent(singleResult, key -> new AtomicInteger(0)).getAndIncrement();
                        }
                    }
                    String resultHover;
                    if (total == 0) {
                        resultHover = "Nothing done";
                    } else {
                        resultHover = total + " result" + (total == 1 ? "" : "s") + ":\n\n" +
                                resultCounts.entrySet().stream()
                                        .map(entry -> entry.getKey().toString() + ": " + entry.getValue().get())
                                        .collect(Collectors.joining("\n"));
                    }

                    long time = System.currentTimeMillis() - startTime;
                    sender.sendMessage(
                            Component.text("Synchronization completed in ", NamedTextColor.GRAY)
                                    .append(Component.text(time + "ms", NamedTextColor.GREEN))
                                    .append(Component.text(" (", NamedTextColor.GRAY))
                                    .append(Component.text(total, NamedTextColor.GREEN))
                                    .append(Component.text(" result" + (total == 1 ? "" : "s") + ")", NamedTextColor.GRAY))
                                    .hoverEvent(HoverEvent.showText(Component.text(resultHover)))
                    );
                });
    }

    private List<CompletableFuture<Set<GroupSyncResult>>> resyncOnlinePlayers(GroupSyncModule module) {
        List<CompletableFuture<Set<GroupSyncResult>>> futures = new ArrayList<>();
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            futures.add(module.resync(player.uniqueId(), GroupSyncCause.COMMAND));
        }
        return futures;
    }
}
