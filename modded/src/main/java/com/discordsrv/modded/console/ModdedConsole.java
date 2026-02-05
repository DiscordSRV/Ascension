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

package com.discordsrv.modded.console;

import com.discordsrv.common.command.game.abstraction.executor.CommandExecutorProvider;
import com.discordsrv.common.core.logging.backend.LoggingBackend;
import com.discordsrv.common.core.logging.backend.impl.Log4JLoggerImpl;
import com.discordsrv.common.feature.console.Console;
import com.discordsrv.modded.ModdedDiscordSRV;
import com.discordsrv.modded.command.game.sender.ModdedCommandSender;
import com.discordsrv.modded.console.executor.ModdedCommandExecutor;
import com.discordsrv.modded.console.executor.ModdedCommandFeedbackExecutor;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;

import java.util.function.Consumer;
import java.util.function.Function;

public class ModdedConsole extends ModdedCommandSender implements Console {

    private final LoggingBackend loggingBackend;
    private final CommandExecutorProvider executorProvider;

    public ModdedConsole(ModdedDiscordSRV discordSRV) {
        super(discordSRV, null);
        this.loggingBackend = Log4JLoggerImpl.getRoot();

        Function<Consumer<Component>, CommandSourceStack> commandSenderProvider =
                consumer -> getCommandSource(discordSRV.getServer(), new ModdedCommandFeedbackExecutor(discordSRV.getServer(), consumer), "DiscordSRVFeedback");
        this.executorProvider = consumer -> new ModdedCommandExecutor(discordSRV, commandSenderProvider.apply(consumer));
    }

    @Override
    public LoggingBackend loggingBackend() {
        return loggingBackend;
    }

    @Override
    public CommandExecutorProvider commandExecutorProvider() {
        return executorProvider;
    }
}
