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

package com.discordsrv.bukkit.requiredlinking;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.config.main.BukkitRequiredLinkingConfig;
import com.discordsrv.common.config.main.linking.RequirementsConfig;
import com.discordsrv.common.linking.requirelinking.ServerRequireLinkingModule;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

// TODO: implement freeze
public class BukkitRequiredLinkingModule extends ServerRequireLinkingModule<BukkitDiscordSRV> implements Listener {

    public BukkitRequiredLinkingModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public RequirementsConfig config() {
        return discordSRV.config().requiredLinking.requirements;
    }

    @Override
    public void enable() {
        super.enable();

        register(PlayerLoginEvent.class, this::handle);
        register(AsyncPlayerPreLoginEvent.class, this::handle);
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void register(Class<T> eventType, BiConsumer<T, EventPriority> eventConsumer) {
        for (EventPriority priority : EventPriority.values()) {
            if (priority == EventPriority.MONITOR) {
                continue;
            }

            discordSRV.server().getPluginManager().registerEvent(
                    eventType,
                    this,
                    priority,
                    (listener, event) -> eventConsumer.accept((T) event, priority),
                    discordSRV.plugin(),
                    true
            );
        }
    }

    @SuppressWarnings("deprecation") // Component is relocated so using it here is inconvenient
    private void handle(AsyncPlayerPreLoginEvent event, EventPriority priority) {
        handle(
                "AsyncPlayerPreLoginEvent",
                priority,
                event.getUniqueId(),
                () -> event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED ? event.getLoginResult().name() : null,
                text -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, text)
        );
    }

    @SuppressWarnings("deprecation") // Component is relocated so using it here is inconvenient
    private void handle(PlayerLoginEvent event, EventPriority priority) {
        handle(
                "PlayerLoginEvent",
                priority,
                event.getPlayer().getUniqueId(),
                () -> event.getResult() != PlayerLoginEvent.Result.ALLOWED ? event.getResult().name() : null,
                text -> event.disallow(PlayerLoginEvent.Result.KICK_OTHER, text)
        );
    }

    private void handle(
            String eventType,
            EventPriority priority,
            UUID playerUUID,
            Supplier<String> alreadyBlocked,
            Consumer<String> disallow
    ) {
        BukkitRequiredLinkingConfig config = discordSRV.config().requiredLinking;
        if (!config.enabled || !config.action.equalsIgnoreCase("KICK")
                || !eventType.equals(config.kick.event) || !priority.name().equals(config.kick.priority)) {
            return;
        }

        String blockType = alreadyBlocked.get();
        if (blockType != null) {
            discordSRV.logger().debug(playerUUID + " is already blocked for " + eventType + "/" + priority + " (" + blockType + ")");
            return;
        }

        Component kickReason = getKickReason(playerUUID).join();
        if (kickReason != null) {
            disallow.accept(BukkitComponentSerializer.legacy().serialize(kickReason));
        }
    }

    @Override
    public void disable() {
        super.disable();

        HandlerList.unregisterAll(this);
    }
}
