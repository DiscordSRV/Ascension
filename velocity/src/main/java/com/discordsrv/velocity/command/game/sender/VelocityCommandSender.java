/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.velocity.command.game.sender;

import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.velocity.VelocityDiscordSRV;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

public class VelocityCommandSender implements ICommandSender {

    protected final VelocityDiscordSRV discordSRV;
    protected final CommandSource commandSource;

    public VelocityCommandSender(VelocityDiscordSRV discordSRV, CommandSource commandSource) {
        this.discordSRV = discordSRV;
        this.commandSource = commandSource;
    }

    @Override
    public boolean hasPermission(String permission) {
        return commandSource.hasPermission(permission);
    }

    @Override
    public void runCommand(String command) {
        discordSRV.proxy().getCommandManager().executeAsync(commandSource, command);
    }

    @Override
    public @NotNull Audience audience() {
        return commandSource;
    }
}
