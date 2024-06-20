package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;

import java.util.concurrent.CompletableFuture;

public interface DiscordForumChannel extends DiscordChannel, DiscordThreadContainer, JDAEntity<ForumChannel> {

    CompletableFuture<DiscordThreadChannel> createPost(String name, SendableDiscordMessage message);
}
