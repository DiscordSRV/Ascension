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

import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.command.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.permission.game.Permissions;

import java.util.Objects;

public class HelpCommand implements GameCommandExecutor {

    private static GameCommand INSTANCE;

    public static GameCommand get() {
        if (INSTANCE == null) {
            HelpCommand cmd = new HelpCommand();
            INSTANCE = GameCommand.literal("help")
                    .requiredPermission(Permissions.COMMAND_HELP)
                    .executor(cmd);
        }

        return INSTANCE;
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments, GameCommand command) {
        Objects.requireNonNull(command.getParent()).sendCommandInstructions(sender, arguments);
    }
}
