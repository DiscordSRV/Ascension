/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.commands.LinkInitCommand;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.commands.DiscordSRVGameCommand;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.config.main.GameCommandConfig;
import com.discordsrv.common.module.type.AbstractModule;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class GameCommandModule extends AbstractModule<DiscordSRV> {

    private final Set<GameCommand> commands = new HashSet<>();

    private final GameCommand primaryCommand;
    private final GameCommand discordAlias;
    private final GameCommand linkCommand;

    public GameCommandModule(DiscordSRV discordSRV) {
        super(discordSRV);
        this.primaryCommand = DiscordSRVGameCommand.get(discordSRV, "discordsrv");
        this.discordAlias = DiscordSRVGameCommand.get(discordSRV, "discord");
        this.linkCommand = LinkInitCommand.getGame(discordSRV);

        registerCommand(primaryCommand);
    }

    @Override
    public void reload(Consumer<DiscordSRVApi.ReloadResult> resultConsumer) {
        GameCommandConfig config = discordSRV.config().gameCommand;
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
