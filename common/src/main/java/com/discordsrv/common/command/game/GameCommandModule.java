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

package com.discordsrv.common.command.game;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.command.DiscordSRVCommand;
import com.discordsrv.common.command.game.command.subcommand.LinkCommand;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.config.main.CommandConfig;
import com.discordsrv.common.module.type.AbstractModule;

import java.util.HashSet;
import java.util.Set;

public class GameCommandModule extends AbstractModule<DiscordSRV> {

    private final Set<GameCommand> commands = new HashSet<>();

    private final GameCommand primaryCommand;
    private final GameCommand discordAlias;
    private final GameCommand linkCommand;

    public GameCommandModule(DiscordSRV discordSRV) {
        super(discordSRV);
        this.primaryCommand = DiscordSRVCommand.get(discordSRV);
        this.discordAlias = GameCommand.literal("discord").redirect(primaryCommand);
        this.linkCommand = LinkCommand.get(discordSRV);

        registerCommand(primaryCommand);
    }

    @Override
    public void reload() {
        CommandConfig config = discordSRV.config().command;
        if (config == null) {
            return;
        }

        registerCommand(primaryCommand);
        if (config.useDiscordCommand) {
            registerCommand(discordAlias);
        }
        if (config.useLinkAlias) {
            registerCommand(linkCommand);
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
