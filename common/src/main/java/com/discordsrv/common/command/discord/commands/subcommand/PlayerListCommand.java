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

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.actionrow.MessageActionRow;
import com.discordsrv.api.discord.entity.interaction.component.impl.Button;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.PlayerListConfig;
import com.discordsrv.common.config.main.generic.DiscordOutputMode;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.PlayerListModule;
import com.github.benmanes.caffeine.cache.Cache;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.RandomStringUtils;
import org.intellij.lang.annotations.Subst;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class PlayerListCommand implements Consumer<DiscordChatInputInteractionEvent> {

    private static final int MESSAGE_MAX_LENGTH = Message.MAX_CONTENT_LENGTH;
    private static final String PREFIX = "playerlist-";

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            PlayerListCommand command = new PlayerListCommand(discordSRV);
            INSTANCE = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "playerlist"), "playerlist", "Show the players online on the server")
                    .setEventHandler(command)
                    .build();
        }

        return INSTANCE;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final Cache<String, List<String>> pages;

    public PlayerListCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "PLAYER_LIST");
        this.pages = discordSRV.caffeineBuilder()
                .expireAfterWrite(Duration.ofMinutes(3))
                .expireAfterAccess(Duration.ofMinutes(1))
                .build();

        discordSRV.eventBus().subscribe(this);
    }

    private PlayerListModule module() {
        return discordSRV.getModule(PlayerListModule.class);
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        event.reply(generatePagination(0), true)
                .whenFailed(t -> logger.debug("Failed to send create pagination", t));
    }

    private SendableDiscordMessage generatePagination(int preferredIndex) {
        DiscordOutputMode outputMode = discordSRV.config().playerList.command.outputMode;
        List<String> formattedPages = PlainPlaceholderFormat.supplyWith(
                outputMode.plainFormat(),
                () -> module().generatePlayerList(outputMode, false, MESSAGE_MAX_LENGTH, true)
        );
        String identifier = RandomStringUtils.randomAlphanumeric(24);

        pages.put(identifier, formattedPages);
        return constructMessage(identifier, Math.min(preferredIndex, formattedPages.size() - 1), formattedPages);
    }

    @Subscribe
    public void onDiscordButtonInteraction(DiscordButtonInteractionEvent event) {
        ComponentIdentifier componentIdentifier = event.getIdentifier();
        String identifier = componentIdentifier.getIdentifier();
        if (!componentIdentifier.getExtensionName().equals("DiscordSRV") || !identifier.startsWith(PREFIX)) {
            return;
        }

        String[] parts = identifier.split("-");
        int selectedIndex;
        try {
            selectedIndex = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {
            return;
        }

        String id = parts[1];
        List<String> pages = this.pages.getIfPresent(id);
        if (pages == null) {
            // expired, re-generate
            event.editMessage(generatePagination(selectedIndex))
                    .whenFailed(t -> logger.error("Failed to re-generate pagination", t));
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= pages.size()) {
            // index wrong
            return;
        }

        event.editMessage(constructMessage(id, selectedIndex, pages))
                .whenFailed(t -> logger.debug("Failed to handle pagination", t));
    }

    private SendableDiscordMessage constructMessage(
            @Subst("63cc4a8f15f142aaafa26050") String identifier,
            @Subst("12") int index,
            List<String> pages
    ) {
        PlayerListConfig config = discordSRV.config().playerList;
        ComponentIdentifier previousIdentifier = ComponentIdentifier.of("DiscordSRV", PREFIX + identifier + "-" + (index - 1));
        ComponentIdentifier nextIdentifier = ComponentIdentifier.of("DiscordSRV", PREFIX + identifier + "-" + (index + 1));

        SendableDiscordMessage.Builder builder = SendableDiscordMessage.builder();
        if (pages.size() != 1) {
            builder.addActionRow(MessageActionRow.of(
                    Button.builder(previousIdentifier, Button.Style.PRIMARY)
                            .setLabel(config.command.previousLabel)
                            .setDisabled(index == 0)
                            .build(),
                    Button.builder(nextIdentifier, Button.Style.PRIMARY)
                            .setLabel(config.command.nextLabel)
                            .setDisabled(index == pages.size() - 1)
                            .build()
            ));
        }
        return builder.setContent(pages.get(index)).build();
    }
}
