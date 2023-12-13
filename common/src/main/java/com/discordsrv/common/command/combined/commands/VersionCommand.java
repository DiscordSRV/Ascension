/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.debug.data.VersionInfo;
import com.discordsrv.common.permission.util.Permission;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class VersionCommand extends CombinedCommand {

    private static VersionCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static VersionCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new VersionCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            VersionCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("version")
                    .requiredPermission(Permission.COMMAND_VERSION)
                    .executor(command);
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            VersionCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "version"), "version", "Get the DiscordSRV version")
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    public VersionCommand(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void execute(CommandExecution execution) {
        VersionInfo versionInfo = discordSRV.versionInfo();

        List<Text> text = new ArrayList<>();
        text.add(
                new Text("Running DiscordSRV ")
                        .withGameColor(TextColor.color(Color.BLURPLE.rgb()))
        );
        text.add(
                new Text("v" + versionInfo.version())
                        .withGameColor(NamedTextColor.GRAY)
                        .withDiscordFormatting(Text.Formatting.BOLD)
        );

        if (versionInfo.isSnapshot()) {
            String rev = StringUtils.substring(versionInfo.gitRevision(), 0, 6);
            text.add(new Text(" (" + rev + "/" + versionInfo.gitBranch() + ")").withGameColor(NamedTextColor.AQUA));
        }

        execution.send(text);
    }
}
