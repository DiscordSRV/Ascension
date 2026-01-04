/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.fabric.console.executor;

import com.discordsrv.common.command.game.abstraction.executor.CommandExecutor;
import com.discordsrv.fabric.FabricDiscordSRV;
import net.minecraft.commands.CommandSourceStack;

public class FabricCommandExecutor implements CommandExecutor {

    private final FabricDiscordSRV discordSRV;
    private final CommandSourceStack source;

    public FabricCommandExecutor(FabricDiscordSRV discordSRV, CommandSourceStack source) {
        this.discordSRV = discordSRV;
        this.source = source;
    }

    @Override
    public void runCommand(String command) {
        //? if minecraft: <1.19 {
        /*discordSRV.getServer().getCommandManager().execute(source, command);
        *///?} else {
        discordSRV.getServer().getCommands().performPrefixedCommand(source, command);
         //?}
    }
}
