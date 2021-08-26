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
