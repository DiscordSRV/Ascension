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

package com.discordsrv.bukkit.command.game.handler;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.handler.ICommandHandler;
import com.discordsrv.common.command.game.abstraction.handler.util.BrigadierUtil;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;
import org.bukkit.command.Command;

public class CommodoreHandler extends BukkitBasicCommandHandler implements ICommandHandler {

    private final Commodore commodore;

    public CommodoreHandler(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
        this.commodore = CommodoreProvider.getCommodore(discordSRV.plugin());
    }

    @Override
    protected void registerPluginCommand(Command command, GameCommand gameCommand) {
        super.registerPluginCommand(command, gameCommand);

        LiteralCommandNode<?> commandNode = BrigadierUtil.convertToBrigadier(discordSRV, gameCommand, null);
        commodore.register(command, commandNode, sender -> gameCommand.hasPermission(discordSRV.playerProvider().player(sender)));
        logger.debug(command.getName() + " registered to Commodore");
    }
}
