/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.command.game.abstraction.executor;

import dev.vankka.dynamicproxy.processor.Original;
import dev.vankka.dynamicproxy.processor.Proxy;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings({"UnstableApiUsage", "deprecation"})
@Proxy(value = Audience.class, className = "AdventureCommandExecutorProxy")
public abstract class AdventureCommandExecutorProxyTemplate implements Audience {

    @Original
    private final Audience audience;
    private final Consumer<Component> componentConsumer;

    public AdventureCommandExecutorProxyTemplate(Audience audience, Consumer<Component> componentConsumer) {
        this.audience = audience;
        this.componentConsumer = componentConsumer;
    }
    
    private void forwardComponent(ComponentLike component) {
        componentConsumer.accept(component.asComponent());
    }

    @Override
    public void sendMessage(@NotNull Identified source, @NotNull ComponentLike message, @NotNull net.kyori.adventure.audience.MessageType type) {
        audience.sendMessage(source, message, type);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull Identity source, @NotNull ComponentLike message, @NotNull net.kyori.adventure.audience.MessageType type) {
        audience.sendMessage(source, message, type);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull Identified source, @NotNull Component message, @NotNull net.kyori.adventure.audience.MessageType type) {
        audience.sendMessage(source, message, type);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull Identity source, @NotNull Component message, @NotNull net.kyori.adventure.audience.MessageType type) {
        audience.sendMessage(source, message, type);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull Identified source, @NotNull ComponentLike message) {
        audience.sendMessage(source, message);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        audience.sendMessage(message);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull ComponentLike message) {
        audience.sendMessage(message);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull Identity source, @NotNull Component message) {
        audience.sendMessage(source, message);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull Component message, @NotNull net.kyori.adventure.audience.MessageType type) {
        audience.sendMessage(message, type);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull Identified source, @NotNull Component message) {
        audience.sendMessage(source, message);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull Identity source, @NotNull ComponentLike message) {
        audience.sendMessage(source, message);
        forwardComponent(message);
    }

    @Override
    public void sendMessage(@NotNull ComponentLike message, @NotNull net.kyori.adventure.audience.MessageType type) {
        audience.sendMessage(message, type);
        forwardComponent(message);
    }
}
