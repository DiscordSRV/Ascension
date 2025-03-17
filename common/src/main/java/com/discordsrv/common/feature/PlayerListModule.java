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

package com.discordsrv.common.feature;

import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.PlayerListConfig;
import com.discordsrv.common.config.main.generic.DiscordOutputMode;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import org.apache.commons.collections4.map.LinkedMap;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerListModule extends AbstractModule<DiscordSRV> {

    public PlayerListModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "PLAYER_LIST"));

        discordSRV.placeholderService().addGlobalContext(this);
    }

    @Placeholder("playerlist")
    public String getPlayerList(@PlaceholderRemainder(supportsNoValue = true) String maxLengthArgument) {
        int maxLength = Integer.MAX_VALUE;
        try {
            maxLength = Integer.parseInt(maxLengthArgument);
        } catch (NumberFormatException ignored) {}
        return generatePlayerList(DiscordOutputMode.OFF, true, maxLength, false).get(0);
    }

    public List<String> generatePlayerList(DiscordOutputMode outputMode, boolean splitGroups, int maxLength, boolean command) {
        PlayerListConfig config = discordSRV.config().playerList;

        PlaceholderService placeholderService = discordSRV.placeholderService();
        List<IPlayer> onlinePlayers = new ArrayList<>(discordSRV.playerProvider().allPlayers());

        Map<String, List<IPlayer>> players = onlinePlayers.stream()
                .filter(player -> !player.isVanished())
                .sorted(Comparator.comparing(player -> placeholderService.replacePlaceholders(config.sortBy, player)))
                .collect(Collectors.groupingBy(
                        player -> !config.group ? "" : placeholderService.replacePlaceholders(config.groupBy, player),
                        LinkedMap::new,
                        Collectors.toList())
                );

        if (players.isEmpty()) {
            return Collections.singletonList(placeholderService.replacePlaceholders(config.noPlayersFormat));
        }

        List<String> formattedPages = new ArrayList<>();
        StringBuilder currentPage = new StringBuilder(Math.min(maxLength, 2_000));
        String playerSeparator = placeholderService.replacePlaceholders(config.playerSeparator);
        String groupSeparator = placeholderService.replacePlaceholders(config.groupSeparator);

        List<Map.Entry<String, List<IPlayer>>> entries = new ArrayList<>(players.entrySet());
        String header = "";
        String footer = "";
        for (int groupIndex = 0; groupIndex < entries.size(); groupIndex++) {
            Map.Entry<String, List<IPlayer>> group = entries.get(groupIndex);
            List<String> formattedPlayers = group.getValue().stream()
                    .map(player -> placeholderService.replacePlaceholders(config.playerFormat, player))
                    .collect(Collectors.toList());

            header = (command && groupIndex == 0 ? placeholderService.replacePlaceholders(config.command.header) : "");
            footer = (command && groupIndex == entries.size() - 1 ? placeholderService.replacePlaceholders(config.command.footer) : "");

            StringBuilder currentGroup = new StringBuilder();
            if (config.group) {
                if (groupIndex != 0) {
                    currentGroup.append(groupSeparator);
                }
                currentGroup.append(placeholderService.replacePlaceholders(config.groupingHeader, new SinglePlaceholder("group", group.getKey())));
            }

            boolean first = true;
            for (String formattedPlayer : formattedPlayers) {
                String valuePrefix = first ? "" : playerSeparator;
                first = false;

                int length = currentPage.length() + currentGroup.length()
                        + valuePrefix.length() + formattedPlayer.length()
                        + header.length() + footer.length()
                        + outputMode.blockLength();
                if (length > maxLength) {
                    int adjustAmount = 0;
                    if (!splitGroups && currentPage.length() != 0) {
                        formattedPages.add(header + outputMode.prefix() + currentPage + outputMode.suffix());
                        adjustAmount = currentPage.length();
                        currentPage.setLength(0);
                    }

                    if (length - adjustAmount > maxLength) {
                        currentPage.append(currentGroup);
                        currentGroup.setLength(0);

                        formattedPages.add(header + outputMode.prefix() + currentPage + outputMode.suffix());
                        header = "";

                        currentPage.setLength(0);
                        valuePrefix = "";
                    }
                }

                currentGroup.append(valuePrefix).append(formattedPlayer);
            }

            currentPage.append(currentGroup);
        }

        formattedPages.add(header + outputMode.prefix() + currentPage + outputMode.suffix() + footer);
        return formattedPages;
    }
}
