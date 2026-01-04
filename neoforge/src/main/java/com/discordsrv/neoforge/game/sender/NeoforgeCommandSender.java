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

package com.discordsrv.neoforge.game.sender;

import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.neoforge.NeoforgeDiscordSRV;
import net.kyori.adventure.audience.Audience;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

public class NeoforgeCommandSender implements ICommandSender {

    protected final NeoforgeDiscordSRV discordSRV;
    protected final CommandSourceStack commandSource;

    public NeoforgeCommandSender(NeoforgeDiscordSRV discordSRV, CommandSourceStack commandSource) {
        this.discordSRV = discordSRV;
        this.commandSource = commandSource;
    }

    //TODO: Fix permission checking for Neoforge. Maybe using Neoforge's PermissionAPI and register permissions there.
    @Override
    public boolean hasPermission(Permission permission) {
        int defaultLevel = permission.requiresOpByDefault() ? 4 : 0;
        return true;
    }

    @Override
    public void runCommand(String command) {
        discordSRV.getServer().getCommands().performPrefixedCommand(commandSource, command);
    }

    @Override
    public @NotNull Audience audience() {
        return discordSRV.componentFactory().audience(commandSource);
    }
}
