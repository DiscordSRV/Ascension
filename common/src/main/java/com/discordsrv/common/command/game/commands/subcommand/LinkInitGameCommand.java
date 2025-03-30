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

package com.discordsrv.common.command.game.commands.subcommand;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.command.game.abstraction.command.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.command.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.linking.LinkStore;
import com.discordsrv.common.util.ComponentUtil;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.UUID;

public class LinkInitGameCommand implements GameCommandExecutor {

    private static GameCommandExecutor INSTANCE;

    public static GameCommandExecutor getExecutor(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            INSTANCE = new LinkInitGameCommand(discordSRV);
        }

        return INSTANCE;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final Cache<UUID, Boolean> linkCheckRateLimit;

    private LinkInitGameCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "LINK_INIT_COMMAND");
        this.linkCheckRateLimit = discordSRV.caffeineBuilder()
                .expireAfterWrite(LinkStore.LINKING_CODE_RATE_LIMIT)
                .build();
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments, String label) {
        if (!(sender instanceof IPlayer)) {
            sender.sendMessage(discordSRV.messagesConfig(sender).pleaseSpecifyPlayerAndUserToLink.asComponent());
            return;
        }

        IPlayer player = (IPlayer) sender;
        MessagesConfig.Minecraft messages = discordSRV.messagesConfig(player);

        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider == null) {
            player.sendMessage(messages.unableToLinkAtThisTime.asComponent());
            return;
        }

        if (linkProvider.getCached(player.uniqueId()).isPresent()) {
            // Check cache first
            player.sendMessage(messages.alreadyLinked1st.asComponent());
            return;
        }

        if (linkCheckRateLimit.getIfPresent(player.uniqueId()) != null) {
            player.sendMessage(messages.pleaseWaitBeforeRunningThatCommandAgain.asComponent());
            return;
        }
        linkCheckRateLimit.put(player.uniqueId(), true);

        player.sendMessage(discordSRV.messagesConfig(player).checkingLinkStatus.asComponent());
        linkProvider.query(player.uniqueId(), true).whenComplete((userId, t1) -> {
            if (t1 != null) {
                logger.error("Failed to check linking status", t1);
                player.sendMessage(messages.unableToLinkAtThisTime.asComponent());
                return;
            }
            if (userId.isPresent()) {
                player.sendMessage(messages.nowLinked1st.asComponent());
                return;
            }

            linkProvider.getLinkingInstructions(player, label).whenComplete((comp, t2) -> {
                if (t2 != null) {
                    logger.error("Failed to link account", t2);
                    player.sendMessage(messages.unableToLinkAtThisTime.asComponent());
                    return;
                }

                player.sendMessage(ComponentUtil.fromAPI(comp));
            });
        });

    }
}
