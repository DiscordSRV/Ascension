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

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.preprocess.game.JoinMessagePreProcessEvent;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.velocity.VelocityDiscordSRV;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;

public class VelocityJoinModule extends AbstractVelocityModule {

    public VelocityJoinModule(VelocityDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "JOIN_MODULE"));
    }

    @Subscribe
    public void onJoin(PlayerChooseInitialServerEvent event) {
        if (!event.getPlayer().isActive()) {
            return;
        }

        boolean cancelled = !event.getInitialServer().isPresent();
        discordSRV.eventBus().publish(
                new JoinMessagePreProcessEvent(
                        event,
                        discordSRV.playerProvider().player(event.getPlayer()),
                        null,
                        null,
                        false, // We can't know if it's the first join on this platform
                        false,
                        false,
                        cancelled
                )
        );
    }
}
