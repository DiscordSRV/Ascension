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

package com.discordsrv.bukkit.console.executor;

import dev.vankka.dynamicproxy.CallOriginal;
import dev.vankka.dynamicproxy.processor.Original;
import dev.vankka.dynamicproxy.processor.Proxy;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

@Proxy(value = CommandSender.class, className = "BukkitCommandExecutorProxy")
public abstract class BukkitCommandExecutorProxyTemplate implements CommandSender {

    @Original
    private final CommandSender commandSender;
    private final Consumer<Component> componentConsumer;

    private Spigot spigot;

    public BukkitCommandExecutorProxyTemplate(CommandSender commandSender, Consumer<Component> componentConsumer) {
        this.commandSender = commandSender;
        this.componentConsumer = componentConsumer;
        try {
            spigot = new Spigot(commandSender.spigot());
        } catch (Throwable ignored) {}
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
        forwardLegacy(String.join("\n", messages));
        CallOriginal.call(sender, messages);
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        forwardLegacy(String.join("\n", messages));
        CallOriginal.call((Object) messages);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        forwardLegacy(message);
        CallOriginal.call(sender, message);
    }

    @Override
    public void sendMessage(@NotNull String message) {
        forwardLegacy(message);
        CallOriginal.call(message);
    }

    private void forwardLegacy(String legacy) {
        componentConsumer.accept(BukkitComponentSerializer.legacy().deserialize(legacy));
    }

    @Override
    public @NotNull CommandSender.Spigot spigot() {
        return spigot;
    }

    @SuppressWarnings("deprecation")
    public class Spigot extends CommandSender.Spigot {

        private final CommandSender.Spigot spigot;

        Spigot(CommandSender.Spigot spigot) {
            this.spigot = spigot;
        }

        @Override
        public void sendMessage(@Nullable UUID sender, @NotNull BaseComponent component) {
            spigot.sendMessage(sender, component);
            forwardBungee(new BaseComponent[] {component});
        }

        @Override
        public void sendMessage(@NotNull BaseComponent component) {
            spigot.sendMessage(component);
            forwardBungee(new BaseComponent[] {component});
        }

        @Override
        public void sendMessage(@Nullable UUID sender, @NotNull BaseComponent... components) {
            spigot.sendMessage(components);
            forwardBungee(components);
        }

        @Override
        public void sendMessage(@NotNull BaseComponent... components) {
            spigot.sendMessage(components);
            forwardBungee(components);
        }

        private void forwardBungee(BaseComponent[] components) {
            componentConsumer.accept(BungeeComponentSerializer.get().deserialize(components));
        }
    }
}
