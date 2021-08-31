/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.api.message;

import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReceivedDiscordMessageClusterImpl implements ReceivedDiscordMessageCluster {

    private final List<ReceivedDiscordMessage> messages;

    public ReceivedDiscordMessageClusterImpl(List<ReceivedDiscordMessage> messages) {
        this.messages = messages;
    }

    @Override
    public List<ReceivedDiscordMessage> getMessages() {
        return messages;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Void> deleteAll() {
        CompletableFuture<Void>[] futures = new CompletableFuture[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            futures[i] = messages.get(i).delete();
        }

        return CompletableFuture.allOf(futures);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<ReceivedDiscordMessageCluster> editAll(SendableDiscordMessage newMessage) {
        CompletableFuture<ReceivedDiscordMessage>[] futures = new CompletableFuture[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            futures[i] = messages.get(i).edit(newMessage);
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    List<ReceivedDiscordMessage> messages = new ArrayList<>();
                    for (CompletableFuture<ReceivedDiscordMessage> future : futures) {
                        // All the futures are done, so we're just going to get the results from all of them
                        messages.add(
                                future.join());
                    }

                    return new ReceivedDiscordMessageClusterImpl(messages);
                });
    }
}
