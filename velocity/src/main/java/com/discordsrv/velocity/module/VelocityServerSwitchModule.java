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

package com.discordsrv.velocity.module;

import com.discordsrv.api.events.message.preprocess.game.ServerSwitchMessagePreProcessEvent;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.velocity.VelocityDiscordSRV;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

public class VelocityServerSwitchModule extends AbstractVelocityModule {

    public VelocityServerSwitchModule(VelocityDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "SERVER_SWITCH_MODULE"));
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        if (!event.getPreviousServer().isPresent()) return; // We don't know the previous server. Might be the initial login.

        discordSRV.eventBus().publish(
                new ServerSwitchMessagePreProcessEvent(
                        event,
                        discordSRV.playerProvider().player(event.getPlayer()),
                        null,
                        event.getPreviousServer().get().getServerInfo().getName(),
                        event.getServer().getServerInfo().getName(),
                        false
                )
        );
    }
}
