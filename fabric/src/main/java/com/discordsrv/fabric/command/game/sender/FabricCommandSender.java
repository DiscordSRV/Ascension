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

package com.discordsrv.fabric.command.game.sender;

import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

public class FabricCommandSender implements ICommandSender {

    protected final FabricDiscordSRV discordSRV;
    protected final ServerCommandSource commandSource;

    public FabricCommandSender(FabricDiscordSRV discordSRV, ServerCommandSource commandSource) {
        this.discordSRV = discordSRV;
        this.commandSource = commandSource;
    }

    @Override
    public boolean hasPermission(String permission) {
        return Permissions.check(commandSource, permission);
    }

    @Override
    public void runCommand(String command) {
        discordSRV.getServer().getCommandManager().executeWithPrefix(commandSource, command);
    }

    @Override
    public @NotNull Audience audience() {
        return MinecraftServerAudiences.of(discordSRV.getServer()).audience(commandSource);
    }
}
