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

package com.discordsrv.modded.command.game.sender;

import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.modded.ModdedDiscordSRV;
import net.kyori.adventure.audience.Audience;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

public class ModdedCommandSender implements ICommandSender {

    protected final ModdedDiscordSRV discordSRV;
    protected final CommandSourceStack commandSource;

    public ModdedCommandSender(ModdedDiscordSRV discordSRV, CommandSourceStack commandSource) {
        this.discordSRV = discordSRV;
        this.commandSource = commandSource;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        int defaultLevel = permission.requiresOpByDefault() ? 4 : 0;

        //? if fabric
        return me.lucko.fabric.api.permissions.v0.Permissions.check(commandSource, permission.permission(), defaultLevel);
        //? if neoforge
        //return commandSource.permissions().hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.byId(defaultLevel)));
    }

    @Override
    public void runCommand(String command) {
        //? if minecraft: <1.19 {
        /*discordSRV.getServer().getCommandManager().execute(commandSource, command);
        *///?} else {
        discordSRV.getServer().getCommands().performPrefixedCommand(commandSource, command);
        //?}
    }

    @Override
    public @NotNull Audience audience() {
        return discordSRV.componentFactory().audience(commandSource);
    }
}
