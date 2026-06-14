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

package com.discordsrv.common.feature.ignore;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.PlayerEvent;
import com.discordsrv.api.events.message.preprocess.discord.DiscordChatMessagePreProcessEvent;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.core.debug.DebugGenerateEvent;
import com.discordsrv.common.core.debug.file.TextDebugFile;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IgnoreModule extends AbstractModule<DiscordSRV> {

    private final Set<UUID> ignoredPlayerUUIDs = ConcurrentHashMap.newKeySet();
    private final Set<Long> ignoredDiscordUserIds = ConcurrentHashMap.newKeySet();

    public IgnoreModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "IGNORE_MODULE"));
    }

    public Set<UUID> getIgnoredPlayers() {
        return ignoredPlayerUUIDs;
    }

    public Set<Long> getIgnoredDiscordUsers() {
        return ignoredDiscordUserIds;
    }

    @Subscribe
    public void onDebugGenerate(DebugGenerateEvent event) {
        StringBuilder builder = new StringBuilder();

        if (!ignoredPlayerUUIDs.isEmpty()) {
            builder.append("Ignored players:");
            Task.allOf(ignoredPlayerUUIDs.stream().map(uuid -> discordSRV.playerProvider().lookupOfflinePlayer(uuid)).collect(Collectors.toList())).whenComplete((offlinePlayers,  throwable) -> {
                for (IOfflinePlayer player : offlinePlayers) {
                    builder.append("\n\t").append(player.username()).append(" - ").append(player.uniqueId());
                }
                builder.append("\n");
            }).join();
        }

        if (!ignoredDiscordUserIds.isEmpty()) {
            builder.append("Ignored Discord users:");
            for (long discordUserId : ignoredDiscordUserIds) {
                DiscordUser user = discordSRV.discordAPI().getUserById(discordUserId);
                builder.append("\n\t").append(user != null ? user.getAsTag() + " - " + user.getId() : discordUserId);
            }
            builder.append("\n");
        }

        event.addFile("ignored.txt", new TextDebugFile(builder));
    }

    @Subscribe
    public void onCancellableEvent(Cancellable cancellableEvent) {
        if (cancellableEvent instanceof PlayerEvent) {
            PlayerEvent event = (PlayerEvent) cancellableEvent;
            if (ignoredPlayerUUIDs.contains(event.getPlayer().uniqueId())) {
                logger().debug("Ignoring event \"" + cancellableEvent.getClass().getSimpleName() + "\" from ignored Minecraft player " + event.getPlayer().username() + " - " + event.getPlayer().uniqueId());
                cancellableEvent.setCancelled(true);
            }
        } else if (cancellableEvent instanceof DiscordChatMessagePreProcessEvent) {
            DiscordChatMessagePreProcessEvent event = (DiscordChatMessagePreProcessEvent) cancellableEvent;
            if (ignoredDiscordUserIds.stream().map(x-> (long) x).collect(Collectors.toList()).contains(event.getMessage().getAuthor().getId())) {
                logger().trace("Ignoring Discord message from ignored Discord user " + event.getMessage().getAuthor().getId() + ": " + event.getMessage().getContent());
                event.setCancelled(true);
            }
        }
    }
}
