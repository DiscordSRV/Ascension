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

package com.discordsrv.common.command.game.commands.subcommand;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.command.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.permission.game.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Objects;

public class HelpCommand implements GameCommandExecutor {

    private static final String LABEL = "help";
    private static final String COMMAND_ARGUMENT = "command";

    private static GameCommand INSTANCE;

    public static GameCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            HelpCommand cmd = new HelpCommand();
            INSTANCE = GameCommand.literal(LABEL)
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.helpCommandDescription))
                    .requiredPermission(Permissions.COMMAND_HELP)
                    .executor(cmd)
                    .then(GameCommand.stringWord(COMMAND_ARGUMENT).executor(cmd));
        }

        return INSTANCE;
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments, GameCommand command, String rootAlias) {
        command = Objects.requireNonNull(command.getParent()); // Go up one level

        if (arguments.has(COMMAND_ARGUMENT)) {
            command = Objects.requireNonNull(command.getParent()); // If it has the argument we need to go up 2 levels

            String subCommand = arguments.getString(COMMAND_ARGUMENT);
            boolean found = false;
            for (GameCommand child : command.getChildren()) {
                if (child.getLabel().equals(subCommand)) {
                    found = true;
                    command = child;
                    break;
                }
            }
            if (!found) {
                sender.sendMessage(Component.text("Unknown command", NamedTextColor.RED));
                return;
            }
        }

        command.sendCommandInstructions(sender, GameCommandArguments.NONE, rootAlias);
    }
}
