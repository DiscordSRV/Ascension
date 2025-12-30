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
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.bukkit.player.BukkitPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import io.papermc.paper.advancement.AdvancementDisplay;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

@ApiStatus.AvailableSince("Paper 1.17.1")
public class PaperAdvancementListener extends AbstractBukkitListener<PlayerAdvancementDoneEvent> {

    private static final PaperComponentHandle.Get<PlayerAdvancementDoneEvent> MESSAGE_HANDLE
            = PaperComponentHandle.get(PlayerAdvancementDoneEvent.class, "message");
    private static final PaperComponentHandle.Get<AdvancementDisplay> ADVANCEMENT_TITLE_HANDLE
            = PaperComponentHandle.get(AdvancementDisplay.class, "title");
    private static final PaperComponentHandle.Get<AdvancementDisplay> ADVANCEMENT_DESCRIPTION_HANDLE
            = PaperComponentHandle.get(AdvancementDisplay.class, "description");

    public PaperAdvancementListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ADVANCEMENT_LISTENER"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull PlayerAdvancementDoneEvent event, Void __) {
        BukkitPlayer player = discordSRV.playerProvider().player(event.getPlayer());

        Boolean gameRuleValue = player.getGameRuleValueForCurrentWorld(com.discordsrv.bukkit.gamerule.GameRule.SHOW_ADVANCEMENT_MESSAGES);
        if (Objects.equals(gameRuleValue, false)) {
            logger().trace("Skipping displaying advancement, disabled by gamerule");
            return;
        }

        Advancement advancement = event.getAdvancement();
        AdvancementDisplay display = advancement.getDisplay();
        if (display == null || !display.doesAnnounceToChat()) {
            logger().trace("Skipping advancement display of \"" + advancement.getKey().getKey() + "\" for "
                                 + event.getPlayer() + ": advancement display == null or does not broadcast to chat");
            return;
        }

        MinecraftComponent message = MESSAGE_HANDLE.getAPI(event);
        MinecraftComponent title = ADVANCEMENT_TITLE_HANDLE.getAPI(display);
        MinecraftComponent description = ADVANCEMENT_DESCRIPTION_HANDLE.getAPI(display);

        discordSRV.eventBus().publish(
                new AwardMessagePreProcessEvent(
                        event,
                        player,
                        message,
                        title,
                        description,
                        AwardMessagePreProcessEvent.AdvancementFrame.fromId(display.frame().toString()),
                        null,
                        message == null
                )
        );
    }

    private EventObserver<PlayerAdvancementDoneEvent, Boolean> observer;

    @Override
    protected void observeEvents(boolean enable) {
        observer = observeEvent(observer, PlayerAdvancementDoneEvent.class, event -> MESSAGE_HANDLE.getRaw(event) == null, enable);
    }

    @Override
    protected void collectRelevantHandlerLists(Consumer<Class<?>> eventClassConsumer) {
        eventClassConsumer.accept(PlayerAdvancementDoneEvent.class);
    }
}
