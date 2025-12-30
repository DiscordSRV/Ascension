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

package com.discordsrv.bukkit.gamerule;

import java.util.Arrays;
import java.util.List;

public class GameRule<T> {

    public static final GameRule<Boolean> SHOW_ADVANCEMENT_MESSAGES = new GameRule<>(
            Boolean.class, "announceAdvancements", "ANNOUNCE_ADVANCEMENTS", "SHOW_ADVANCEMENT_MESSAGES");
    public static final GameRule<Boolean> SHOW_DEATH_MESSAGES = new GameRule<>(
            Boolean.class, "showDeathMessages", "SHOW_DEATH_MESSAGES");

    private final Class<T> type;
    private final List<String> options;

    private GameRule(Class<T> type, String... options) {
        this.type = type;
        this.options = Arrays.asList(options);
    }

    public Class<T> getType() {
        return type;
    }

    public List<String> getOptions() {
        return options;
    }
}
