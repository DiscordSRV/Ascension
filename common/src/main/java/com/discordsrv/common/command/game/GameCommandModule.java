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

package com.discordsrv.common.command.game;

import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.commands.LinkOtherCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.handler.ICommandHandler;
import com.discordsrv.common.command.game.commands.DiscordSRVGameCommand;
import com.discordsrv.common.config.main.command.GameCommandConfig;
import com.discordsrv.common.core.module.type.AbstractModule;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class GameCommandModule extends AbstractModule<com.discordsrv.common.DiscordSRV> {

    private final Set<GameCommand> commands = new HashSet<>();

    public GameCommandModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public boolean canEnableBeforeReady() {
        // Can enable after JDA starts attempting to connect or if startup fails
        return discordSRV.status() != DiscordSRV.Status.INITIALIZED;
    }

    @Override
    public boolean isEnabled() {
        return discordSRV.isReady() || canEnableBeforeReady();
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        registerCommand(DiscordSRVGameCommand.get(discordSRV, "discordsrv"));

        GameCommandConfig config = discordSRV.config() != null ? discordSRV.config().gameCommand : null;
        if (config == null) {
            return;
        }

        if (config.useDiscordCommand) {
            registerCommand(DiscordSRVGameCommand.get(discordSRV, "discord"));
        }
        if (config.useLinkAlias) {
            registerCommand(LinkOtherCommand.getGame(discordSRV));
        }
    }

    private void registerCommand(GameCommand command) {
        ICommandHandler handler = discordSRV.commandHandler();
        if (handler == null) {
            return;
        }

        if (!commands.add(command)) {
            return;
        }

        handler.registerCommand(command);
    }
}
