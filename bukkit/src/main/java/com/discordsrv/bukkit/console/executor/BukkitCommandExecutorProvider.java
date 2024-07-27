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

package com.discordsrv.bukkit.console.executor;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.command.game.executor.CommandExecutor;
import com.discordsrv.common.command.game.executor.CommandExecutorProvider;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

import java.util.function.Consumer;

public class BukkitCommandExecutorProvider implements CommandExecutorProvider {

    private static final boolean HAS_PAPER_FORWARDING = hasPaperForwarding();

    @SuppressWarnings("JavaReflectionMemberAccess") // Paper only
    private static boolean hasPaperForwarding() {
        try {
            Server.class.getDeclaredMethod("createCommandSender", Consumer.class);
            return true;
        } catch (Throwable ignored) {}

        return false;
    }

    private final BukkitDiscordSRV discordSRV;

    public BukkitCommandExecutorProvider(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public CommandExecutor getConsoleExecutor(Consumer<Component> componentConsumer) {
        if (HAS_PAPER_FORWARDING) {
            try {
                CommandSender sender = new PaperCommandFeedbackExecutor(
                        discordSRV.server(),
                        apiComponent -> componentConsumer.accept(ComponentUtil.fromAPI(apiComponent))
                ).sender();
                return new CommandSenderExecutor(discordSRV, sender);
            } catch (Throwable ignored) {}
        }

        CommandSender commandSender = new BukkitCommandFeedbackExecutorProxy(discordSRV.server().getConsoleSender(), componentConsumer).getProxy();
        return new CommandSenderExecutor(discordSRV, commandSender);
    }
}
