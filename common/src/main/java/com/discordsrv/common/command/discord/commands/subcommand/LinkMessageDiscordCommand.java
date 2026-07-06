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

package com.discordsrv.common.command.discord.commands.subcommand;

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.impl.DiscordButton;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.discord.modal.LinkModalDiscordCommand;

import java.util.List;
import java.util.function.Consumer;

public class LinkMessageDiscordCommand implements Consumer<DiscordChatInputInteractionEvent> {

    private static final String LABEL = "link-message";
    private static final ComponentIdentifier COMMAND_IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "link-message");
    private static final ComponentIdentifier BUTTON_IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "link-button");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand getInstance(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            LinkMessageDiscordCommand command = new LinkMessageDiscordCommand(discordSRV);

            INSTANCE = DiscordCommand.chatInput(COMMAND_IDENTIFIER, LABEL, "")
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.linkMessageCommandDescription.content()))
                    .setEventHandler(command)
                    .build();
        }
        return INSTANCE;
    }

    private final DiscordSRV discordSRV;

    public LinkMessageDiscordCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;

        discordSRV.eventBus().subscribe(this);
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        event.getChannel().sendMessage(
                discordSRV.messagesConfig().linkMessage.builder()
                        .addActionRow(
                                DiscordButton.builder(BUTTON_IDENTIFIER, DiscordButton.Style.PRIMARY)
                                        .setLabel(discordSRV.messagesConfig().linkMessageButtonLabel)
                                        .build()
                        ).build()
        ).whenSuccessful(message -> event.reply(discordSRV.messagesConfig().linkMessageSuccess.get(), true));
    }

    @Subscribe
    public void onDiscordButtonInteraction(DiscordButtonInteractionEvent event) {
        ComponentIdentifier componentIdentifier = event.getIdentifier();
        if (!componentIdentifier.equals(BUTTON_IDENTIFIER)) {
            return;
        }

        event.replyModal(LinkModalDiscordCommand.getInstance(discordSRV));
    }
}
