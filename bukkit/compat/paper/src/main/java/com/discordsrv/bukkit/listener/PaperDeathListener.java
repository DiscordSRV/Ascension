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

package com.discordsrv.bukkit.listener;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.preprocess.game.DeathMessagePreProcessEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.core.logging.NamedLogger;
import org.bukkit.GameRule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PaperDeathListener extends AbstractBukkitListener<PlayerDeathEvent> {

    private static final PaperComponentHandle.Get<PlayerDeathEvent> MESSAGE_HANDLE
            = PaperComponentHandle.get(PlayerDeathEvent.class, "deathMessage");

    public PaperDeathListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "DEATH_LISTENER"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull PlayerDeathEvent event, Void __) {
        Boolean gameRuleValue = event.getPlayer().getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
        if (Objects.equals(gameRuleValue, false)) {
            logger().debug("Skipping displaying death message, disabled by gamerule");
            return;
        }

        MinecraftComponent message = MESSAGE_HANDLE.getAPI(event);

        DiscordSRVPlayer player = discordSRV.playerProvider().player(event.getEntity());
        discordSRV.eventBus().publish(
                new DeathMessagePreProcessEvent(
                        event,
                        player,
                        message,
                        null,
                        message == null
                )
        );
    }

    private EventObserver<PlayerDeathEvent, Boolean> observer;

    @Override
    protected void observeEvents(boolean enable) {
        observer = observeEvent(observer, PlayerDeathEvent.class, event -> event.isCancelled() || MESSAGE_HANDLE.getRaw(event) == null, enable);
    }
}
