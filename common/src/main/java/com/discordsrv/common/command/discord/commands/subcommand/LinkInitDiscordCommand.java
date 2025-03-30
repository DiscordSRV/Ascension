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

package com.discordsrv.common.command.discord.commands.subcommand;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.linking.LinkStore;
import com.discordsrv.common.feature.linking.LinkingModule;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.UUID;
import java.util.function.Consumer;

public class LinkInitDiscordCommand implements Consumer<DiscordChatInputInteractionEvent> {

    private static DiscordCommand INSTANCE;

    public static DiscordCommand getInstance(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            LinkInitDiscordCommand command = new LinkInitDiscordCommand(discordSRV);
            ComponentIdentifier identifier = ComponentIdentifier.of("DiscordSRV", "link-init");

            INSTANCE = DiscordCommand.chatInput(identifier, "link", "Link your Minecraft account to your Discord account")
                    .addOption(
                            CommandOption.builder(CommandOption.Type.STRING, "code", "The code provided by the in-game command")
                                    .setRequired(true)
                                    .build()
                    )
                    .setEventHandler(command)
                    .build();
        }
        return INSTANCE;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final Cache<Long, Boolean> linkCheckRateLimit;

    public LinkInitDiscordCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "LINK_INIT_COMMAND");
        this.linkCheckRateLimit = discordSRV.caffeineBuilder()
                .expireAfterWrite(LinkStore.LINKING_CODE_RATE_LIMIT)
                .build();
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        DiscordUser user = event.getUser();
        MessagesConfig.Discord messagesConfig = discordSRV.messagesConfig(event.getUserLocale()).discord;

        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider == null || !linkProvider.usesLocalLinking()) {
            event.reply(SendableDiscordMessage.builder().setContent("Cannot create links using this link provider").build());
            return;
        }
        LinkStore linkStore = linkProvider.store();

        LinkingModule module = discordSRV.getModule(LinkingModule.class);
        if (module == null) {
            event.reply(SendableDiscordMessage.builder().setContent("Unable to link at this time").build());
            return;
        }

        if (module.rateLimit(event.getUser().getId())) {
            event.reply(messagesConfig.pleaseWaitBeforeRunningThatCommandAgain.get());
            return;
        }

        String code = event.getOptionAsString("code");
        if (StringUtils.isEmpty(code) || !linkStore.isValidCode(code)) {
            event.reply(messagesConfig.invalidLinkingCode.get(), true);
            return;
        }

        if (linkProvider.getCached(user.getId()).isPresent()) {
            event.reply(messagesConfig.alreadyLinked1st.get(), true);
            return;
        }

        if (linkCheckRateLimit.getIfPresent(user.getId()) != null) {
            event.reply(messagesConfig.pleaseWaitBeforeRunningThatCommandAgain.get(), true);
            return;
        }
        linkCheckRateLimit.put(user.getId(), true);

        event.deferReply(true).whenComplete((interactionHook, t1) -> {
            if (t1 != null) {
                logger.error("Failed to defer reply", t1);
                return;
            }

            linkProvider.query(user.getId()).whenComplete((existingLink, t2) -> {
                if (t2 != null) {
                    logger.error("Failed to check linking status", t2);
                    interactionHook.editOriginal(messagesConfig.unableToCheckLinkingStatus.get());
                    return;
                }
                if (existingLink.isPresent()) {
                    interactionHook.editOriginal(messagesConfig.alreadyLinked1st.get());
                    return;
                }

                linkStore.getCodeLinking(user.getId(), code)
                        .then(player -> module.link(player.getKey(), user.getId()).thenApply(__ -> player))
                        .whenComplete((player, t3) -> {
                            if (t3 != null) {
                                logger.error("Failed to link", t3);
                                interactionHook.editOriginal(messagesConfig.unableToCheckLinkingStatus.get());
                                return;
                            }

                            linkSuccess(interactionHook, linkStore, messagesConfig, player);
                        });
            });
        });

    }

    private void linkSuccess(
            DiscordInteractionHook interactionHook,
            LinkStore linkStore,
            MessagesConfig.Discord messagesConfig,
            Pair<UUID, String> pair
    ) {
        UUID playerUUID = pair.getKey();
        String username = pair.getValue();

        linkStore.removeLinkingCode(playerUUID).whenComplete((v, t) -> {
            if (t != null) {
                logger.error("Failed to remove linking code from storage", t);
            }
        });

        IPlayer onlinePlayer = discordSRV.playerProvider().player(playerUUID);
        (onlinePlayer != null
            ? Task.completed(onlinePlayer)
            : discordSRV.playerProvider().lookupOfflinePlayer(playerUUID)
        ).whenComplete((player, __) -> interactionHook
                .editOriginal(
                        messagesConfig.accountLinked.format()
                                .addContext(player)
                                .addPlaceholder("%player_name%", username)
                                .addPlaceholder("%player_uuid%", playerUUID)
                                .applyPlaceholderService()
                                .build()
                )
        );
    }
}
