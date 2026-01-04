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

package com.discordsrv.common.discord.api.entity.message;

import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.task.Task;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ReceivedDiscordMessageClusterImpl implements ReceivedDiscordMessageCluster {

    private final Set<ReceivedDiscordMessage> messages;

    public ReceivedDiscordMessageClusterImpl(Collection<ReceivedDiscordMessage> messages) {
        this.messages = new HashSet<>(messages);
    }

    @Override
    public @NotNull Set<ReceivedDiscordMessage> getMessages() {
        return messages;
    }

    @Override
    public @NotNull Task<Void> deleteAll() {
        List<Task<Void>> futures = new ArrayList<>(messages.size());
        for (ReceivedDiscordMessage message : messages) {
            futures.add(message.delete());
        }

        return Task.allOf(futures).thenApply(v -> null);
    }

    @Override
    public @NotNull Task<ReceivedDiscordMessageCluster> editAll(SendableDiscordMessage newMessage) {
        List<Task<ReceivedDiscordMessage>> futures = new ArrayList<>(messages.size());
        for (ReceivedDiscordMessage message : messages) {
            futures.add(message.edit(newMessage));
        }

        return Task.allOf(futures).thenApply(ReceivedDiscordMessageClusterImpl::new);
    }
}
