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

package com.discordsrv.bukkit.requiredlinking.listener;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.config.main.BukkitRequiredLinkingConfig;
import com.discordsrv.bukkit.requiredlinking.BukkitRequiredLinkingModule;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class BukkitRequiredLinkingListener<E extends Event> implements Listener {

    private final BukkitRequiredLinkingModule module;
    private final String eventType;

    @SuppressWarnings("unchecked")
    public BukkitRequiredLinkingListener(BukkitRequiredLinkingModule module, Class<E> eventType, EventPriority priority) {
        this.module = module;
        this.eventType = eventType.getSimpleName();

        discordSRV().server().getPluginManager().registerEvent(
                eventType,
                this,
                priority,
                (listener, event) -> handleEvent(priority, (E) event),
                discordSRV().plugin(),
                true
        );
    }

    public void close() {
        HandlerList.unregisterAll(this);
    }

    protected BukkitDiscordSRV discordSRV() {
        return module.discordSRV();
    }

    @Nullable
    public abstract String getAlreadyBlockedReason(E event);
    @NotNull
    public abstract Pair<UUID, String> getPlayer(E event);
    public abstract void blockLogin(E event, Component reason);

    private void handleEvent(EventPriority priority, E event) {
        BukkitRequiredLinkingConfig config = module.config();
        if (config == null) {
            blockLogin(event, Component.text(RequiredLinkingModule.NOT_READY_MESSAGE));
            return;
        }

        if (!config.enabled || module.action() != ServerRequiredLinkingConfig.Action.KICK
                || !eventType.equals(config.kick.event) || !priority.name().equals(config.kick.priority)) {
            return;
        }

        Pair<UUID, String> player = getPlayer(event);
        UUID playerUUID = player.getLeft();
        String playerName = player.getRight();

        String blockType = getAlreadyBlockedReason(event);
        if (blockType != null) {
            discordSRV().logger().debug(playerName + " is already blocked for " + eventType + "/" + priority + " (" + blockType + ")");
            return;
        }

        Component kickReason = module.getBlockReason(playerUUID, playerName, true).join();
        if (kickReason != null) {
            blockLogin(event, kickReason);
        }
    }
}
