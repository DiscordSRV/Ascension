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

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.unrelocate.net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

@ApiStatus.AvailableSince("Paper 1.18")
public class PaperCommandFeedbackExecutor implements Consumer<Component> {

    private final Consumer<net.kyori.adventure.text.Component> componentConsumer;
    private final CommandSender sender;

    @SuppressWarnings("unchecked")
    public PaperCommandFeedbackExecutor(BukkitDiscordSRV discordSRV, Consumer<net.kyori.adventure.text.Component> componentConsumer) {
        this.componentConsumer = componentConsumer;
        this.sender = discordSRV.server().createCommandSender((Consumer<? super net.kyori.adventure.text.Component>) (Object) this);
    }

    public CommandSender sender() {
        return sender;
    }

    @Override
    public void accept(Component component) {
        componentConsumer.accept(ComponentUtil.fromAPI(MinecraftComponent.fromAdventure(component)));
    }
}