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

package com.discordsrv.common.command.discord.modal;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.impl.DiscordLabel;
import com.discordsrv.api.discord.entity.interaction.component.impl.DiscordModal;
import com.discordsrv.api.discord.entity.interaction.component.impl.DiscordTextInput;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
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
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.UUID;
import java.util.function.Consumer;

public class LinkModalDiscordCommand implements Consumer<DiscordModalInteractionEvent> {

    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "link-modal");
    private static final ComponentIdentifier CODE_IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "code");

    private static DiscordModal INSTANCE;

    public static DiscordModal getInstance(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            LinkModalDiscordCommand modal = new LinkModalDiscordCommand(discordSRV);

            INSTANCE = DiscordModal.builder(IDENTIFIER, "Minecraft linking")
                    .addComponent(
                            DiscordLabel.of("Linking Code", null, DiscordTextInput.builder(CODE_IDENTIFIER, "Linking Code", DiscordTextInput.Style.SHORT)
                                    // TODO: Implement translations for modals and labels
                                    .setPlaceholder("Enter your linking code here")
                                    .setRequired(true)
                                    .setMaxLength(6)
                                    .setMinLength(6)
                                    .build()
                            )
                    )
                    .setEventHandler(modal)
                    .build();
            discordSRV.discordAPI().registerModal(INSTANCE);
        }
        return INSTANCE;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final Cache<Long, Boolean> linkCheckRateLimit;

    public LinkModalDiscordCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "LINK_INIT_MODAL");
        this.linkCheckRateLimit = discordSRV.caffeineBuilder()
                .expireAfterWrite(LinkStore.LINKING_CODE_RATE_LIMIT)
                .build();
    }

    @Override
    public void accept(DiscordModalInteractionEvent event) {
        DiscordUser user = event.getUser();
        MessagesConfig messagesConfig = discordSRV.messagesConfig(event.getUserLocale());

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
            event.reply(messagesConfig.pleaseWaitBeforeRunningThatCommandAgain.discord().get());
            return;
        }

        ModalMapping codeValue = event.asJDA().getValue(CODE_IDENTIFIER.getDiscordIdentifier());
        String code = codeValue == null ? "" : codeValue.getAsString();
        if (StringUtils.isEmpty(code) || !linkStore.isValidCode(code)) {
            event.reply(messagesConfig.invalidLinkingCode.get(), true);
            return;
        }

        if (linkProvider.getCached(user.getId()).isPresent()) {
            event.reply(messagesConfig.alreadyLinked1st.discord().get(), true);
            return;
        }

        if (linkCheckRateLimit.getIfPresent(user.getId()) != null) {
            event.reply(messagesConfig.pleaseWaitBeforeRunningThatCommandAgain.discord().get(), true);
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
                    interactionHook.editOriginal(messagesConfig.unableToCheckLinkingStatus.discord().get());
                    return;
                }
                if (existingLink.isPresent()) {
                    interactionHook.editOriginal(messagesConfig.alreadyLinked1st.discord().get());
                    return;
                }

                linkStore.getCodeLinking(user.getId(), code)
                        .then(player -> module.link(player.getKey(), user.getId()).thenApply(__ -> player))
                        .whenComplete((player, t3) -> {
                            if (t3 != null) {
                                logger.error("Failed to link", t3);
                                interactionHook.editOriginal(messagesConfig.unableToCheckLinkingStatus.discord().get());
                                return;
                            }

                            linkSuccess(user, interactionHook, linkStore, messagesConfig, player);
                        });
            });
        });
    }

    private void linkSuccess(
            DiscordUser user,
            DiscordInteractionHook interactionHook,
            LinkStore linkStore,
            MessagesConfig messagesConfig,
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
                        messagesConfig.nowLinked1st.discord().format()
                                .addContext(user, player)
                                .addPlaceholder("%player_name%", username)
                                .addPlaceholder("%player_uuid%", playerUUID)
                                .build()
                )
        );
    }
}
