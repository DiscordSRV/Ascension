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

package com.discordsrv.bungee.console.executor;

import com.discordsrv.bungee.BungeeDiscordSRV;
import com.discordsrv.common.command.game.abstraction.executor.CommandExecutor;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;

import java.util.function.Consumer;

public class BungeeCommandExecutor implements CommandExecutor {

    private final BungeeDiscordSRV discordSRV;
    private final CommandSender commandSender;

    public BungeeCommandExecutor(BungeeDiscordSRV discordSRV, Consumer<Component> componentConsumer) {
        this.discordSRV = discordSRV;
        this.commandSender = new BungeeCommandExecutorProxy(
                discordSRV.proxy().getConsole(),
                componentConsumer
        ).getProxy();
    }

    @Override
    public void runCommand(String command) {
        discordSRV.proxy().getPluginManager().dispatchCommand(commandSender, command);
    }
}
