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

package com.discordsrv.common.command.game.commands;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.commands.*;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.command.GameCommandExecutor;
import com.discordsrv.common.command.game.commands.subcommand.BroadcastCommand;
import com.discordsrv.common.command.game.commands.subcommand.reload.ReloadCommand;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.feature.linking.LinkStore;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.ComponentUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordSRVGameCommand implements GameCommandExecutor {

    private static final Map<String, GameCommand> INSTANCES = new ConcurrentHashMap<>();
    private static DiscordSRVGameCommand COMMAND;

    public static GameCommand get(DiscordSRV discordSRV, String alias) {
        if (COMMAND == null) {
            COMMAND = new DiscordSRVGameCommand(discordSRV);
        }
        return INSTANCES.computeIfAbsent(alias, key -> {
            GameCommand command = GameCommand.literal(alias)
                    .requiredPermission(Permissions.COMMAND_ROOT)
                    .executor(COMMAND)
                    .then(BroadcastCommand.discord(discordSRV))
                    .then(BroadcastCommand.minecraft(discordSRV))
                    .then(BroadcastCommand.json(discordSRV))
                    .then(DebugCommand.getGame(discordSRV))
                    .then(LinkOtherCommand.getGame(discordSRV))
                    .then(LinkedCommand.getGame(discordSRV))
                    .then(ReloadCommand.get(discordSRV))
                    .then(ResyncCommand.getGame(discordSRV))
                    .then(VersionCommand.getGame(discordSRV));
            if (discordSRV.linkProvider() instanceof LinkStore) {
                command = command.then(UnlinkCommand.getGame(discordSRV));
            }

            return command;
        });
    }

    private final DiscordSRV discordSRV;

    public DiscordSRVGameCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments, String label) {
        MinecraftComponent component = discordSRV.componentFactory()
                .textBuilder(discordSRV.config().gameCommand.discordFormat)
                .addContext(sender)
                .applyPlaceholderService()
                .build();

        sender.sendMessage(ComponentUtil.fromAPI(component));
    }
}
