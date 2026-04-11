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

package com.discordsrv.bungee.module;

import com.discordsrv.api.events.message.preprocess.game.ServerSwitchMessagePreProcessEvent;
import com.discordsrv.bungee.BungeeDiscordSRV;
import com.discordsrv.bungee.listener.AbstractBungeeListener;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.event.EventHandler;
import org.jspecify.annotations.NonNull;

public class BungeeServerSwitchModule extends AbstractBungeeListener<ServerSwitchEvent> {

    public BungeeServerSwitchModule(BungeeDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "SERVER_SWITCH_MODULE"));
    }

    @EventHandler()
    public void onServerSwitch(ServerSwitchEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NonNull ServerSwitchEvent event, Void __) {
        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        discordSRV.eventBus().publish(
                new ServerSwitchMessagePreProcessEvent(
                        event,
                        player,
                        null,
                        false
                )
        );
    }
}
