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

package com.discordsrv.common.config.main;

import com.discordsrv.common.config.main.generic.DiscordOutputMode;

public class PlayerListConfig {

    public String noPlayersFormat = "No players";

    public String sortBy = "%player_name%";

    public boolean group = false;
    public String groupBy = "%player_primary_group%";
    public String groupingHeader = "%group%\n";
    public String groupSeparator = "\n\n";

    public String playerFormat = "%player_team_display_name%";
    public String playerSeparator = ", ";

    public Command command = new Command();

    public static class Command {

        public DiscordOutputMode outputMode = DiscordOutputMode.ANSI;

        public String header = "";
        public String footer = "";

        public String previousLabel = "⬅";
        public String nextLabel = "➡";

    }
}
