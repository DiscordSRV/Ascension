package com.discordsrv.api.discord.api.entity.channel;

import com.discordsrv.api.discord.api.entity.Snowflake;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A Discord channel that can send/receive messages.
 */
public interface DiscordMessageChannel extends Snowflake {

    /**
     * Sends the provided message to the channel.
     * @param message the channel to send to the channel
     * @return a future returning the message after being sent
     * @throws com.discordsrv.api.discord.api.exception.NotReadyException if DiscordSRV is not ready, {@link com.discordsrv.api.DiscordSRVApi#isReady()}
     */
    @NotNull
    CompletableFuture<ReceivedDiscordMessage> sendMessage(SendableDiscordMessage message);

    /**
     * Edits the message identified by the id.
     * @param id the id of the message to edit
     * @param message the new message content
     * @return a future returning the message after being edited
     * @throws com.discordsrv.api.discord.api.exception.NotReadyException if DiscordSRV is not ready, {@link com.discordsrv.api.DiscordSRVApi#isReady()}
     */
    @NotNull
    CompletableFuture<ReceivedDiscordMessage> editMessageById(String id, SendableDiscordMessage message);

}
