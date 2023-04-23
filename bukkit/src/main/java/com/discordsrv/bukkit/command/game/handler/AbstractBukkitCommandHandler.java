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

package com.discordsrv.bukkit.command.game.handler;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.command.game.sender.BukkitCommandSender;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.Locale;

public abstract class AbstractBukkitCommandHandler implements ICommandHandler {

    private static final MethodHandle COMMAND_MAP_HANDLE;

    static {
        MethodHandle handle = null;
        try {
            handle = MethodHandles.lookup().findVirtual(
                    Server.class,
                    "getCommandMap",
                    MethodType.methodType(CommandMap.class)
            );
        } catch (ReflectiveOperationException ignored) {}
        COMMAND_MAP_HANDLE = handle;
    }

    public static AbstractBukkitCommandHandler get(BukkitDiscordSRV discordSRV) {
        try {
            Class.forName("me.lucko.commodore.Commodore");
            return new CommodoreHandler(discordSRV);
        } catch (Throwable e) {
            BukkitBasicCommandHandler handler = new BukkitBasicCommandHandler(discordSRV);
            if (e instanceof ClassNotFoundException) {
                handler.logger.debug("Brigadier classes not present, not using commodore");
            } else {
                handler.logger.debug("Failed to initialize Commodore", e);
            }
            return handler;
        }
    }

    protected final BukkitDiscordSRV discordSRV;
    protected final Logger logger;

    public AbstractBukkitCommandHandler(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "COMMAND_HANDLER");
    }

    protected ICommandSender sender(CommandSender commandSender) {
        if (commandSender instanceof Player) {
            return discordSRV.playerProvider().player((Player) commandSender);
        } else if (commandSender instanceof ConsoleCommandSender) {
            return discordSRV.console();
        } else {
            return new BukkitCommandSender(discordSRV, commandSender, () -> discordSRV.audiences().sender(commandSender));
        }
    }

    protected PluginCommand command(GameCommand gameCommand) {
        String label = gameCommand.getLabel();
        PluginCommand pluginCommand = discordSRV.plugin().getCommand(label);
        if (pluginCommand != null) {
            return pluginCommand;
        }

        if (COMMAND_MAP_HANDLE == null) {
            // CommandMap unusable, can't get the command from it
            return null;
        }

        PluginCommand command = null;
        try {
            Constructor<?> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            command = (PluginCommand) constructor.newInstance(label, discordSRV.plugin());

            CommandMap commandMap = (CommandMap) COMMAND_MAP_HANDLE.invokeExact(discordSRV.server());
            commandMap.register(label, discordSRV.plugin().getName().toLowerCase(Locale.ROOT), command);
        } catch (Throwable ignored) {}

        return command;
    }
}
