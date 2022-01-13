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

package com.discordsrv.common.discord.connection.jda;

import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.IEventManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class JDAEventManager implements IEventManager {

    private final DiscordSRV discordSRV;

    public JDAEventManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    private void illegalUse() {
        throw new RuntimeException("The JDA event manager may not be used while using DiscordSRV. Please use DiscordSRV's own event bus to listen for JDA events");
    }

    @Override
    public void register(@NotNull Object listener) {
        illegalUse();
    }

    @Override
    public void unregister(@NotNull Object listener) {
        illegalUse();
    }

    @Override
    public void handle(@NotNull GenericEvent event) {
        discordSRV.eventBus().publish(event);
    }

    @NotNull
    @Override
    public List<Object> getRegisteredListeners() {
        return Collections.emptyList();
    }
}
