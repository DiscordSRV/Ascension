package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import net.dv8tion.jda.api.entities.channel.concrete.MediaChannel;

import java.util.concurrent.CompletableFuture;

public interface DiscordMediaChannel extends DiscordChannel, DiscordThreadContainer, JDAEntity<MediaChannel> {
    CompletableFuture<DiscordThreadChannel> createPost(String name, SendableDiscordMessage message);
}
