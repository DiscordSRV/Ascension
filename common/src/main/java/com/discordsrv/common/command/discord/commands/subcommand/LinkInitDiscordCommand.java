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
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.discord.modal.LinkModalDiscordCommand;

import java.util.function.Consumer;

public class LinkInitDiscordCommand implements Consumer<DiscordChatInputInteractionEvent> {

    private static final String LABEL = "link";
    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "link-init");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand getInstance(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            LinkInitDiscordCommand command = new LinkInitDiscordCommand(discordSRV);

            INSTANCE = DiscordCommand.chatInput(IDENTIFIER, LABEL, "")
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.linkCommandDescription.discord().content()))
                    .setEventHandler(command)
                    .build();
        }
        return INSTANCE;
    }

    private final DiscordSRV discordSRV;

    public LinkInitDiscordCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        event.replyModal(LinkModalDiscordCommand.getInstance(discordSRV));
    }
}
