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
import com.discordsrv.api.events.message.preprocess.game.AwardMessagePreProcessEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.WordUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class BukkitAchievementListener extends AbstractBukkitListener<PlayerAchievementAwardedEvent> {

    public BukkitAchievementListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ACHIEVEMENT_LISTENER"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAchievementAwarded(PlayerAchievementAwardedEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull PlayerAchievementAwardedEvent event, Void __) {
        String achievement = WordUtils.capitalizeFully(event.getAchievement().name().replace('_', ' '));
        MinecraftComponent achievementName = ComponentUtil.toAPI(Component.text(achievement));

        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        discordSRV.eventBus().publish(
                new AwardMessagePreProcessEvent(
                        event,
                        player,
                        null,
                        achievementName,
                        null,
                        null,
                        null,
                        event.isCancelled()
                )
        );
    }

    private EventObserver<PlayerAchievementAwardedEvent, Boolean> observer;

    @Override
    protected void observeEvents(boolean enable) {
        observer = observeEvent(observer, PlayerAchievementAwardedEvent.class, PlayerAchievementAwardedEvent::isCancelled, enable);
    }

    @Override
    protected void collectRelevantHandlerLists(Consumer<Class<?>> eventClassConsumer) {
        eventClassConsumer.accept(PlayerAchievementAwardedEvent.class);
    }
}
