/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.command.game.commands.subcommand;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutor;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.LinkStore;
import com.discordsrv.common.player.IPlayer;
import com.github.benmanes.caffeine.cache.Cache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class LinkCommand implements GameCommandExecutor {

    private static GameCommand INSTANCE;

    public static GameCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            INSTANCE = GameCommand.literal("link")
                    .requiredPermission("discordsrv.player.link")
                    .executor(new LinkCommand(discordSRV));
        }

        return INSTANCE;
    }

    private final DiscordSRV discordSRV;
    private final Cache<UUID, Boolean> linkCheckRateLimit;

    public LinkCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.linkCheckRateLimit = discordSRV.caffeineBuilder()
                .expireAfterWrite(LinkStore.LINKING_CODE_RATE_LIMIT)
                .build();
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments, String label) {
        if (!(sender instanceof IPlayer)) {
            sender.sendMessage(Component.text("Player only command").color(NamedTextColor.RED));
            return;
        }

        IPlayer player = (IPlayer) sender;
        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider.getCachedUserId(player.uniqueId()).isPresent()) {
            player.sendMessage(discordSRV.messagesConfig(player).alreadyLinked.asComponent());
            return;
        }

        if (linkCheckRateLimit.getIfPresent(player.uniqueId()) != null) {
            player.sendMessage(discordSRV.messagesConfig(player).pleaseWaitBeforeRunningThatCommandAgain.asComponent());
            return;
        }
        linkCheckRateLimit.put(player.uniqueId(), true);

        sender.sendMessage(discordSRV.messagesConfig(player).checkingLinkStatus.asComponent());
        linkProvider.queryUserId(player.uniqueId()).whenComplete((userId, t) -> {
            if (t != null) {
                sender.sendMessage(discordSRV.messagesConfig(player).unableToLinkAtThisTime.asComponent());
                return;
            }
            if (userId.isPresent()) {
                sender.sendMessage(discordSRV.messagesConfig(player).youAreNowLinked.asComponent());
                return;
            }

            linkProvider.getLinkingInstructions(player, label).whenComplete((comp, t2) -> {
                if (t2 != null) {
                    sender.sendMessage(discordSRV.messagesConfig(player).unableToLinkAtThisTime.asComponent());
                    return;
                }

                sender.sendMessage(ComponentUtil.fromAPI(comp));
            });
        });
    }
}
