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

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import com.discordsrv.common.config.main.generic.DiscordOutputMode;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class PlayerListConfig {

    @Comment("The complete output of the playerlist command/placeholder when no (visible) players are online on the server")
    public String noPlayersFormat = "No players online";

    @Comment("The order in which players should be sorted, in ascending order\n"
            + "\n"
            + "Suggested placeholders:\n"
            + "%player_name%\n"
            + "More placeholders at %1 (Player)")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    @Untranslated(value = Untranslated.Type.VALUE)
    public String sortBy = "%player_name%";

    @Comment("If players should be grouped")
    public boolean groupPlayers = false;

    @Comment("The criteria by which players should be grouped\n"
            + "All players which have the same value in this option will belong to the same group\n"
            + "\n"
            + "Suggested placeholders:\n"
            + "%player_primary_group%\n"
            + "More placeholders at %1 (Player)")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    @Untranslated(value = Untranslated.Type.VALUE)
    public String groupBy = "%player_primary_group%";

    @Comment("The header before listing all players in a given group\n"
            + "\n"
            + "Suggested placeholders:\n"
            + "%group% - The group (determined by the above option)\n"
            + "More placeholders at %1 (only global placeholders may be used)")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    @Untranslated(value = Untranslated.Type.VALUE)
    public String groupingHeader = "%group%\n";

    @Comment("Separator between groups")
    @Untranslated(value = Untranslated.Type.VALUE)
    public String groupSeparator = "\n\n";

    @Comment("The format for a single player\n"
            + "\n"
            + "Suggested placeholders:\n"
            + "%player_team_display_name% - Displays the player's display name formatted with (vanilla) team options\n"
            + "%player_display_name% - The player's display name\n"
            + "%player_name% - The player's username\n"
            + "%player_prefix% - The player's prefix (LuckPerms meta \"discordsrv_prefix\", otherwise their in-game prefix)\n"
            + "%player_meta_prefix% - The player's prefix from the LuckPerms meta \"discordsrv_prefix\" only\n"
            + "%player_suffix% - The player's suffix (LuckPerms meta \"discordsrv_suffix\", otherwise their in-game suffix)\n"
            + "%player_meta_suffix% - The player's suffix from the LuckPerms meta \"discordsrv_suffix\" only\n"
            + "More placeholders at %1 (Player)")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    @Untranslated(value = Untranslated.Type.VALUE)
    public String playerFormat = "%player_team_display_name%";

    @Comment("Separator between players")
    @Untranslated(value = Untranslated.Type.VALUE)
    public String playerSeparator = ", ";

    @Comment("Options for the /minecraft playerlist command specifically (not shared with the %playerlist% placeholder)")
    public Command command = new Command();

    public static class Command {

        @Comment("The output mode for the message\n"
                + "- ansi: A colored ansi code block\n"
                + "- markdown: Plain text with bold, italics, strikethrough and underlining\n"
                + "- code_block: Plain text code block\n"
                + "- plain: Plain text")
        public DiscordOutputMode outputMode = DiscordOutputMode.ANSI;

        @Comment("The text shown at the top and bottom of the message\n"
                + "\n"
                + "Suggested placeholders:\n"
                + "%playerlist_count% - The amount of players on the server\n"
                + "More placeholders at %1 (only global placeholders may be used)")
        @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
        @Untranslated(value = Untranslated.Type.VALUE)
        public String header = "%playerlist_count% players online";
        public String footer = "";

        @Comment("If the player list output is too long for a single Discord message, labels for buttons to change pages")
        public String previousLabel = "⬅";
        public String nextLabel = "➡";

    }
}
