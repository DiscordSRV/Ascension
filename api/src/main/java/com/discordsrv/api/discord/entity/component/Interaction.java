package com.discordsrv.api.discord.entity.component;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.concurrent.CompletableFuture;

public interface Interaction extends JDAEntity<InteractionHook> {

    long getExpiryTime();
    boolean isExpired();

    DiscordUser getUser();

    CompletableFuture<Interaction> replyLater(boolean ephemeral);

    CompletableFuture<ReceivedDiscordMessage> editOriginal(SendableDiscordMessage message);
    CompletableFuture<Interaction> reply(SendableDiscordMessage message);
    CompletableFuture<Interaction> replyEphemeral(SendableDiscordMessage message);
    CompletableFuture<Void> replyModal(Modal modal);

}
