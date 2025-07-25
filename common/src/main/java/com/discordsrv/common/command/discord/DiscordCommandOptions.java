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

package com.discordsrv.common.command.discord;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.helper.DiscordMessage;
import com.discordsrv.common.config.messages.MessagesConfig;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class DiscordCommandOptions {

    private DiscordCommandOptions() {}

    private static Map<Locale, String> translations(DiscordSRV discordSRV, Function<MessagesConfig, DiscordMessage> translationFunction) {
        return discordSRV.getAllTranslations(config -> translationFunction.apply(config).content());
    }

    public static CommandOption.Builder user(DiscordSRV discordSRV) {
        return CommandOption.builder(CommandOption.Type.USER, "user", "")
                .addDescriptionTranslations(translations(discordSRV, config -> config.discordUserCommandArgumentDescription.discord()));
    }

    public static CommandOption.Builder player(DiscordSRV discordSRV, Predicate<DiscordSRVPlayer> playerPredicate) {
        return CommandOption.player(playerPredicate)
                .addDescriptionTranslations(translations(discordSRV, config -> config.playerCommandArgumentDescription.discord()));
    }
}
