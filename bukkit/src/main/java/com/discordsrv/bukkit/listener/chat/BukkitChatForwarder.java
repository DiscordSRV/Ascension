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

package com.discordsrv.bukkit.listener.chat;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.event.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.common.channel.GlobalChannel;
import com.discordsrv.common.player.IPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

public class BukkitChatForwarder implements IBukkitChatForwarder {

    public static Listener get(BukkitDiscordSRV discordSRV) {
        // TODO: config option
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (1 == 2 && PaperComponentHandle.IS_PAPER_ADVENTURE) {
            return new PaperChatListener(new BukkitChatForwarder(discordSRV));
        }

        return new BukkitChatListener(new BukkitChatForwarder(discordSRV));
    }

    private final BukkitDiscordSRV discordSRV;

    protected BukkitChatForwarder(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void publishEvent(Event event, Player player, MinecraftComponent component, boolean cancelled) {
        IPlayer srvPlayer = discordSRV.playerProvider().player(player);
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessageReceiveEvent(
                        event,
                        srvPlayer,
                        component,
                        new GlobalChannel(discordSRV),
                        cancelled
                )
        ));
    }
}
