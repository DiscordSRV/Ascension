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

package com.discordsrv.bungee.console.executor;

import dev.vankka.dynamicproxy.CallOriginal;
import dev.vankka.dynamicproxy.processor.Original;
import dev.vankka.dynamicproxy.processor.Proxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.function.Consumer;

@Proxy(value = CommandSender.class, className = "BungeeCommandExecutorProxy")
public abstract class BungeeCommandExecutorProxyTemplate implements CommandSender {

    @Original
    private final CommandSender commandSender;
    private final Consumer<Component> componentConsumer;

    public BungeeCommandExecutorProxyTemplate(CommandSender commandSender, Consumer<Component> componentConsumer) {
        this.commandSender = commandSender;
        this.componentConsumer = componentConsumer;
    }

    private void forwardComponent(Component component) {
        this.componentConsumer.accept(component);
    }

    @Override
    public void sendMessage(BaseComponent... message) {
        CallOriginal.call((Object) message);
        forwardComponent(BungeeComponentSerializer.get().deserialize(message));
    }

    @Override
    public void sendMessage(BaseComponent message) {
        CallOriginal.call(message);
        forwardComponent(BungeeComponentSerializer.get().deserialize(new BaseComponent[]{message}));
    }

    @Override
    public void sendMessage(String message) {
        CallOriginal.call(message);
        forwardComponent(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    @Override
    public void sendMessages(String... messages) {
        CallOriginal.call((Object) messages);
        forwardComponent(LegacyComponentSerializer.legacySection().deserialize(String.join("\n", messages)));
    }
}
