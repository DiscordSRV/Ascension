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

package com.discordsrv.bukkit.listener.award;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.event.events.message.receive.game.AwardMessageReceiveEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.player.IPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.intellij.lang.annotations.Language;

public class BukkitAwardForwarder implements IBukkitAwardForwarder {

    public static Listener get(BukkitDiscordSRV discordSRV) {
        BukkitAwardForwarder forwarder = new BukkitAwardForwarder(discordSRV);

        if (exists("org.bukkit.event.player.PlayerAdvancementDoneEvent")) {
            // Advancement
            if (exists("io.papermc.paper.advancement.AdvancementDisplay")) {
                // Paper (Since 1.17.1)
                return new PaperModernAdvancementListener(discordSRV, forwarder);
            } else if (exists("org.bukkit.advancement.AdvancementDisplay")) {
                // Spigot (Since 1.19)
                return new SpigotModernAdvancementListener(discordSRV, forwarder);
            } else {
                // Generic
                return new BukkitLegacyAdvancementListener(discordSRV, forwarder);
            }
        } else {
            // Achievement
            return new BukkitAchievementListener(discordSRV, forwarder);
        }
    }

    private static boolean exists(
            @Language(value = "JAVA", prefix = "class X{static{Class.forName(\"", suffix = "\")}}") String className
    ) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
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
