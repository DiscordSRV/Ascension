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
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.DiscordCommandConfig;
import com.github.benmanes.caffeine.cache.Cache;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.RandomStringUtils;
import org.intellij.lang.annotations.Subst;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private final Cache<String, List<String>> pages;

    public PlayerListCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.pages = discordSRV.caffeineBuilder()
                .expireAfterWrite(Duration.ofMinutes(3))
                .expireAfterAccess(Duration.ofMinutes(1))
                .build();

        discordSRV.eventBus().subscribe(this);
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        DiscordCommandConfig.PlayerListCommand config = discordSRV.config().discordCommand.playerList;

        PlaceholderService placeholderService = discordSRV.placeholderService();
        List<IPlayer> onlinePlayers = new ArrayList<>(discordSRV.playerProvider().allPlayers());
        int originalSize = onlinePlayers.size();
        for (int j = 0; j < 200; j++) {
            for (int i = 0; i < originalSize; i++) {
                onlinePlayers.add(onlinePlayers.get(i));
            }
        }

        Map<String, List<IPlayer>> players = onlinePlayers.stream()
                .filter(player -> !player.isVanished())
                .sorted(Comparator.comparing(player -> placeholderService.replacePlaceholders(config.sortBy, player)))
                .collect(Collectors.groupingBy(player -> placeholderService.replacePlaceholders(config.groupBy, player)));

        List<String> formattedPages = new ArrayList<>();
        StringBuilder currentPage = new StringBuilder(MESSAGE_MAX_LENGTH);
        currentPage.append(placeholderService.replacePlaceholders(config.header));
        String playerSeparator = placeholderService.replacePlaceholders(config.playerSeparator);
        String groupSeparator = placeholderService.replacePlaceholders(config.groupSeparator);

        List<Map.Entry<String, List<IPlayer>>> entries = new ArrayList<>(players.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, List<IPlayer>> group = entries.get(i);
            List<String> formattedPlayers = group.getValue().stream()
                    .map(player -> placeholderService.replacePlaceholders(config.playerFormat, player))
                    .collect(Collectors.toList());

            String footer = i == entries.size() - 1 ? placeholderService.replacePlaceholders(config.footer) : "";

            StringBuilder currentGroup = new StringBuilder();
            if (i != 0) {
                currentGroup.append(groupSeparator);
            }
            currentGroup.append(placeholderService.replacePlaceholders(config.groupingHeader, new SinglePlaceholder("group", group.getKey())));

            boolean first = true;
            for (String formattedPlayer : formattedPlayers) {
                String playerPrefix = first ? "" : playerSeparator;
                first = false;

                if (playerPrefix.length() + currentGroup.length() + formattedPlayer.length() + footer.length() > MESSAGE_MAX_LENGTH) {
                    if (currentPage.length() != 0) {
                        formattedPages.add(currentPage.toString());
                    }
                    currentPage.setLength(0);

                    formattedPages.add(currentGroup.toString());
                    currentGroup.setLength(0);
                    playerPrefix = "";
                }

                currentGroup.append(playerPrefix).append(formattedPlayer);
            }

            currentGroup.append(footer);
            currentPage.append(currentGroup);
        }

        formattedPages.add(currentPage.toString());

        String identifier = RandomStringUtils.randomAlphanumeric(24);
        pages.put(identifier, formattedPages);

        event.reply(constructMessage(identifier, 0, formattedPages))
                .whenFailed(t -> discordSRV.logger().debug("Failed to send playerlist response", t));
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
            // expired
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= pages.size()) {
            // index wrong
            return;
        }

        event.hook().sendMessage(constructMessage(id, selectedIndex, pages));
    }

    private SendableDiscordMessage constructMessage(@Subst("63cc4a8f15f142aaafa26050") String identifier, @Subst("12") int index, List<String> pages) {
        ComponentIdentifier previousIdentifier = ComponentIdentifier.of("DiscordSRV", PREFIX + identifier + "-" + (index - 1));
        ComponentIdentifier nextIdentifier = ComponentIdentifier.of("DiscordSRV", PREFIX + identifier + "-" + (index + 1));

        SendableDiscordMessage.Builder builder = SendableDiscordMessage.builder();
        if (pages.size() != 1) {
            builder.addActionRow(MessageActionRow.of(
                    Button.builder(previousIdentifier, Button.Style.PRIMARY)
                            .setLabel("⬅")
                            .setDisabled(index == 0)
                            .build(),
                    Button.builder(nextIdentifier, Button.Style.PRIMARY)
                            .setLabel("➡")
                            .setDisabled(index == pages.size() - 1)
                            .build()
            ));
        }
        return builder.setContent(pages.get(index)).build();
    }
}
