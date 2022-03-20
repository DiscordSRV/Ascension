/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bungee.command.game.sender;

import com.discordsrv.bungee.BungeeDiscordSRV;
import com.discordsrv.common.command.game.sender.ICommandSender;
import net.kyori.adventure.audience.Audience;
import net.md_5.bungee.api.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class BungeeCommandSender implements ICommandSender {

    protected final BungeeDiscordSRV discordSRV;
    protected final CommandSender commandSender;
    protected final Supplier<Audience> audience;

    public BungeeCommandSender(BungeeDiscordSRV discordSRV, CommandSender commandSender, Supplier<Audience> audience) {
        this.discordSRV = discordSRV;
        this.commandSender = commandSender;
        this.audience = audience;
    }

    @Override
    public boolean hasPermission(String permission) {
        return commandSender.hasPermission(permission);
    }

    @Override
    public void runCommand(String command) {
        discordSRV.proxy().getPluginManager().dispatchCommand(commandSender, command);
    }

    @Override
    public @NotNull Audience audience() {
        return audience.get();
    }
}
