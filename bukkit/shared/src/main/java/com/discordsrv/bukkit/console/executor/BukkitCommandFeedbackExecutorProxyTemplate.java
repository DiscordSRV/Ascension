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

package com.discordsrv.bukkit.console.executor;

import dev.vankka.dynamicproxy.processor.Original;
import dev.vankka.dynamicproxy.processor.Proxy;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.function.Consumer;

@Proxy(value = CommandSender.class, className = "BukkitCommandFeedbackExecutorProxy")
public abstract class BukkitCommandFeedbackExecutorProxyTemplate implements CommandSender {

    @Original
    private final CommandSender commandSender;
    private final Consumer<Component> componentConsumer;

    public BukkitCommandFeedbackExecutorProxyTemplate(CommandSender commandSender, Consumer<Component> componentConsumer) {
        this.commandSender = commandSender;
        this.componentConsumer = componentConsumer;
    }

    private void forwardLegacy(String legacy) {
        componentConsumer.accept(BukkitComponentSerializer.legacy().deserialize(legacy));
    }

    @Override
    public void sendMessage(String message) {
        commandSender.sendMessage(message);
        forwardLegacy(message);
    }

    @Override
    public void sendMessage(String[] messages) {
        commandSender.sendMessage(messages);
        for (String message : messages) {
            forwardLegacy(message);
        }
    }
}
