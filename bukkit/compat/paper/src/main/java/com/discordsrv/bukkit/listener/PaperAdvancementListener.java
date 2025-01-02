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
import com.discordsrv.api.events.message.receive.game.AwardMessageReceiveEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import io.papermc.paper.advancement.AdvancementDisplay;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.AvailableSince("Paper 1.17.1")
public class PaperAdvancementListener extends AbstractBukkitListener<PlayerAdvancementDoneEvent> {

    private static final PaperComponentHandle.Get<PlayerAdvancementDoneEvent> MESSAGE_HANDLE
            = PaperComponentHandle.get(PlayerAdvancementDoneEvent.class, "message");
    private static final PaperComponentHandle.Get<Advancement> ADVANCEMENT_DISPLAY_NAME_HANDLE
            = PaperComponentHandle.get(Advancement.class, "displayName");

    public PaperAdvancementListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ADVANCEMENT_LISTENER"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        receiveEvent(event);
    }

    @Override
    protected void handleEvent(PlayerAdvancementDoneEvent event) {
        Advancement advancement = event.getAdvancement();
        AdvancementDisplay display = advancement.getDisplay();
        if (display == null || !display.doesAnnounceToChat()) {
            logger().trace("Skipping advancement display of \"" + advancement.getKey().getKey() + "\" for "
                                 + event.getPlayer() + ": advancement display == null or does not broadcast to chat");
            return;
        }

        MinecraftComponent message = MESSAGE_HANDLE.getAPI(event);
        MinecraftComponent displayName = ADVANCEMENT_DISPLAY_NAME_HANDLE.getAPI(advancement);

        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new AwardMessageReceiveEvent(
                        event,
                        player,
                        message,
                        displayName,
                        null,
                        message == null
                )
        ));
    }

    private EventObserver<PlayerAdvancementDoneEvent, Boolean> observer;

    @Override
    protected void observeEvents(boolean enable) {
        observer = observeEvent(observer, PlayerAdvancementDoneEvent.class, event -> event.message() == null, enable);
    }
}
