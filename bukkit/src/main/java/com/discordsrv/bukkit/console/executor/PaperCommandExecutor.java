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

package com.discordsrv.bukkit.console.executor;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

@SuppressWarnings("JavaLangInvokeHandleSignature") // PaperAPI that is not included at compile accessed via reflection
public class PaperCommandExecutor extends CommandSenderExecutor {

    public static final MethodHandle CREATE_COMMAND_SENDER;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle handle = null;
        try {
            MethodType methodType = MethodType.methodType(CommandSender.class, Consumer.class);
            handle = lookup.findVirtual(Server.class, "createCommandSender", methodType);
        } catch (Throwable ignored) {}
        CREATE_COMMAND_SENDER = handle;
    }

    public PaperCommandExecutor(BukkitDiscordSRV discordSRV, Consumer<Component> componentConsumer) throws Throwable {
        super(discordSRV, (CommandSender) CREATE_COMMAND_SENDER.invoke(componentConsumer));
    }
}
