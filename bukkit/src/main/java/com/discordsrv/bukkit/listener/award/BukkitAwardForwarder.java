/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bukkit.listener.award;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.event.events.message.receive.game.AwardMessageReceiveEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.player.IPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class BukkitAwardForwarder implements IBukkitAwardForwarder {

    public static Listener get(BukkitDiscordSRV discordSRV) {
        try {
            Class.forName("org.bukkit.event.player.PlayerAdvancementDoneEvent");
            try {
                Class.forName("io.papermc.paper.advancement.AdvancementDisplay");

                return new PaperModernAdvancementListener(discordSRV, new BukkitAwardForwarder(discordSRV));
            } catch (ClassNotFoundException ignored) {
                return new BukkitAdvancementListener(discordSRV, new BukkitAwardForwarder(discordSRV));
            }
        } catch (ClassNotFoundException ignored) {
            return new BukkitAchievementListener(discordSRV, new BukkitAwardForwarder(discordSRV));
        }
    }

    private final BukkitDiscordSRV discordSRV;

    protected BukkitAwardForwarder(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public void publishEvent(Object triggeringEvent, Player player, MinecraftComponent advancementName, MinecraftComponent message, boolean cancelled) {
        IPlayer srvPlayer = discordSRV.playerProvider().player(player);
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new AwardMessageReceiveEvent(
                        triggeringEvent,
                        srvPlayer,
                        advancementName,
                        message,
                        null,
                        cancelled
                )
        ));
    }
}
