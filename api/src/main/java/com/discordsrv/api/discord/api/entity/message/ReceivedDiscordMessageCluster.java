package com.discordsrv.api.discord.api.entity.message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A cluster of Discord messages, or just a single message.
 */
public interface ReceivedDiscordMessageCluster {

    /**
     * Gets the messages in this cluster.
     * @return the messages in this cluster
     */
    List<? extends ReceivedDiscordMessage> getMessages();

    /**
     * Deletes all the messages from this cluster, one request per message.
     * @return a future that fails if any of the requests fail.
     */
    CompletableFuture<Void> deleteAll();

    /**
     * Edits all the messages in this cluster, one request per edit.
     * @param newMessage the new content of the messages
     * @return a future that fails if any of the requests fail.
     */
    CompletableFuture<ReceivedDiscordMessageCluster> editAll(SendableDiscordMessage newMessage);

}
