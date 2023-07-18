package com.discordsrv.api.event.events.message.receive.discord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.event.events.Cancellable;
import com.discordsrv.api.event.events.Processable;

/**
 * Indicates that a Discord message is about to be processed, this will run once per {@link GameChannel} destination,
 * meaning it could run multiple times for a single Discord message. This runs after {@link DiscordChatMessageReceiveEvent}.
 */
public class DiscordChatMessageProcessEvent implements Cancellable, Processable {

    private final DiscordMessageChannel discordChannel;
    private final ReceivedDiscordMessage message;
    private String content;
    private final GameChannel destinationChannel;
    private boolean cancelled;
    private boolean processed;

    public DiscordChatMessageProcessEvent(
            DiscordMessageChannel discordChannel,
            ReceivedDiscordMessage message,
            GameChannel destinationChannel
    ) {
        this.discordChannel = discordChannel;
        this.message = message;
        this.content = message.getContent();
        this.destinationChannel = destinationChannel;
    }

    public DiscordMessageChannel getDiscordChannel() {
        return discordChannel;
    }

    public ReceivedDiscordMessage getMessage() {
        return message;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public GameChannel getDestinationChannel() {
        return destinationChannel;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isProcessed() {
        return processed;
    }

    @Override
    public void markAsProcessed() {
        if (cancelled) {
            throw new IllegalStateException("Cannot process cancelled event");
        }
        if (processed) {
            throw new IllegalStateException("Cannot process already processed event");
        }
        this.processed = true;
    }
}
